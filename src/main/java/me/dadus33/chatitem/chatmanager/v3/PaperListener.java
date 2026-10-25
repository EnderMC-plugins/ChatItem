package me.dadus33.chatitem.chatmanager.v3;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.dadus33.chatitem.C;
import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.ItemSlot;
import me.dadus33.chatitem.Storage;
import me.dadus33.chatitem.chatmanager.Chat;
import me.dadus33.chatitem.chatmanager.ChatAction;
import me.dadus33.chatitem.chatmanager.ChatManager;
import me.dadus33.chatitem.platform.hook.PaperPlatform;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;

public class PaperListener implements Listener {

	private PaperChatManager manage;

	public PaperListener(PaperChatManager manage) {
		this.manage = manage;
	}

	public Storage getStorage() {
		return manage.getStorage();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onChat(AsyncChatEvent e) {
		if (ChatManager.isTestingEnabled() && !ChatManager.isTesting("paper"))
			return;
		if (!ChatManager.isSelected("paper"))
			return;

		Player p = e.getPlayer();
		Component message = e.message();
		String rawMessage = C.string(message);
		// fix for v1: put back all placeholders
		for (int i = 0; i < 50 && ChatManager.containsSeparator(rawMessage); i++) {
			Chat chat = Chat.getFrom(rawMessage);
			if (chat == null)
				break;
			message = message.replaceText(TextReplacementConfig.builder().matchLiteral(ChatManager.addSeparator(chat.getId()))
					.replacement(C.text(chat.getSlot().getPlaceholders().get(0))).build());
			String next = C.string(message);
			if (next.equals(rawMessage))
				break;
			rawMessage = next;
		}
		List<ItemSlot> slots = ItemSlot.getItemSlotsFromMessage(rawMessage);
		ChatItem.debug("(v3) Raw message: " + rawMessage + ", slots: " + slots);
		if (slots.isEmpty()) // if not found
			return;
		boolean used = false;
		for (ItemSlot slot : slots) {
			ChatAction action = ChatManager.getChatAction(slot, p);
			if (!ChatManager.canUsePlaceholder(p, action, slot, e)) {
				if (e.isCancelled())
					return;
				continue;
			}
			TextComponent like = PaperPlatform.createActionComponent(p, action);
			for (String s : slot.getPlaceholders()) {
				message = message.replaceText(TextReplacementConfig.builder().matchLiteral(s).replacement(like).build());
			}
			used = true;
		}
		if (!used)
			return;
		if (ChatItem.getInstance().getConfig().getBoolean("manager-config.paper.send-ourself", true)) {
			for (Audience a : e.viewers().isEmpty() ? Bukkit.getOnlinePlayers() : e.viewers())
				a.sendMessage(e.renderer().render(p, p.displayName(), message, a));
			e.setCancelled(true);
			ChatItem.debug("Sent message by ourself : " + C.string(message));
		} else {
			e.message(message);
			ChatItem.debug("Changed message to " + C.string(message));
		}
		if (getStorage().cooldown > 0 && !p.hasPermission("chatitem.ignore-cooldown"))
			ChatManager.applyCooldown(p);
	}
}
