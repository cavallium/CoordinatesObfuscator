try {
	{
		var respawnFakePacket = new PacketContainer(Server.RESPAWN);
		respawnFakePacket.getIntegers().write(0,Environment.NORMAL.getId());
		respawnFakePacket.getLongs().write(0,839834L);
		respawnFakePacket.getGameModes().write(0,NativeGameMode.fromBukkit(player.getGameMode()));
		respawnFakePacket.getWorldTypeModifier().write(0,player.getWorld().getWorldType());
		ProtocolLibrary.getProtocolManager().sendServerPacket(player,respawnFakePacket);
	}

	{
		var respawnFakePacket = new PacketContainer(Server.RESPAWN);
		respawnFakePacket.getIntegers().write(0,player.getWorld().getEnvironment().getId());
		respawnFakePacket.getLongs().write(0,player.getWorld().getSeed());
		respawnFakePacket.getGameModes().write(0,NativeGameMode.fromBukkit(player.getGameMode()));
		respawnFakePacket.getWorldTypeModifier().write(0,player.getWorld().getWorldType());
		ProtocolLibrary.getProtocolManager().sendServerPacket(player,respawnFakePacket);
	}
} catch (InvocationTargetException e) {
	e.printStackTrace();
}