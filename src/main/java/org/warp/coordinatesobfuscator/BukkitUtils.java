package org.warp.coordinatesobfuscator;

public class BukkitUtils {

	private static final boolean IS_FOLIA;
	private static final Bukkit BUKKIT;

	static {
		boolean isFolia;
		try {
			Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
			isFolia = true;
		} catch (Throwable ignored) {
			isFolia = false;
		}
		IS_FOLIA = isFolia;

		BUKKIT = isFolia ? new Folia() : new Craftbukkit();
	}

	public static boolean isFolia() {
		return IS_FOLIA;
	}

	public static Bukkit getBukkit() {
		return BUKKIT;
	}
}
