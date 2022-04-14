package org.warp.coordinatesobfuscator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CoordsOffsetsManager {
	private final Map<UUID, HashMap<UUID, CoordinateOffset>> playerCoordsPerWorld = new HashMap<>();
	private final Logger pluginLogger;

	public CoordsOffsetsManager(Logger pluginLogger) {
		this.pluginLogger = pluginLogger;
	}

	public synchronized CoordinateOffset get(Player player, World world) {
		return get(player.getUniqueId(), world.getUID());
	}

	public synchronized CoordinateOffset get(UUID player, UUID world) {
		return getValue(player, world);
	}

	public synchronized CoordinateOffset getOrNull(Player player, World world) {
		return getOrNull(player.getUniqueId(), world.getUID());
	}

	public synchronized CoordinateOffset getOrNull(UUID player, UUID world) {
		return getValueOrNull(player, world);
	}

	public synchronized void put(Player player, World world, CoordinateOffset offset) {
		put(player.getUniqueId(), world.getUID(), offset);
	}

	public synchronized void put(UUID player, UUID world, CoordinateOffset offset) {
		setValue(player, world, offset);
	}

	public synchronized void replace(Player player, World world, CoordinateOffset offset) {
		replace(player.getUniqueId(), world.getUID(), offset);
	}

	public synchronized void replace(UUID player, UUID world, CoordinateOffset offset) {
		replaceValue(player, world, offset);
	}

	public synchronized void remove(Player player, World world) {
		removeValue(player.getUniqueId(), world.getUID());
	}

	public synchronized void remove(UUID player, UUID world) {
		removeValue(player, world);
	}

	public synchronized void remove(Player player) {
		removeValue(player.getUniqueId());
	}

	public synchronized void remove(UUID player) {
		removeValue(player);
	}

	public synchronized CoordinateOffset getOrPut(Player player, World world, Supplier<CoordinateOffset> putSupplier) {
		CoordinateOffset currentValue = getOrNull(player, world);
		if (currentValue != null) {
			return currentValue;
		} else {
			CoordinateOffset value = putSupplier.get();
			put(player, world, value);
			return value;
		}
	}

	private void removeValue(UUID playerUUID, UUID worldUUID) {
		pluginLogger.info("Deleting player coordinates offset");
		HashMap<UUID, CoordinateOffset> worldsMap = playerCoordsPerWorld.get(playerUUID);
		if (worldsMap != null) {
			CoordinateOffset playerPosition = worldsMap.remove(worldUUID);
			if (playerPosition == null) {
				if (CoordinatesObfuscator.DISALLOW_REMOVING_NONEXISTENT_COORDINATES) {
					throw new UnsupportedOperationException("Trying to remove nonexistent coordinate offset");
				}
			}
		} else {
			if (CoordinatesObfuscator.DISALLOW_REMOVING_NONEXISTENT_COORDINATES) {
				throw new UnsupportedOperationException("Trying to remove nonexistent coordinate offset");
			}
		}
	}

	private void removeValue(UUID playerUUID) {
		pluginLogger.info("Deleting player coordinates offset (globally)");
		HashMap<UUID, CoordinateOffset> worldsMap = playerCoordsPerWorld.remove(playerUUID);
		if (worldsMap == null) {
			if (CoordinatesObfuscator.DISALLOW_REMOVING_NONEXISTENT_COORDINATES) {
				throw new UnsupportedOperationException("Trying to remove nonexistent coordinate offset");
			}
		}
	}

	private void setValue(UUID playerUUID, UUID worldUUID, CoordinateOffset value) {
		pluginLogger.info("Setting player coordinates offset");
		value.validate();
		HashMap<UUID, CoordinateOffset> worldsMap = playerCoordsPerWorld.computeIfAbsent(playerUUID, (playerUid) -> new HashMap<>());
		if (worldsMap.containsKey(worldUUID)) {
			throw new UnsupportedOperationException("Trying to overwrite coordinate offset");
		} else {
			worldsMap.put(worldUUID, value);
		}
	}

	private void replaceValue(UUID playerUUID, UUID worldUUID, CoordinateOffset value) {
		pluginLogger.info("Replacing player coordinates offset");
		value.validate();
		HashMap<UUID, CoordinateOffset> worldsMap = playerCoordsPerWorld.computeIfAbsent(playerUUID, (playerUid) -> new HashMap<>());
		if (!worldsMap.containsKey(worldUUID)) {
			throw new UnsupportedOperationException("Trying to replace nonexistent coordinate offset");
		} else {
			if (!worldsMap.containsKey(worldUUID)) {
				throw new UnsupportedOperationException("Trying to replace nonexistent coordinate offset");
			}
			worldsMap.put(worldUUID, value);
		}
	}

	private CoordinateOffset getValue(UUID playerUUID, UUID worldUUID) {
		HashMap<UUID, CoordinateOffset> worldsMap = playerCoordsPerWorld.get(playerUUID);
		if (worldsMap == null) {
			throw new UnsupportedOperationException("Trying to get nonexistent coordinate offset");
		}
		CoordinateOffset offset = worldsMap.get(worldUUID);
		if (offset == null) {
			throw new UnsupportedOperationException("Trying to get nonexistent coordinate offset");
		}
		return offset;
	}

	private CoordinateOffset getValueOrNull(UUID playerUUID, UUID worldUUID) {
		HashMap<UUID, CoordinateOffset> worldsMap = playerCoordsPerWorld.get(playerUUID);
		if (worldsMap == null) {
			return null;
		}
		CoordinateOffset offset = worldsMap.get(worldUUID);
		return offset;
	}
}
