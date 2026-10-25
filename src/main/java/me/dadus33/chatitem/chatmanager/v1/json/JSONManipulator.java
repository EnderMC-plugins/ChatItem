package me.dadus33.chatitem.chatmanager.v1.json;

import static me.dadus33.chatitem.utils.PacketUtils.getNmsClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.chatmanager.Chat;
import me.dadus33.chatitem.chatmanager.ChatAction;
import me.dadus33.chatitem.chatmanager.ChatManager;
import me.dadus33.chatitem.utils.PacketUtils;
import me.dadus33.chatitem.utils.ReflectionUtils;
import me.dadus33.chatitem.utils.Version;

public class JSONManipulator {

	private static JSONManipulator instance = new JSONManipulator();

	public static JSONManipulator getInstance() {
		return instance;
	}

	public static final Class<?> CRAFT_ITEM_STACK_CLASS = PacketUtils.getObcClass("inventory.CraftItemStack");
	public static final Class<?> NMS_ITEM_STACK_CLASS = getNmsClass("ItemStack", "world.item.");
	public static final Method AS_NMS_COPY = ReflectionUtils.getMethod(CRAFT_ITEM_STACK_CLASS, "asNMSCopy", ItemStack.class);
	public static final Class<?> NBT_TAG_COMPOUND = getNmsClass("NBTTagCompound", "nbt.", "CompoundTag");
	public static final Method SAVE_NMS_ITEM_STACK_METHOD = ReflectionUtils.getMethod(NMS_ITEM_STACK_CLASS, NBT_TAG_COMPOUND, NBT_TAG_COMPOUND);
	public static final Field MAP = ReflectionUtils.getField(NBT_TAG_COMPOUND, "map", "x", "tags");

	private JsonArray classicTooltip;

	/**
	 * Since MC 1.21.5, text components use snake_case keys (hover_event,
	 * click_event...) and the command of a click event is in "command"
	 */
	private static boolean isSnakeCase() {
		return Version.getVersion().isNewerOrEquals(Version.V1_21_5);
	}

	private static String getHoverEventKey() {
		return isSnakeCase() ? "hover_event" : "hoverEvent";
	}

	private static String getClickEventKey() {
		return isSnakeCase() ? "click_event" : "clickEvent";
	}

	/**
	 * Create the click event of the action: run the command, or copy the text
	 * (only suggested in chat before MC 1.15)
	 *
	 * @param action the action of the message
	 * @return the click event or null if nothing should happen on click
	 */
	private static JsonObject createClick(ChatAction action) {
		JsonObject click = new JsonObject();
		if (action.hasCommand()) {
			click.addProperty("action", "run_command");
			click.addProperty(isSnakeCase() ? "command" : "value", action.getCommand());
		} else if (action.hasCopyText()) {
			if (Version.getVersion().isNewerOrEquals(Version.V1_15)) {
				click.addProperty("action", "copy_to_clipboard");
				click.addProperty("value", action.getCopyText());
			} else {
				click.addProperty("action", "suggest_command");
				click.addProperty("value", action.getCopyText());
			}
		} else
			return null;
		return click;
	}

	public static JsonObject parseOrGet(String json) {
		if(json.startsWith("\"") && json.endsWith("\"")) {// seems to be simple line
			if(json.startsWith("\"{") && json.endsWith("}\"")) // json in string
				return JsonParser.parseString(json.substring(1, json.length() - 1).replace("\\\"", "\"")).getAsJsonObject();
			return JsonParser.parseString("{\"text\":" + json + "}").getAsJsonObject();
		}
		try {
			return JsonParser.parseString(json).getAsJsonObject();
		} catch (Exception e) {
			JsonObject obj = new JsonObject();
			obj.addProperty("text", json);
			return obj;
		}
	}

	public String parse(Chat chat, String json, ChatAction action, String replacement) throws Exception {
		JsonObject obj = parseOrGet(json);

		JsonObject wrapper = new JsonObject(); // Create a wrapper object for the whole array
		JsonArray use = Translator.toJson(replacement); // We get the json representation of the old color
														// formatting method
		ChatItem.debug("Remplacement: " + replacement + " use: " + use.toString());
		// There's no public clone method for JSONObjects so we need to parse them every
		// time
		JsonObject hover = new JsonObject();
		JsonObject itemHover = action.hasItem() && Version.getVersion().isNewerOrEquals(Version.V1_20_6) ? PacketUtils.getItemHoverJson(action.getItem()) : null;
		if (itemHover != null) { // since 1.20.5, data components can only be written by the server
			hover = itemHover;
		} else if (action.hasItem()) {
			hover.addProperty("action", "show_item");

			// Get the JSON representation of the item (well, not really JSON, but rather a
			// string representation of NBT data)
			String item = ChatItem.getPlatform().stringifyItem(action.getItem());
			hover.addProperty("value", item);
			hover.add("contents", parseOrGet(item));
		} else {
			hover.addProperty("action", "show_text");

			// Get the JSON representation of the item (well, not really JSON, but rather a
			// string representation of NBT data)
			JsonArray hoverArray = new JsonArray();
			hoverArray.add(action.getHoverText());
			hover.add("value", hoverArray);

			JsonObject click = createClick(action);
			if (click != null)
				wrapper.add(getClickEventKey(), click);
		}

		if (use.size() == 1) {
			JsonElement extraElement = use.get(0);
			if (extraElement.isJsonPrimitive())
				wrapper.addProperty("text", extraElement.getAsString());
			else if (extraElement.isJsonObject())
				wrapper.addProperty("text", extraElement.getAsJsonObject().get("text").getAsString());
			else
				wrapper.add("extra", use); // add it only if
		} else if (!use.isEmpty())
			wrapper.add("extra", use); // add it only if
		if (!wrapper.has("text"))
			wrapper.addProperty("text", ""); // The text field is compulsory, even if it's empty
		ChatItem.debug("Wrapper " + wrapper + " > " + use);
		wrapper.add(getHoverEventKey(), hover);

		String separator = ChatManager.addSeparator(chat.getId());
		moveTextToExtra(obj, separator);
		if(!obj.has("extra"))
			obj.add("extra", new JsonArray());
		JsonArray result = parseArray(obj.getAsJsonArray("extra"), wrapper, separator);
		ChatItem.debug("Parsing array " + obj.getAsJsonArray("extra") + " FROM: " + obj + ". Result: " + result);
		obj.add("extra", result);
		if (!obj.has("text")) {
			obj.addProperty("text", "");
		}
		return obj.toString();
	}

	/**
	 * Move the text of the root object which contains the separator into the
	 * extra, so the separator can be replaced like others
	 *
	 * @param obj       the root object
	 * @param separator the separator of the chat
	 */
	private static void moveTextToExtra(JsonObject obj, String separator) {
		JsonElement text = obj.get("text");
		if (text == null || !text.isJsonPrimitive() || !ChatManager.fixSeparator(text.getAsString()).contains(separator))
			return;
		JsonArray extra = new JsonArray();
		extra.add(text);
		if (obj.has("extra") && obj.get("extra").isJsonArray())
			extra.addAll(obj.getAsJsonArray("extra"));
		obj.add("extra", extra);
		obj.addProperty("text", "");
	}

	@SuppressWarnings("deprecation")
	public String parseEmpty(Chat chat, String json, List<String> tooltip, Player sender) {
		JsonObject obj = parseOrGet(json);
		String separator = ChatManager.addSeparator(chat.getId());
		moveTextToExtra(obj, separator);
		JsonArray array = obj.has("extra") ? obj.getAsJsonArray("extra") : new JsonArray();
		JsonArray use = Translator.toJson(ChatManager.getNameForChatAction(chat.getPlayer(), chat, ChatItem.getInstance().getStorage()).replace("{name}", sender.getName()).replace("{display-name}", sender.getDisplayName()));
		JsonObject hover = JsonParser.parseString("{\"action\":\"show_text\", \"value\": \"\"}").getAsJsonObject();

		StringBuilder oneLineTooltip = new StringBuilder("");
		int index = 0;
		for (String m : tooltip) {
			oneLineTooltip.append(m.replace("{name}", sender.getName()).replace("{display-name}", sender.getDisplayName()));
			++index;
			if (index != tooltip.size()) {
				oneLineTooltip.append('\n');
			}
		}

		hover.add("value", new JsonPrimitive(oneLineTooltip.toString()));
		if (!tooltip.isEmpty()) {
			for (JsonElement ob : use)
				ob.getAsJsonObject().add(getHoverEventKey(), hover);
			classicTooltip = use;
		}
		obj.add("extra", parseArray(array, classicTooltip, separator));
		if (!obj.has("text")) {
			obj.addProperty("text", "");
		}
		return obj.toString();
	}

	/**
	 * Replace the separator by the tooltip in all elements of the array. Separator
	 * of other chats are kept, as a message can have many placeholders
	 *
	 * @param arr       the array to parse
	 * @param tooltip   the element which replace the separator
	 * @param separator the separator of the actual chat, with it's ID
	 * @return the new array
	 */
	private JsonArray parseArray(JsonArray arr, JsonElement tooltip, String separator) {
		JsonArray replacer = new JsonArray();
		for (int i = 0; i < arr.size(); ++i) {
			JsonElement element = arr.get(i);
			if(element.isJsonNull())
				continue;
			/*if(separator) {
				if(ChatManager.containsSeparatorEnd(element.toString()))
					separator = false;
				continue;
			}*/
			if (element.isJsonObject()) {
				JsonObject o = element.getAsJsonObject();
				JsonElement text = o.get("text");
				ChatItem.debug("adding JsonObject " + o + " (text: " + text + ")");
				if(text != null && !text.getAsString().isEmpty()) {
					addParsedStringToArray(text.getAsString(), replacer, o, tooltip, separator);
				}
				if (o.has("extra") && !o.get("extra").getAsJsonArray().isEmpty()) {
					JsonArray jar = o.get("extra").getAsJsonArray();
					JsonArray tmpArray = parseArray(jar, tooltip, separator);
					if (!tmpArray.isEmpty()) {
						o.add("extra", tmpArray);
						replacer.add(o);
					}
					ChatItem.debug("[addParsedJsonObjectToArray] " + jar + " parsed " + tmpArray + " into " + replacer);
				}
				
			} else if (element.isJsonArray()) {
				JsonArray jar = element.getAsJsonArray();
				if (!jar.isEmpty()) {
					replacer.set(i, parseArray(element.getAsJsonArray(), tooltip, separator));
				}
			} else if(element.isJsonPrimitive()) {
				if(ChatManager.containsSeparator(element.getAsString())) {
					addParsedStringToArray(element.getAsString(), replacer, element, tooltip, separator);
				} else
					replacer.add(element); // add basic element
			}
		}
		return replacer;
	}

	private void addParsedStringToArray(String msg, JsonArray rep, JsonElement o, JsonElement tooltip, String separator) {
		msg = ChatManager.fixSeparator(msg);
		if (!msg.contains(separator)) { // nothing to replace, or only separator of others chats
			rep.add(o.deepCopy());
			return;
		}
		ChatItem.debug("[JSONManipulator] Parsed string " + msg + ", rep: " + rep);
		String[] parts = msg.split(Pattern.quote(separator), -1);
		for (int i = 0; i < parts.length; i++) {
			if (i > 0)
				rep.add(tooltip);
			String current = parts[i];
			if (current.isEmpty())
				continue;
			if(o.isJsonObject()) {
				JsonObject jsonObj = o.getAsJsonObject().deepCopy();
				jsonObj.addProperty("text", current); // edit text
				rep.add(jsonObj); // add with all basic coloring things
			} else {
				rep.add(current);
			}
		}
	}
}
