package org.warp.coordinatesobfuscator;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.nbt.NbtBase;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import java.lang.invoke.MethodType;

import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public class CoordinatesObfuscator extends JavaPlugin implements Listener {

	public static final boolean DEBUG_ENABLED = "debug".equals(System.getProperty("coordinates_obfuscator.env"));
	public static final boolean DISALLOW_REMOVING_NONEXISTENT_COORDINATES = false;

	private static final MethodHandle CREATE_WORLD_BORDER_METHOD;

	static {
		var lookup = MethodHandles.publicLookup();
		MethodHandle createWorldBorderMethod = null;
		try {
			Class<?> worldBorderClass = lookup.findClass("org.bukkit.WorldBorder");
			createWorldBorderMethod = lookup.findStatic(Bukkit.class, "createWorldBorder", MethodType.methodType(worldBorderClass));
		} catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
			// ignore
		}
		CREATE_WORLD_BORDER_METHOD = createWorldBorderMethod;
	}
	private Logger logger;

	@Override
	public void onDisable() {
		PlayerManager.unload();
	}


	@SuppressWarnings("CommentedOutCode")
	@Override
	public void onEnable() {
		this.logger = getLogger();
		PlayerManager.load(logger);

		Bukkit.getPluginManager().registerEvents(this, this);

		final ProtocolManager pm = ProtocolLibrary.getProtocolManager();

		final HashSet<PacketType> packets = new HashSet<>();

		// /Server side packets
		{
			final PacketAdapter.AdapterParameteters paramsServer = PacketAdapter.params();
			paramsServer.plugin(this);
			paramsServer.connectionSide(ConnectionSide.SERVER_SIDE);
			paramsServer.listenerPriority(ListenerPriority.HIGHEST);
			paramsServer.gamePhase(GamePhase.PLAYING);

			packets.add(PacketType.Play.Server.BUNDLE);
			packets.add(PacketType.Play.Server.BLOCK_ACTION);
			packets.add(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
			packets.add(PacketType.Play.Server.BLOCK_CHANGE);
			packets.add(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
			packets.add(PacketType.Play.Server.MAP_CHUNK);
			packets.add(Server.UNLOAD_CHUNK);
			packets.add(PacketType.Play.Server.LIGHT_UPDATE);
			packets.add(PacketType.Play.Server.EXPLOSION);
			packets.add(PacketType.Play.Server.SPAWN_POSITION);

			packets.add(PacketType.Play.Server.RESPAWN);
			packets.add(PacketType.Play.Server.POSITION);

			packets.add(PacketType.Play.Server.WORLD_PARTICLES);
			packets.add(PacketType.Play.Server.WORLD_EVENT);

			packets.add(PacketType.Play.Server.NAMED_SOUND_EFFECT);

			packets.add(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
			packets.add(PacketType.Play.Server.SPAWN_ENTITY);
			packets.add(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB);
			packets.add(PacketType.Play.Server.ENTITY_TELEPORT);

			packets.add(PacketType.Play.Server.OPEN_SIGN_EDITOR);

			packets.add(PacketType.Play.Server.ENTITY_METADATA);
			packets.add(PacketType.Play.Server.VIEW_CENTRE);
			packets.add(PacketType.Play.Server.WINDOW_ITEMS);
			packets.add(PacketType.Play.Server.WINDOW_DATA);
			packets.add(PacketType.Play.Server.SET_SLOT);

			packets.add(Server.TILE_ENTITY_DATA);

			//todo: these packets shouldn't have position. Check if some of them must be translated
			// packets.add(Server.ENTITY_DESTROY);
			// packets.add(Server.ENTITY_EQUIPMENT);
			// packets.add(Server.ENTITY_LOOK);
			// packets.add(Server.ENTITY_EFFECT);
			// packets.add(Server.ENTITY_HEAD_ROTATION);
			// packets.add(Server.ENTITY_SOUND);
			// packets.add(Server.ENTITY_STATUS);
			// packets.add(Server.ENTITY_VELOCITY);
			// packets.add(Server.REL_ENTITY_MOVE);
			// packets.add(Server.REL_ENTITY_MOVE_LOOK);
			// packets.add(Server.ATTACH_ENTITY);

			paramsServer.types(packets);

			pm.addPacketListener(new PacketAdapter(paramsServer) {
				@SuppressWarnings("DuplicateBranchesInSwitch")
				@Override
				public void onPacketSending(final PacketEvent event) {

					PacketContainer packet;

					switch (event.getPacket().getType().name()) {
						case "LIGHT_UPDATE" -> packet = event.getPacket().shallowClone();
						case "TILE_ENTITY_DATA" -> packet = cloneTileEntityData(event.getPacket());
						case "MAP_CHUNK" -> packet = cloneMapChunkEntitiesData(event.getPacket());
						case "WORLD_PARTICLES" -> packet = event.getPacket().deepClone();
						default -> packet = event.getPacket().shallowClone();
					}

					Player player = event.getPlayer();

					boolean cancel = TranslatorClientbound.outgoing(logger, packet, player);

					event.setPacket(packet);

					if (cancel) {
						event.setCancelled(true);
					}
				}

			});

		}
		// End Server Packets

		// /Client side Packets
		{
			final PacketAdapter.AdapterParameteters paramsClient = PacketAdapter.params();
			paramsClient.plugin(this);
			paramsClient.connectionSide(ConnectionSide.CLIENT_SIDE);
			paramsClient.listenerPriority(ListenerPriority.LOWEST);
			paramsClient.gamePhase(GamePhase.PLAYING);

			packets.clear();

			packets.add(PacketType.Play.Client.POSITION);
			packets.add(PacketType.Play.Client.POSITION_LOOK);
			packets.add(PacketType.Play.Client.BLOCK_DIG);
			packets.add(PacketType.Play.Client.BLOCK_PLACE);
			packets.add(PacketType.Play.Client.USE_ITEM);
			packets.add(PacketType.Play.Client.USE_ENTITY);
			packets.add(PacketType.Play.Client.VEHICLE_MOVE);
			packets.add(PacketType.Play.Client.SET_COMMAND_BLOCK);
			packets.add(PacketType.Play.Client.SET_JIGSAW);
			packets.add(PacketType.Play.Client.STRUCT);
			packets.add(PacketType.Play.Client.UPDATE_SIGN);

			paramsClient.types(packets);

			pm.addPacketListener(new PacketAdapter(paramsClient) {


				@Override
				public void onPacketReceiving(final PacketEvent event) {
					try {
						TranslatorServerbound.incoming(logger, event.getPacket(), event.getPlayer());
					} catch (final UnsupportedOperationException e) {
						event.setCancelled(true);
						e.printStackTrace();
						if (event.getPlayer() != null) {
							Bukkit.getServer().broadcastMessage("Failed: " + event.getPacket().getType().name());
						}
					}
				}
			});
		}
		// End client packets
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerQuit(final PlayerQuitEvent event) {
		PlayerManager.exitPlayer(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		// This part of code has been commented because this event is fired too late.
		//PlayerManager.joinPlayer(event.getPlayer());

		setMaxWorldBorder(event.getPlayer()); // StarLightMinecraft
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerTeleport(final PlayerRespawnEvent event){
		setMaxWorldBorder(event.getPlayer());
	}

	private void setMaxWorldBorder(final Player player) {
		if (BukkitUtils.isFolia()) {
			BukkitUtils.getBukkit().run(this, player, () -> setMaxWorldBorderSync(player), null);
		} else {
			BukkitUtils.getBukkit().runLater(this, () -> setMaxWorldBorderSync(player), 5);
		}
	}

	private void setMaxWorldBorderSync(Player player) {
		if (!player.isOnline()) return;
		// Some bukkit versions don't have this method
		if (CREATE_WORLD_BORDER_METHOD == null) return;
		WorldBorder border;
		try {
			border = (WorldBorder) CREATE_WORLD_BORDER_METHOD.invokeExact();
		} catch (Throwable e) {
			// This shouldn't happen
			throw new RuntimeException(e);
		}
		border.setCenter(player.getWorld().getWorldBorder().getCenter());
		border.setSize(59999968E7);
		player.setWorldBorder(border);
	}

	private PacketContainer cloneTileEntityData(PacketContainer packet) {
		packet = packet.shallowClone();
		int i = 0;
		for (final NbtBase<?> obj : packet.getNbtModifier().getValues()) {
			if (obj == null) continue;
			packet.getNbtModifier().write(i, obj.deepClone());
			i++;
		}

		return packet;
	}

	private PacketContainer cloneMapChunkEntitiesData(PacketContainer packet) {
		packet = packet.shallowClone();
		int i = 0;
		for (final List<NbtBase<?>> obj : packet.getListNbtModifier().getValues()) {
			ArrayList<NbtBase<?>> newList = new ArrayList<NbtBase<?>>(obj.size());
			for (NbtBase<?> nbtBase : obj) {
				newList.add(nbtBase.deepClone());
			}
			packet.getListNbtModifier().write(i, newList);
			i++;
		}

		return packet;
	}
}
