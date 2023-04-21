package org.warp.coordinatesobfuscator;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface Bukkit {
	void run(Plugin plugin, Player player, Runnable runnable, Runnable fallback);

	void runLater(Plugin plugin, Runnable runnable, long delay);
}
