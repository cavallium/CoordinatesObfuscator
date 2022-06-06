package org.warp.coordinatesobfuscator;

import com.comphenix.protocol.injector.server.TemporaryPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class PlayerManager {

	private static final Random random = new Random();
	private static final int staticXOffset = getRandomChunkOffsetBase();
	private static final int staticZOffset = getRandomChunkOffsetBase();
	private static Logger logger;
	private static CoordsOffsetsManager coordsOffsetsManager;
	private static LastPlayerCoordinateManager lastPlayerCoordinateManager;

	public static void spawnPlayer(final Player player, final World world) {
		coordsOffsetsManager.put(player, world, generateOffset(world)); // StarLightMinecraft - make center and size of the world border into the offset to make sure it's always generate in the world border
		lastPlayerCoordinateManager.setLastPlayerLocation(player.getUniqueId(), player.getLocation());
	}

	public static CoordinateOffset respawnPlayer(final Player player, final World world) {
		CoordinateOffset offset = generateOffset(world); // StarLightMinecraft - make center and size of the world border into the offset to make sure it's always generate in the world border
		coordsOffsetsManager.replace(player, world, offset);
		lastPlayerCoordinateManager.setLastPlayerLocation(player.getUniqueId(), player.getLocation());
		return offset;
	}

	public static CoordinateOffset teleportPlayer(final Player player, final World world, boolean overrideLastLocation) {
		CoordinateOffset offset = generateOffset(world); // StarLightMinecraft - make center and size of the world border into the offset to make sure it's always generate in the world border
		coordsOffsetsManager.replace(player, world, offset);
		if (overrideLastLocation) {
			lastPlayerCoordinateManager.setLastPlayerLocation(player.getUniqueId(), player.getLocation());
		}

		return offset;
	}

	public static void joinPlayer(final Player player) {
		if (!(player instanceof TemporaryPlayer)) {
			coordsOffsetsManager.getOrPut(player, player.getWorld(), () -> generateOffset(player.getWorld())); // StarLightMinecraft - make center and size of the world border into the offset to make sure it's always generate in the world border
			lastPlayerCoordinateManager.setLastPlayerLocation(player.getUniqueId(), player.getLocation());
		} else {
			lastPlayerCoordinateManager.resetLastPlayerLocation(player.getUniqueId());
		}
	}

	private static CoordinateOffset generateOffset(World world) {
		//return CoordinateOffset.of(64 * 16, 64 * 16);
		// StarLightMinecraft Start - make center and size of the world border into the offset to make sure it's always generate in the world border
		int x = world.getWorldBorder().getCenter().getBlockX();
		int z = world.getWorldBorder().getCenter().getBlockZ();
		double size = world.getWorldBorder().getSize();
		// StarLightMinecraft End
		return CoordinateOffset.of((getRandomChunkOffset(true) * 16) % (size * 2) - x, getRandomChunkOffset(false) * 16 % (size * 2) - z); // StarLightMinecraft - make center and size of the world border into the offset to make sure it's always generate in the world border
	}

	private static int getRandomChunkOffset(boolean x) {
		return getRandomChunkOffsetBase() + (x ? staticXOffset : staticZOffset);
	}

	private static int getRandomChunkOffsetBase() {
		int number = 64 + random.nextInt((int) Math.floor(496000d / 16d) - 64);
		if (random.nextBoolean()) {
			number *= -1;
		}
		return number;
	}

	public static void exitPlayer(final Player player) {
		coordsOffsetsManager.remove(player.getUniqueId());
		lastPlayerCoordinateManager.resetLastPlayerLocation(player.getUniqueId());
	}

	public static void load(Logger pluginLogger) {
		logger = pluginLogger;
		coordsOffsetsManager = new CoordsOffsetsManager(pluginLogger);
		lastPlayerCoordinateManager = new LastPlayerCoordinateManager();
	}

	public static void unload() {
		logger = null;
		coordsOffsetsManager = null;
		lastPlayerCoordinateManager = null;
	}

	public static CoordinateOffset getOffset(Player player) {
		return coordsOffsetsManager.get(player, player.getWorld());
	}

	public static CoordinateOffset getOffset(Player player, World world) {
		return coordsOffsetsManager.get(player, world);
	}

	public static CoordinateOffset getOffsetOrNull(Player player, World world) {
		return coordsOffsetsManager.getOrNull(player, world);
	}

	public static CoordinateOffset getOffsetOrJoinPlayer(Player player, World world) {
		AtomicBoolean generated = new AtomicBoolean(false);
		CoordinateOffset result = coordsOffsetsManager.getOrPut(player, world, () -> {
			generated.set(true);
			return generateOffset(world); // StarLightMinecraft - make center and size of the world border into the offset to make sure it's always generate in the world border
		});
		if (generated.get()) {
			if (!(player instanceof TemporaryPlayer)) {
				coordsOffsetsManager.getOrPut(player, player.getWorld(), () -> generateOffset(world)); // StarLightMinecraft - make center and size of the world border into the offset to make sure it's always generate in the world border
			}
			lastPlayerCoordinateManager.resetLastPlayerLocation(player.getUniqueId());
		}
		return result;
	}

	public static Optional<Location> getLastPlayerLocation(Player player) {
		return lastPlayerCoordinateManager.getLastPlayerLocation(player.getUniqueId());
	}

	public static void setLastPlayerLocation(Player player, Location location) {
		lastPlayerCoordinateManager.setLastPlayerLocation(player.getUniqueId(), location);
	}

	public static void resetLastPlayerLocation(Player player) {
		lastPlayerCoordinateManager.resetLastPlayerLocation(player.getUniqueId());
	}
}
