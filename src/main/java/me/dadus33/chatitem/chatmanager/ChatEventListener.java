package me.dadus33.chatitem.chatmanager;

import static me.dadus33.chatitem.chatmanager.ChatManager.SEPARATOR;
import static me.dadus33.chatitem.chatmanager.ChatManager.SEPARATOR_END;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.ItemSlot;
import me.dadus33.chatitem.Storage;
import me.dadus33.chatitem.utils.Utils;

@SuppressWarnings("deprecation")
public class ChatEventListener implements Listener {

	private Storage getStorage() {
		return ChatItem.getInstance().getStorage();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncPlayerChatEvent e) {
		if (ChatManager.containsSeparator(e.getMessage())) { // If the BELL character is found, we have to remove it
			e.setMessage(ChatManager.removeSeparator(e.getMessage()));
		}
		Player p = e.getPlayer();
		String oldMsg = e.getMessage();
		List<ItemSlot> slots = ItemSlot.getItemSlotsFromMessage(oldMsg);
		if (slots.isEmpty()) {
			ChatItem.debug("(general) not found placeholders in: " + oldMsg);
			return;
		}
		String s = oldMsg;
		for (ItemSlot slot : slots) { // each kind of placeholder has it's own chat
			ChatAction action = ChatManager.getChatAction(slot, p);
			if (!ChatManager.canUsePlaceholder(p, action, slot, e)) {
				if (e.isCancelled())
					return;
				continue;
			}
			Chat c = Chat.create(p, oldMsg, action);
			ChatItem.debug("(general) Set placeholder to message " + c + " for " + slot);
			s = slot.replacePlaceholders(s, SEPARATOR + Integer.toString(c.getId()) + SEPARATOR_END);
		}
		if (s.equals(oldMsg))
			return;
		if (Utils.countMatches(s, Character.toString(ChatManager.SEPARATOR)) > getStorage().limit) {
			e.setCancelled(true);
			if (!getStorage().messageLimit.isEmpty())
				p.sendMessage(getStorage().messageLimit);
			return;
		}
		e.setMessage(s);
		e.setFormat(e.getFormat().replace(oldMsg, e.getMessage())); // set own message for plugin that already put the message into the format
	}
}
