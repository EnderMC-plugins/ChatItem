package me.dadus33.chatitem.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.chatmanager.v1.json.JSONManipulator;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class PacketUtils {

	public static final String NMS_PREFIX;
	public static final String OBC_PREFIX;
	public static final boolean IS_THERMOS = Utils.isClassExist("thermos.Thermos");
	public static final Class<?> COMPONENT_CLASS;

	private static Method getNbtMethod, sendPacketMethod, itemHoverMethod;
	private static List<Method> itemComponentMethods;

	/**
	 * This Map is to reduce Reflection action which take more resources than just
	 * RAM action
	 */
	private static final Map<String, Class<?>> ALL_CLASS = Collections.synchronizedMap(new HashMap<String, Class<?>>());

	static {
		String version = ChatItem.getPlatform().getNMSVersion();
		NMS_PREFIX = Version.getVersion().isNewerOrEquals(Version.V1_17) || IS_THERMOS ? "net.minecraft." : "net.minecraft.server." + (version.isEmpty() ? "" : version + ".");
		OBC_PREFIX = "org.bukkit.craftbukkit." + (version.isEmpty() ? "" : version + ".");
		COMPONENT_CLASS = getNmsClass("IChatBaseComponent", "network.chat.", "Component");
	}

	/**
	 * Get the Class in NMS, with a processing reducer
	 * 
	 * @param name of the NMS class (in net.minecraft.server package ONLY, because it's NMS)
	 * @return clazz the searched class
	 */
	public static Class<?> getNmsClass(String name, String packagePrefix, String... alias) {
		return ALL_CLASS.computeIfAbsent(name, (a) -> {
			String fullPrefix = NMS_PREFIX + (Version.getVersion().isNewerOrEquals(Version.V1_17) || IS_THERMOS ? packagePrefix : "");
			try {
				Class<?> clazz = Class.forName(fullPrefix + name);
				if (clazz != null)
					return clazz;
			} catch (ClassNotFoundException e) {}

			for (String className : alias) {
				try {
					Class<?> clazz = Class.forName(fullPrefix + className);
					if (clazz != null)
						return clazz;
				} catch (ClassNotFoundException e) {}
			}
			return null;
		});
	}

	/**
	 * Get the Class in NMS, with a processing reducer
	 * 
	 * @param name of the NMS class (in net.minecraft.server package ONLY, because
	 *             it's NMS)
	 * @return clazz the searched class
	 */
	public static Class<?> getObcClass(String name, String... alias) {
		return ALL_CLASS.computeIfAbsent(name, (a) -> {
			try {
				Class<?> clazz = Class.forName(OBC_PREFIX + name);
				if (clazz != null)
					return clazz;
			} catch (ClassNotFoundException e) {
				if (alias.length == 0)
					e.printStackTrace(); // no alias, print error
				// else ignore and go check for alias
			}

			for (String className : alias) {
				try {
					Class<?> clazz = Class.forName(OBC_PREFIX + className);
					if (clazz != null)
						return clazz;
				} catch (ClassNotFoundException e) {
					if (className == alias[alias.length - 1]) // if it's last alias, print error
						e.printStackTrace();
				}
			}
			return null;
		});
	}

	/**
	 * Get NMS entity player of specified one
	 * 
	 * @param p the player that we want the NMS entity player
	 * @return the entity player
	 */
	public static Object getEntityPlayer(Player p) {
		try {
			return getObcClass("entity.CraftPlayer").getMethod("getHandle").invoke(p);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get NMS player connection of specified player
	 * 
	 * @param p Player of which we want to get the player connection
	 * @return the NMS player connection
	 */
	public static Object getPlayerConnection(Player p) {
		try {
			Object entityPlayer = getEntityPlayer(p);
			return ReflectionUtils.getFirstWith(entityPlayer, entityPlayer.getClass(), getNmsClass("ServerGamePacketListenerImpl", "server.network.", "PlayerConnection"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the NMS player list of the server
	 *
	 * @return the player list
	 */
	public static Object getCraftServer() {
		try {
			return getObcClass("CraftServer").getMethod("getHandle").invoke(Bukkit.getServer());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Send the packet to the specified player
	 * 
	 * @param p      which will receive the packet
	 * @param packet the packet to sent
	 */
	public static void sendPacket(Player p, Object packet) {
		try {
			Object playerConnection = getPlayerConnection(p);
			if(sendPacketMethod == null) {
				Version v = Version.getVersion();
				if(v.isNewerOrEquals(Version.V1_20)) {
					Class<?> packetSrvClass = getNmsClass("ServerCommonPacketListenerImpl", "server.network.");
					Class<?> packetClass = getNmsClass("Packet", "network.protocol.");
					// "send" with mojang mappings, "sendPacket" is added by CraftBukkit, "b" with spigot mappings
					if(packetSrvClass != null) {
						for(String name : Arrays.asList("send", "sendPacket", "b")) {
							try {
								sendPacketMethod = packetSrvClass.getDeclaredMethod(name, packetClass);
								break;
							} catch (NoSuchMethodException e) {}
						}
					}
				}
				if(sendPacketMethod == null)
					sendPacketMethod = playerConnection.getClass().getMethod(Version.getVersion().isNewerOrEquals(Version.V1_18) ? "a" : "sendPacket", getNmsClass("Packet", "network.protocol."));
			}
			sendPacketMethod.invoke(playerConnection, packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string for
	 * sending with {@link net.md_5.bungee.api.chat.BaseComponent}'s.
	 *
	 * @param item the item to convert
	 * @return the Json string representation of the item
	 */
	public static String getNbtTag(ItemStack item) {
		try {
			if (getNbtMethod == null) {
				Class<?> itemClass = getNmsClass("ItemStack", "world.item.");
				Version v = Version.getVersion();
				if(v.isNewerOrEquals(Version.V1_20_6)) // the method that return the DataComponentPatch, named differently according to mappings
					getNbtMethod = ReflectionUtils.getMethod(itemClass, getNmsClass("DataComponentPatch", "core.component."));
				else if (v.equals(Version.V1_20))
					getNbtMethod = itemClass.getDeclaredMethod("w");
				else if (v.equals(Version.V1_19))
					getNbtMethod = itemClass.getDeclaredMethod("u");
				else if (v.equals(Version.V1_18))
					getNbtMethod = itemClass.getDeclaredMethod("t");
				else
					getNbtMethod = itemClass.getDeclaredMethod("getTag");

				getNbtMethod.setAccessible(true);
			}
			Object tag = getNbtMethod.invoke(JSONManipulator.AS_NMS_COPY.invoke(null, item));
			return tag == null ? "{}" : tag.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	/**
	 * Get the registry access of the server, needed by some serializer since MC
	 * 1.20.5
	 *
	 * @return the registry access
	 * @throws Exception if something gone wrong
	 */
	public static Object getRegistryAccess() throws Exception {
		try {
			return getObcClass("CraftRegistry").getMethod("getMinecraftRegistry").invoke(null);
		} catch (NoSuchMethodException | NullPointerException e) {}
		Class<?> serverClass = getNmsClass("MinecraftServer", "server.");
		Object server = serverClass.getMethod("getServer").invoke(null);
		for (String name : Arrays.asList("registryAccess", "ba")) {
			try {
				return serverClass.getMethod(name).invoke(server);
			} catch (NoSuchMethodException e) {}
		}
		return null;
	}

	/**
	 * Create the "show_item" hover event of the given item, as JSON in the format
	 * used by the server.<br>
	 * Since MC 1.20.5, items have data components that can't be written by hand
	 * nor by the BungeeCord API. So we let the server create the hover: the NMS
	 * method that gives the display name of an item ("[Name]") includes it.
	 *
	 * @param item the item to show
	 * @return the hover event JSON object, or null if it can't be created
	 */
	public static JsonObject getItemHoverJson(ItemStack item) {
		try {
			Object nmsItem = JSONManipulator.AS_NMS_COPY.invoke(null, item);
			if (itemHoverMethod != null)
				return findShowItemHover(nmsItem, itemHoverMethod);
			if (itemComponentMethods == null) {
				itemComponentMethods = new ArrayList<>();
				for (Method m : getNmsClass("ItemStack", "world.item.").getDeclaredMethods()) {
					if (m.getParameterCount() == 0 && COMPONENT_CLASS.isAssignableFrom(m.getReturnType())) {
						m.setAccessible(true);
						if (m.getName().equals("getDisplayName")) // mojang mappings, so check it first
							itemComponentMethods.add(0, m);
						else
							itemComponentMethods.add(m);
					}
				}
			}
			for (Method m : itemComponentMethods) {
				JsonObject hover = findShowItemHover(nmsItem, m);
				if (hover != null) {
					itemHoverMethod = m; // the one that creates the hover, keep it
					return hover;
				}
			}
			ChatItem.debug("Can't find NMS method to create item hover in " + itemComponentMethods.size() + " methods");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static JsonObject findShowItemHover(Object nmsItem, Method m) throws Exception {
		Object comp = m.invoke(nmsItem);
		if (comp == null)
			return null;
		String json = ChatItem.getPlatform().baseComponentToJson(comp);
		return json == null ? null : findShowItemHover(JsonParser.parseString(json));
	}

	private static JsonObject findShowItemHover(JsonElement element) {
		if (element.isJsonArray()) {
			for (JsonElement e : element.getAsJsonArray()) {
				JsonObject hover = findShowItemHover(e);
				if (hover != null)
					return hover;
			}
		} else if (element.isJsonObject()) {
			JsonObject obj = element.getAsJsonObject();
			for (String key : Arrays.asList("hover_event", "hoverEvent")) {
				JsonElement hover = obj.get(key);
				if (hover != null && hover.isJsonObject() && hover.getAsJsonObject().has("action") && hover.getAsJsonObject().get("action").getAsString().equals("show_item"))
					return hover.getAsJsonObject();
			}
			for (String key : Arrays.asList("extra", "with")) {
				if (obj.has(key)) {
					JsonObject hover = findShowItemHover(obj.get(key));
					if (hover != null)
						return hover;
				}
			}
		}
		return null;
	}

	/**
	 * Replace all "show_item" hover events of the JSON message by the one of the
	 * same item (or the first one if not found).
	 *
	 * @param element the JSON message
	 * @param hovers  the hover events to use, by item key
	 */
	public static void replaceShowItemHover(JsonElement element, Map<String, JsonObject> hovers) {
		if (element.isJsonArray()) {
			for (JsonElement e : element.getAsJsonArray())
				replaceShowItemHover(e, hovers);
		} else if (element.isJsonObject()) {
			JsonObject obj = element.getAsJsonObject();
			for (String key : Arrays.asList("hover_event", "hoverEvent")) {
				JsonElement actual = obj.get(key);
				if (actual != null && actual.isJsonObject() && actual.getAsJsonObject().has("action") && actual.getAsJsonObject().get("action").getAsString().equals("show_item")) {
					JsonObject hover = hovers.get(getItemKey(actual.getAsJsonObject()));
					obj.add(key, hover == null ? hovers.values().iterator().next() : hover);
				}
			}
			for (String key : Arrays.asList("extra", "with")) {
				if (obj.has(key))
					replaceShowItemHover(obj.get(key), hovers);
			}
		}
	}

	private static String getItemKey(String id, int amount) {
		return (id.contains(":") ? id : "minecraft:" + id) + "|" + amount;
	}

	private static String getItemKey(JsonObject hover) {
		JsonObject content = hover.has("contents") && hover.get("contents").isJsonObject() ? hover.getAsJsonObject("contents") : hover;
		int amount = 1;
		for (String key : Arrays.asList("count", "Count"))
			if (content.has(key) && content.get(key).isJsonPrimitive())
				amount = content.get(key).getAsInt();
		return getItemKey(content.has("id") ? content.get("id").getAsString() : "", amount);
	}

	/**
	 * Convert components to JSON, in the format used by the server (it changes
	 * since MC 1.21.5)
	 *
	 * @param comps the components to convert
	 * @return the JSON
	 */
	public static String bungeeToJson(BaseComponent... comps) {
		try {
			Class<?> chatMessageClass = Class.forName(OBC_PREFIX + "util.CraftChatMessage");
			try { // paper
				return (String) chatMessageClass.getMethod("bungeeToJson", BaseComponent[].class).invoke(null, (Object) comps);
			} catch (NoSuchMethodException e) {}
			try { // spigot
				Object serializer = chatMessageClass.getMethod("getBungee").invoke(null);
				return (String) serializer.getClass().getMethod("toString", BaseComponent[].class).invoke(serializer, (Object) comps);
			} catch (NoSuchMethodException e) {}
		} catch (ClassNotFoundException e) {
			// old version
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ComponentSerializer.toString(comps);
	}

	/**
	 * Convert components to JSON, with the "show_item" hover events created by the
	 * server, as BungeeCord API can't show data components (since MC 1.20.5)
	 *
	 * @param comps the components to convert
	 * @param items the items to show
	 * @return the JSON, or null if a hover event can't be created
	 */
	public static String bungeeToJsonWithItems(BaseComponent[] comps, List<ItemStack> items) {
		Map<String, JsonObject> hovers = new LinkedHashMap<>();
		for (ItemStack item : items) {
			JsonObject hover = getItemHoverJson(item);
			if (hover == null)
				return null;
			hovers.putIfAbsent(getItemKey(item.getType().getKey().toString(), item.getAmount()), hover);
		}
		JsonElement json = JsonParser.parseString(bungeeToJson(comps));
		replaceShowItemHover(json, hovers);
		return json.toString();
	}

	/**
	 * Send a JSON message to the player, as system chat message (only for MC
	 * 1.19+)
	 *
	 * @param p    the player that will receive the message
	 * @param json the message in the JSON format of the server
	 * @return true if sent
	 */
	public static boolean sendSystemChat(Player p, String json) {
		try {
			Object comp = ChatItem.getPlatform().jsonToBaseComponent(json);
			if (comp == null)
				return false;
			Class<?> packetClass = getNmsClass("ClientboundSystemChatPacket", "network.protocol.game.");
			Constructor<?> cons = packetClass.getConstructor(COMPONENT_CLASS, boolean.class);
			sendPacket(p, cons.newInstance(comp, false));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void printPacketToDebug(Object packet) {
		if(!ChatItem.hasDebug())
			return;
		if(packet == null) {
			ChatItem.debug("Packet is null");
			return;
		}
		ChatItem.debug("Packet with Class: " + packet.getClass().getSimpleName());
		for (Field f : packet.getClass().getDeclaredFields()) {
			f.setAccessible(true);
			try {
				ChatItem.debug(f.getName() + ": " + f.get(packet));
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
	}
}
