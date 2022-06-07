package org.warp.coordinatesobfuscator;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.server.TemporaryPlayer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.logging.Logger;

public class TranslatorServerbound {

	private static final String USE_ITEM = "BLOCK_PLACE";
	private static final String BLOCK_PLACE = "USE_ITEM";

	public static void incoming(Logger logger, final PacketContainer packet, final Player player) {
		if (player == null || player instanceof TemporaryPlayer) {
			return;
		}
		CoordinateOffset offset = PlayerManager.getOffsetOrJoinPlayer(player, player.getWorld());
		Objects.requireNonNull(offset);
		if (offset.isZero()) {
			return;
		}
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
			case USE_ITEM:
				break;
			case BLOCK_PLACE:
				recvMovingPosition(logger, packet, offset);
				break;
			case "USE_ENTITY":
				break;
			default:
				break;
		}
	}

	private static void recvDouble(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getDoubles().size() > 2) {
			packet.getDoubles().modify(0, x -> x == null ? null : x + offset.getX());
			packet.getDoubles().modify(2, z -> z == null ? null : z + offset.getZ());
		} else {
			int size = packet.getDoubles().size();
			logger.severe("Packet size error: " + size);
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
		var mopb = packet.getMovingBlockPositions().read(0);
		if (mopb == null) {
			return;
		}
		mopb.setBlockPosition(mopb.getBlockPosition().add(new BlockPosition(offset.getXInt(), 0, offset.getZInt())));
		mopb.setPosVector(mopb.getPosVector().add(new Vector(offset.getX(), 0, offset.getZ())));
		packet.getMovingBlockPositions().write(0, mopb);
	}
}
