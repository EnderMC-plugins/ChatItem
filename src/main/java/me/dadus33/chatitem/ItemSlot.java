package me.dadus33.chatitem;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.dadus33.chatitem.chatmanager.ChatManager;
import me.dadus33.chatitem.hook.VaultEconomySupport;
import me.dadus33.chatitem.utils.Colors;

public enum ItemSlot {

	HAND("", false),
	HELMET("helmet", false),
	CHESTPLATE("chestplate", false),
	LEGGINGS("leggings", false),
	BOOTS("boots", false),
	INVENTORY("inventory", true),
	ENDERCHEST("enderchest", true),
	MONEY("money", false, true),
	POSITION("position", false, true),
	HEALTH("health", false, true),
	LEVEL("level", false, true);

	private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT));
	private final boolean command, info;
	private final String key;

	private ItemSlot(String key, boolean command) {
		this(key, command, false);
	}

	private ItemSlot(String key, boolean command, boolean info) {
		this.command = command;
		this.info = info;
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
	
	public boolean isBasic() {
		return key.equalsIgnoreCase("");
	}
	
	public boolean isCommand() {
		return command;
	}

	/**
	 * Check if the placeholder only show an information about the player (money,
	 * position...) instead of an item or an inventory
	 *
	 * @return true if it's an information placeholder
	 */
	public boolean isInfo() {
		return info;
	}

	public List<String> getPlaceholders() {
		return ChatItem.getInstance().getConfig().getStringList(isBasic() ? "general.placeholders" : "general.other-placeholders." + key + ".keys");
	}

	public boolean isEnabled() {
		return isBasic() || (ChatItem.getInstance().getConfig().getBoolean("general.other-placeholders." + key + ".enabled", true) && isAvailable());
	}

	private boolean isAvailable() {
		if (this == MONEY) // need an economy plugin
			return Bukkit.getPluginManager().isPluginEnabled("Vault") && VaultEconomySupport.isAvailable();
		return true;
	}

	/**
	 * Get placeholders used in messages of this slot, with their values for the
	 * given player
	 *
	 * @param p the player that use the placeholder
	 * @return the placeholders and their values
	 */
	public Object[] getMessagePlaceholders(Player p) {
		switch (this) {
		case MONEY:
			return new Object[] { "%cible%", p.getName(), "%money%", VaultEconomySupport.getBalance(p) };
		case POSITION:
			Location loc = p.getLocation();
			return new Object[] { "%cible%", p.getName(), "%x%", loc.getBlockX(), "%y%", loc.getBlockY(), "%z%", loc.getBlockZ(), "%world%", loc.getWorld().getName() };
		case HEALTH:
			return new Object[] { "%cible%", p.getName(), "%health%", HEALTH_FORMAT.format(p.getHealth()), "%max_health%", HEALTH_FORMAT.format(p.getMaxHealth()) };
		case LEVEL:
			return new Object[] { "%cible%", p.getName(), "%level%", p.getLevel() };
		default:
			return new Object[] { "%cible%", p.getName() };
		}
	}

	/**
	 * Get the text to copy when clicking on the placeholder
	 *
	 * @param p the player that use the placeholder
	 * @return the text to copy or null if nothing to copy
	 */
	public String getCopyText(Player p) {
		if (this == POSITION) {
			Location loc = p.getLocation();
			return loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
		}
		return null;
	}
	
	public boolean isDenyIfNoItem() {
		return ChatItem.getInstance().getConfig().getBoolean("general" + (isBasic() ? "" : ".other-placeholders." + key) + ".deny-if-no-item", true);
	}
	 
	public String getShowMessage() {
		// without default value to use the one of the default config if missing
		String msg = ChatItem.getInstance().getConfig().getString("general" + (isBasic() ? "" : ".other-placeholders." + key) + ".show");
		return Colors.color(msg == null ? "" : msg);
	}
	
	public boolean hasPlaceholders(String message) {
		for (String rep : getPlaceholders()) {
			if (message.contains(rep)) {
				return true;
			}
		}
		return false;
	}
	
	public String replacePlaceholdersToSeparator(String message) {
		return replacePlaceholders(message, Character.toString(ChatManager.SEPARATOR));
	}

	public String replacePlaceholders(String message, String replacement) {
		for(String rep : getPlaceholders())
			message = message.replace(rep, replacement);
		return message;
	}

	public static ItemSlot getItemSlotFromMessage(String message) {
		for(ItemSlot slot : ItemSlot.values()) {
			if(slot.isEnabled() && slot.hasPlaceholders(message)) {
				return slot;
			}
		}
		return null;
	}

	/**
	 * Get all slots that have placeholders in the message
	 *
	 * @param message the message to check
	 * @return all slots found, or an empty list
	 */
	public static List<ItemSlot> getItemSlotsFromMessage(String message) {
		List<ItemSlot> slots = new ArrayList<>();
		for(ItemSlot slot : ItemSlot.values()) {
			if(slot.isEnabled() && slot.hasPlaceholders(message)) {
				slots.add(slot);
			}
		}
		return slots;
	}
	
	public static ItemSlot getItemSlotByKey(String key) {
		for(ItemSlot slot : ItemSlot.values()) {
			if(slot.isEnabled() && slot.getKey().equalsIgnoreCase(key)) {
				return slot;
			}
		}
		return null;
	}
}
