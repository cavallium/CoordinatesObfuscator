package org.warp.coordinatesobfuscator;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Folia implements Bukkit {
	@Override
	public void run(Plugin plugin, Player player, Runnable runnable, Runnable fallback) {
		player.getScheduler().run(plugin, st -> runnable.run(), fallback);
	}

	@Override
	public void runLater(Plugin plugin, Runnable runnable, long delay) {
		org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
	}
}
