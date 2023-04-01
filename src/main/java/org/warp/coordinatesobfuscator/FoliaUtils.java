package org.warp.coordinatesobfuscator;

public class FoliaUtils {

	private static final boolean IS_FOLIA;

	static {
		boolean isFolia;
		try {
			Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
			isFolia = true;
		} catch (Throwable ignored) {
			isFolia = false;
		}
		IS_FOLIA = isFolia;
	}
	
	public static boolean isFolia() {
		return IS_FOLIA;
	}
}
