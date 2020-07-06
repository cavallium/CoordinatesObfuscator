package org.warp.coordinatesobfuscator;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Client;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.server.TemporaryPlayer;
import com.comphenix.protocol.wrappers.BlockPosition;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TranslatorServerbound {

	private static final String SERVER_VERSION;

	public static final Class<?> VEC3DCLASS;
	public static final Class<?> ENUMDIRECTIONCLASS;
	public static final Class<?> BLOCKPOSITIONCLASS;
	public static final Class<?> MOVINGOBJECTPOSITIONBLOCKCLASS;
	public static final Method MovingObjectPositionBlockGetBlockPositionMethod;
	public static final Method MovingObjectPositionBlockGetDirectionMethod;
	public static final Method MovingObjectPositionBlockGetPosMethod;
	public static final Method MovingObjectPositionBlockStaticConstructor;
	public static final Method Vec3DaddMethod;
	public static final Method BlockPositionAddMethod;

	static {
		// This gets the server version.
		String name = Bukkit.getServer().getClass().getName();
		name = name.substring(name.indexOf("craftbukkit.") + "craftbukkit.".length());
		name = name.substring(0, name.indexOf("."));
		SERVER_VERSION = name;
		try {
			VEC3DCLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".Vec3D");
			ENUMDIRECTIONCLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".EnumDirection");
			BLOCKPOSITIONCLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".BlockPosition");
			MOVINGOBJECTPOSITIONBLOCKCLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".MovingObjectPositionBlock");
			MovingObjectPositionBlockGetBlockPositionMethod = MOVINGOBJECTPOSITIONBLOCKCLASS.getDeclaredMethod("getBlockPosition");
			MovingObjectPositionBlockGetDirectionMethod = MOVINGOBJECTPOSITIONBLOCKCLASS.getMethod("getDirection");
			MovingObjectPositionBlockGetPosMethod = MOVINGOBJECTPOSITIONBLOCKCLASS.getMethod("getPos");
			MovingObjectPositionBlockStaticConstructor = MOVINGOBJECTPOSITIONBLOCKCLASS.getMethod("a", VEC3DCLASS, ENUMDIRECTIONCLASS, BLOCKPOSITIONCLASS);
			Vec3DaddMethod = VEC3DCLASS.getMethod("add", double.class, double.class, double.class);
			BlockPositionAddMethod = BLOCKPOSITIONCLASS.getMethod("a", double.class, double.class, double.class);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void incoming(Logger logger, final PacketContainer packet, final Player player) {
		if (player instanceof TemporaryPlayer) {
			return;
		}
		CoordinateOffset offset = PlayerManager.getOffsetOrJoinPlayer(player, player.getWorld());
		Objects.requireNonNull(offset);
		switch (packet.getType().name()) {
			case "POSITION":
			case "VEHICLE_MOVE":
			case "POSITION_LOOK":
				PlayerManager.setLastPlayerLocation(player, player.getLocation());
				recvDouble(logger, packet, offset);
				break;
			case "STRUCT":
			case "SET_JIGSAW":
			case "SET_COMMAND_BLOCK":
			case "UPDATE_SIGN":
			case "BLOCK_DIG":
				recvPosition(logger, packet, offset);
				break;
			case "BLOCK_PLACE":
				if (Client.BLOCK_PLACE.getCurrentId() == 44) {
					recvMovingPosition(logger, packet, offset);
				}
				break;
			case "USE_ITEM": // This is Block Place, not Use_Item. It's a naming bug
				if (Client.USE_ITEM.getCurrentId() == 44) {
					recvMovingPosition(logger, packet, offset);
				}
				break;
			case "USE_ENTITY":
				break;
		}
	}

	private static void recvDouble(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getDoubles().size() > 2) {
			packet.getDoubles().modify(0, x -> x == null ? null : x + offset.getX());
			packet.getDoubles().modify(2, z -> z == null ? null : z + offset.getZ());
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void recvInt(Logger logger, final PacketContainer packet, final CoordinateOffset offset, final int index) {
		if (packet.getIntegers().size() > 2) {
			packet.getIntegers().modify(index, curr_x -> curr_x == null ? null : curr_x + offset.getXInt());
			packet.getIntegers().modify(index + 2, curr_z -> curr_z == null ? null : curr_z + offset.getZInt());
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void recvPosition(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getBlockPositionModifier().size() > 0) {
			packet.getBlockPositionModifier().modify(0, pos -> offsetPosition(logger, offset, pos));
		} else {
			logger.severe("Packet size error");
		}
	}

	private static BlockPosition offsetPosition(Logger logger, CoordinateOffset offset, BlockPosition pos) {
		if (pos == null) return null;
		return pos.add(new BlockPosition(offset.getXInt(), 0, offset.getZInt()));
	}

	private static void recvMovingPosition(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		var mopb = packet.getModifier().read(0);
		if (mopb == null) {
			return;
		}
		if (!MOVINGOBJECTPOSITIONBLOCKCLASS.isInstance(mopb)) {
			throw new RuntimeException("Wrong type");
		}
		try {
			var blockPosition = MovingObjectPositionBlockGetBlockPositionMethod.invoke(mopb);
			var direction = MovingObjectPositionBlockGetDirectionMethod.invoke(mopb);
			var vec3d = MovingObjectPositionBlockGetPosMethod.invoke(mopb);

			var newVec3d = Vec3DaddMethod.invoke(vec3d, offset.getX(), 0, offset.getZ());

			var newBlockPosition = BlockPositionAddMethod.invoke(blockPosition, offset.getX(), 0, offset.getZ());

			var result = MovingObjectPositionBlockStaticConstructor.invoke(null, newVec3d, direction, newBlockPosition);
			packet.getModifier().write(0, result);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
