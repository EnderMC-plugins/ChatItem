package me.dadus33.chatitem.chatmanager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.dadus33.chatitem.ItemSlot;
import me.dadus33.chatitem.utils.Messages;

public class ChatAction {

	private boolean hasItem;
	private ItemSlot slot;
	private String command, copyText;
	private ItemStack item;
	private Player origin;
	private Object[] placeholders;

	public ChatAction(ItemSlot slot, Player origin, ItemStack item, String command) {
		this.slot = slot;
		this.item = item == null ? new ItemStack(Material.AIR) : item;
		this.command = command;
		this.origin = origin;
		this.hasItem = true;
		this.placeholders = slot.getMessagePlaceholders(origin);
	}

	public ChatAction(ItemSlot slot, Player origin, String command) {
		this.slot = slot;
		this.command = command;
		this.origin = origin;
		this.hasItem = false;
		// values are taken now, so all players will see the same
		this.placeholders = slot.getMessagePlaceholders(origin);
		this.copyText = slot.getCopyText(origin);
	}

	public Player getOrigin() {
		return origin;
	}

	public ItemSlot getSlot() {
		return slot;
	}

	public boolean hasItem() {
		return hasItem;
	}

	public String getCommand() {
		return command;
	}

	public boolean hasCommand() {
		return command != null && command != "";
	}

	public String getCopyText() {
		return copyText;
	}

	public boolean hasCopyText() {
		return copyText != null && !copyText.isEmpty();
	}

	/**
	 * Get the text that replace the placeholder in chat (only for placeholders
	 * without item)
	 *
	 * @return the text to show in chat
	 */
	public String getChatText() {
		return Messages.getMessage(slot.name().toLowerCase() + ".chat", placeholders);
	}

	/**
	 * Get the hover text of the placeholder (only for placeholders without item)
	 *
	 * @return the hover text
	 */
	public String getHoverText() {
		return Messages.getMessage(slot.name().toLowerCase() + ".hover", placeholders);
	}

	public ItemStack getItem() {
		return item == null ? new ItemStack(Material.AIR) : item;
	}

	@Override
	public String toString() {
		return "ChatAction[slot=" + slot + ",command=" + command + ",item=" + item + ",origin=" + origin + "]";
	}
}
