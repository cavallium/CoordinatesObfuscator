package org.warp.coordinatesobfuscator;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;

public class LastPlayerCoordinateManager {
	Map<UUID, Location> locations = new HashMap<>();

	public synchronized Optional<Location> getLastPlayerLocation(UUID player) {
		return Optional.ofNullable(locations.getOrDefault(player, null));
	}

	public synchronized void setLastPlayerLocation(UUID player, Location location) {
		locations.put(player, location);
	}

	public synchronized void resetLastPlayerLocation(UUID player) {
		locations.remove(player);
	}
}
