package org.warp.coordinatesobfuscator;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.server.TemporaryPlayer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TranslatorClientbound {

	private static final String SERVER_VERSION;
	public static final Class<?> COMPASSMETACLASS;
	public static final Class<?> SECTIONPOSITIONCLASS;
	public static final Method SectionPositionCreateMethod;
	public static final Method SectionPositionGetChunkXMethod;
	public static final Method SectionPositionGetChunkYMethod;
	public static final Method SectionPositionGetChunkZMethod;
	private static final boolean USE_1_16_R2;

	static {
		// This gets the server version.
		String name = Bukkit.getServer().getClass().getName();
		name = name.substring(name.indexOf("craftbukkit.") + "craftbukkit.".length());
		name = name.substring(0, name.indexOf("."));
		SERVER_VERSION = name;
		USE_1_16_R2 = SERVER_VERSION.equals("v1_16_R2");
		try {
			SECTIONPOSITIONCLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".SectionPosition");
			SectionPositionCreateMethod = SECTIONPOSITIONCLASS.getDeclaredMethod("a", int.class, int.class, int.class);
			SectionPositionGetChunkXMethod = SECTIONPOSITIONCLASS.getDeclaredMethod("a");
			SectionPositionGetChunkYMethod = SECTIONPOSITIONCLASS.getDeclaredMethod("b");
			SectionPositionGetChunkZMethod = SECTIONPOSITIONCLASS.getDeclaredMethod("c");
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		Class<?> compassMetaClass;
		try {
			compassMetaClass = Class.forName("org.bukkit.inventory.meta.CompassMeta");
		} catch (ClassNotFoundException | NoClassDefFoundError ex) {
			compassMetaClass = null;
		}
		COMPASSMETACLASS = compassMetaClass;

		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);

			Class<?> BLOCKCLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".Block");
			Method BlockNewVoxelShapeMethod = BLOCKCLASS.getDeclaredMethod("a", double.class, double.class, double.class, double.class, double.class, double.class);
			Class<?> BLOCKBAMBOOCLASS = Class.forName("net.minecraft.server." + SERVER_VERSION + ".BlockBamboo");
			Field blockBambooBoundingBoxA = BLOCKBAMBOOCLASS.getDeclaredField("a");
			blockBambooBoundingBoxA.setAccessible(true);
			modifiersField.setInt(blockBambooBoundingBoxA, blockBambooBoundingBoxA.getModifiers() & ~Modifier.FINAL);
			blockBambooBoundingBoxA.set(null, BlockNewVoxelShapeMethod.invoke(null, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
			Field blockBambooBoundingBoxB = BLOCKBAMBOOCLASS.getDeclaredField("b");
			blockBambooBoundingBoxB.setAccessible(true);
			modifiersField.setInt(blockBambooBoundingBoxB, blockBambooBoundingBoxB.getModifiers() & ~Modifier.FINAL);
			blockBambooBoundingBoxB.set(null, BlockNewVoxelShapeMethod.invoke(null, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
			Field blockBambooBoundingBoxC = BLOCKBAMBOOCLASS.getDeclaredField("c");
			blockBambooBoundingBoxC.setAccessible(true);
			modifiersField.setInt(blockBambooBoundingBoxC, blockBambooBoundingBoxC.getModifiers() & ~Modifier.FINAL);
			blockBambooBoundingBoxC.set(null, BlockNewVoxelShapeMethod.invoke(null, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
		} catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException ex) {
			System.err.println("Can't disable bamboo bounding boxes. Please use Java 8. " + ex.getLocalizedMessage());
		}
	}


	/**ENTITY_METADATA
	 *
	 *
	 * @param logger
	 * @param packet
	 * @param player
	 * @return true to cancel
	 */
	public static boolean outgoing(Logger logger, final PacketContainer packet, final Player player) {
		if (player instanceof TemporaryPlayer) {
			return false;
		}
		CoordinateOffset offset = PlayerManager.getOffsetOrJoinPlayer(player, player.getWorld());
		Objects.requireNonNull(offset);
		switch (packet.getType().name()) {
			case "WINDOW_DATA":
				break;
			case "WINDOW_ITEMS":
				fixWindowItems(logger, packet, offset);
				break;
			case "SET_SLOT":
				fixWindowItem(logger, packet, offset);
				break;
			case "SPAWN_POSITION":
			case "WORLD_EVENT":
			case "UPDATE_SIGN":
			case "BLOCK_BREAK":
			case "BLOCK_BREAK_ANIMATION":
			case "BLOCK_ACTION":
			case "BLOCK_CHANGE":
			case "SPAWN_ENTITY_PAINTING":
			case "OPEN_SIGN_EDITOR":
				sendBlockPosition(logger, packet, offset);
				break;
			case "RESPAWN":
				logger.fine("[out]Respawning player");
				respawn(logger, offset, player);
				break;
			case "POSITION":
				boolean isRelativeX = false;
				boolean isRelativeZ = false;
				Set<Enum> items = packet.getSets(Converters.passthrough(Enum.class)).read(0);
				//noinspection rawtypes
				for (Enum item : items) {
					switch (item.name()) {
						case "X":
							isRelativeX = true;
							break;
						case "Z":
							isRelativeZ = true;
							break;
					}
				}
				if (!isRelativeX || !isRelativeZ) {
					logger.fine("[out]Repositioning player");
					CoordinateOffset positionOffset;
					if (!isRelativeX && !isRelativeZ) {
						positionOffset = respawn(logger, offset, player);
					} else {
						positionOffset = offset;
					}
					if (packet.getDoubles().size() > 2) {
						if (!isRelativeX) {
							packet.getDoubles().modify(0, x -> x == null ? null : x - positionOffset.getX());
						}
						if (!isRelativeZ) {
							packet.getDoubles().modify(2, z -> z == null ? null : z - positionOffset.getZ());
						}
					} else {
						logger.severe("Packet size error");
					}
				}
				break;
			case "BED":
				//todo: remove
				//sendInt(packet, offset, 1);
				break;
			case "NAMED_ENTITY_SPAWN":
			case "SPAWN_ENTITY":
				Objects.requireNonNull(offset);
				sendDouble(logger, packet, offset);
				break;
			case "SPAWN_ENTITY_LIVING":
			case "WORLD_PARTICLES":
			case "SPAWN_ENTITY_WEATHER":
			case "SPAWN_ENTITY_EXPERIENCE_ORB":
			case "ENTITY_TELEPORT":
				sendDouble(logger, packet, offset);
				break;
			case "UNLOAD_CHUNK":
				sendChunk(logger, packet, offset, false, false);
				break;
			case "LIGHT_UPDATE":
				sendChunk(logger, packet, offset, false, true);
				break;
			case "MAP_CHUNK":
				sendChunk(logger, packet, offset, true, false);
				break;
			case "MULTI_BLOCK_CHANGE":
				sendChunkUpdate(logger, packet, offset);
				break;
			case "VIEW_CENTRE":
				sendIntChunk(logger, packet, offset);
				break;
			case "MAP_CHUNK_BULK":
				sendChunkBulk(logger, packet, offset);
				break;
			case "EXPLOSION":
				sendExplosion(logger, packet, offset);
				break;
			case "NAMED_SOUND_EFFECT":
				sendInt8(logger, packet, offset);
				break;
			case "ENTITY_METADATA":
				if (packet.getWatchableCollectionModifier().size() > 0) {
					packet.getWatchableCollectionModifier().modify(0, wrappedWatchableObjects -> {
						if (wrappedWatchableObjects == null)
							return null;
						ArrayList<WrappedWatchableObject> result = new ArrayList<WrappedWatchableObject>(wrappedWatchableObjects.size());
						for (WrappedWatchableObject wrappedWatchableObject : wrappedWatchableObjects) {
							if (wrappedWatchableObject == null) {
								result.add(null);
								continue;
							}

							Object oldValue = wrappedWatchableObject.getValue();
							if (oldValue instanceof Optional) {
								//noinspection rawtypes
								Optional opt = (Optional) oldValue;
								if (opt.isPresent()) {
									Object val = opt.get();
									if (TranslatorServerbound.BLOCKPOSITIONCLASS.isInstance(val)) {
										wrappedWatchableObject.setValue(Optional.of(offsetPositionMc(logger, offset, val)));
									}
								}
							}
							result.add(wrappedWatchableObject);
						}
						return result;
					});
				}
				break;
			case "TILE_ENTITY_DATA":
				sendTileEntityData(logger, packet, offset);
				break;
			case "PLAYER_INFO":
				break;
			default:
				logger.fine(packet.getType().name());
				break;
		}
		return false;
	}

	private static CoordinateOffset respawn(Logger logger, CoordinateOffset offset, Player player) {
		boolean hasSetLastLocation = false;
		Optional<Location> lastLocationOpt = PlayerManager.getLastPlayerLocation(player);
		if (lastLocationOpt.isPresent()) {
			Location lastLocation = lastLocationOpt.get();
			if (lastLocation.getWorld().getUID().equals(player.getLocation().getWorld().getUID())) {
				int clientViewDistance = 64; //player.getClientViewDistance();
				int minTeleportDistance = clientViewDistance * 2 * 16 + 2;
				minTeleportDistance *= minTeleportDistance; // squared
				if (lastLocation.distanceSquared(player.getLocation()) > minTeleportDistance) {
					offset = PlayerManager.teleportPlayer(player, player.getWorld(), true);
					logger.fine("Teleporting player. Prev[" + lastLocation.getBlockX() + "," + lastLocation.getBlockZ() + "] Next[" + player.getLocation().getX() + "," + player.getLocation().getZ() + "]" );
					hasSetLastLocation = true;
				}
			}
		}
		if (hasSetLastLocation) {
			PlayerManager.setLastPlayerLocation(player, player.getLocation());
		}
		return offset;
	}

	private static void fixWindowItems(Logger logger, PacketContainer packet, CoordinateOffset offset) {
		packet.getItemListModifier().modify(0, itemStacks -> {
			if (itemStacks == null)
				return null;
			List<ItemStack> newItems = new ArrayList<>(itemStacks.size());
			for (ItemStack itemStack : itemStacks) {
				if (itemStack == null) {
					newItems.add(null);
					continue;
				}
				newItems.add(transformItemStack(logger, itemStack, offset));
			}
			return newItems;
		});
	}

	private static void fixWindowItem(Logger logger, PacketContainer packet, CoordinateOffset offset) {
		packet.getItemModifier().modify(0, itemStack -> {
			if (itemStack == null) {
				return null;
			}
			return transformItemStack(logger, itemStack, offset);
		});
	}

	private static ItemStack transformItemStack(Logger logger, ItemStack itemStack, CoordinateOffset offset) {
		itemStack = itemStack.clone();

		if (itemStack.hasItemMeta()) {
			ItemMeta itemMeta = itemStack.getItemMeta();

			// Before 1.16.1
			if (COMPASSMETACLASS != null) {
				if (itemMeta instanceof org.bukkit.inventory.meta.CompassMeta) {
					org.bukkit.inventory.meta.CompassMeta compassMeta = (org.bukkit.inventory.meta.CompassMeta) itemMeta;
					Location lodestoneLocation = compassMeta.getLodestone();
					if (lodestoneLocation != null) {
						compassMeta.setLodestone(lodestoneLocation.subtract(offset.getXInt(), 0, offset.getZInt()));
						if (!itemStack.setItemMeta(compassMeta)) {
							logger.severe("Can't apply meta");
						}
					}
				}
			}
		}
		return itemStack;
	}


	private static void sendChunk(Logger logger, final PacketContainer packet, final CoordinateOffset offset, boolean includesEntities, boolean includeLight) {
		StructureModifier<Integer> integers = packet.getIntegers();
		integers.modify(0, curr_x -> {
			if (curr_x != null) {
				return curr_x - offset.getXChunk();
			} else {
				return null;
			}
		});
		integers.modify(1, curr_z -> {
			if (curr_z != null) {
				return curr_z - offset.getZChunk();
			} else {
				return null;
			}
		});
		if (includeLight) {
			StructureModifier<List<byte[]>> byteArrays = packet.getLists(Converters.passthrough(byte[].class));
			for (int i = 0; i < 2; i++) {
				byteArrays.modify(i, list -> {
					if (list == null) return null;
					ArrayList<byte[]> newList = new ArrayList<>(list);
					newList.replaceAll(bytes -> Arrays.copyOf(bytes, bytes.length));
					return newList;
				});
			}
		}
		if (includesEntities) {
			packet.getListNbtModifier().modify(0, entities -> {
				if (entities != null) {
					for (NbtBase<?> entity : entities) {
						NbtCompound entityCompound = (NbtCompound) entity;
						if (entityCompound.containsKey("x") && entityCompound.containsKey("z")) {
							entityCompound.put("x", entityCompound.getInteger("x") - offset.getXInt());
							entityCompound.put("z", entityCompound.getInteger("z") - offset.getZInt());
						}
					}
					return entities;
				} else {
					return null;
				}
			});
		}
	}


	private static void sendChunkBulk(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getIntegerArrays().size() > 1) {
			final int[] x = packet.getIntegerArrays().read(0).clone();
			final int[] z = packet.getIntegerArrays().read(1).clone();

			for (int i = 0; i < x.length; i++) {

				x[i] = x[i] - offset.getXChunk();
				z[i] = z[i] - offset.getZChunk();
			}

			packet.getIntegerArrays().write(0, x);
			packet.getIntegerArrays().write(1, z);
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void sendChunkUpdate(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (USE_1_16_R2) {
			Object sp = packet.getModifier().read(0);
			if (sp == null) {
				return;
			}
			if (!SECTIONPOSITIONCLASS.isInstance(sp)) {
				throw new RuntimeException("Wrong type");
			}
			try {
				int sectionX = (int) SectionPositionGetChunkXMethod.invoke(sp) - offset.getXChunk();
				int sectionY = (int) SectionPositionGetChunkYMethod.invoke(sp);
				int sectionZ = (int) SectionPositionGetChunkZMethod.invoke(sp) - offset.getZChunk();
				Object newSectionPosition = SectionPositionCreateMethod.invoke(null, sectionX, sectionY, sectionZ);
				packet.getModifier().write(0, newSectionPosition);
			} catch (IllegalAccessException | InvocationTargetException e) {
				logger.severe(e.getLocalizedMessage());
				e.printStackTrace();
			}
		} else {
			if (packet.getChunkCoordIntPairs().size() > 0) {
				ChunkCoordIntPair oldPair = packet.getChunkCoordIntPairs().read(0);
				final ChunkCoordIntPair newCoords = new ChunkCoordIntPair(
						oldPair.getChunkX() - offset.getXChunk(),
						oldPair.getChunkZ() - offset.getZChunk()
				);

				packet.getChunkCoordIntPairs().write(0, newCoords);
			} else {
				logger.severe("Packet size error");
			}
		}
	}


	private static void sendDouble(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getDoubles().size() > 2) {
			packet.getDoubles().modify(0, x -> x == null ? null : x - offset.getX());
			packet.getDoubles().modify(2, z -> z == null ? null : z - offset.getZ());
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendExplosion(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		sendDouble(logger, packet, offset);

		packet.getBlockPositionCollectionModifier().modify(0, lst -> {
			if (lst == null) return null;
			ArrayList<BlockPosition> newLst = new ArrayList<BlockPosition>(lst.size());
			for (BlockPosition blockPosition : lst) {
				newLst.add(blockPosition.subtract(new BlockPosition(offset.getXInt(), 0, offset.getZInt())));
			}
			return newLst;
		});
	}


	private static void sendFixedPointNumber(Logger logger, final PacketContainer packet, final CoordinateOffset offset, final int index) {
		if (packet.getIntegers().size() > 2) {
			packet.getIntegers().modify(index, curr_x -> curr_x == null ? null : curr_x - (offset.getXInt() << 5));
			packet.getIntegers().modify(index + 2, curr_z -> curr_z == null ? null : curr_z - (offset.getZInt() << 5));
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendFloat(Logger logger, final PacketContainer packet, final CoordinateOffset offset, final int index) {
		if (packet.getFloat().size() > 2) {
			packet.getFloat().modify(index, curr_x -> curr_x == null ? null : (float) (curr_x - offset.getX()));
			packet.getFloat().modify(index + 2, curr_z -> curr_z == null ? null : (float) (curr_z - offset.getZ()));
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void sendInt(Logger logger, final PacketContainer packet, final CoordinateOffset offset, final int index) {
		if (packet.getIntegers().size() > 2) {
		packet.getIntegers().modify(index, curr_x -> curr_x == null ? null : curr_x - offset.getXInt());
		packet.getIntegers().modify(index + 2, curr_z -> curr_z == null ? null : curr_z - offset.getZInt());
		} else {
			logger.severe("Packet size error");
		}
	}

	private static void sendIntChunk(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getIntegers().size() > 1) {
		packet.getIntegers().modify(0, curr_x -> curr_x == null ? null : curr_x - offset.getXChunk());
		packet.getIntegers().modify(1, curr_z -> curr_z == null ? null : curr_z - offset.getZChunk());
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendBlockPosition(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getBlockPositionModifier().size() > 0) {
			packet.getBlockPositionModifier().modify(0, pos -> offsetPosition(logger, offset, pos));
		}	else {
			logger.severe("Packet size error");
		}
	}

	private static BlockPosition offsetPosition(Logger logger, CoordinateOffset offset, BlockPosition pos) {
		if (pos == null) return null;
		return pos.subtract(new BlockPosition(offset.getXInt(), 0, offset.getZInt()));
	}

	private static Object offsetPositionMc(Logger logger, CoordinateOffset offset, Object pos) {
		if (pos == null) return null;
		try {
			return TranslatorServerbound.BlockPositionAddMethod.invoke(pos, -offset.getX(), -0d, -offset.getZ());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}


	private static void sendInt8(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		if (packet.getIntegers().size() > 2) {
			packet.getIntegers().modify(0, curr_x -> curr_x == null ? null : curr_x - (offset.getXInt() << 3));
			packet.getIntegers().modify(2, curr_z -> curr_z == null ? null : curr_z - (offset.getZInt() << 3));
		} else {
			logger.severe("Packet size error");
		}
	}


	private static void sendTileEntityData(Logger logger, final PacketContainer packet, final CoordinateOffset offset) {
		sendBlockPosition(logger, packet, offset);

		packet.getNbtModifier().modify(0, nbtBase -> {
			if (nbtBase == null) return null;
			if (nbtBase instanceof NbtCompound) {
				final NbtCompound nbt = (NbtCompound) (((NbtCompound) nbtBase).deepClone());
				if (nbt.containsKey("x") && nbt.containsKey("z")) {
					nbt.put("x", nbt.getInteger("x") - offset.getXInt());
					nbt.put("z", nbt.getInteger("z") - offset.getZInt());
				}
				return nbt;
			} else {
				return nbtBase;
			}
		});
	}
}
