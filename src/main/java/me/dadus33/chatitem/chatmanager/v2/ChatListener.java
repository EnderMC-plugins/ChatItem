package me.dadus33.chatitem.chatmanager.v2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.ItemSlot;
import me.dadus33.chatitem.Storage;
import me.dadus33.chatitem.chatmanager.Chat;
import me.dadus33.chatitem.chatmanager.ChatAction;
import me.dadus33.chatitem.chatmanager.ChatManager;
import me.dadus33.chatitem.hook.DiscordSrvSupport;
import me.dadus33.chatitem.playernamer.PlayerNamerManager;
import me.dadus33.chatitem.utils.Utils;
import net.md_5.bungee.api.ChatColor;

@SuppressWarnings("deprecation")
public class ChatListener implements Listener {

	private ChatListenerChatManager manage;

	public ChatListener(ChatListenerChatManager manage) {
		this.manage = manage;
	}

	public Storage getStorage() {
		return manage.getStorage();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onChat(AsyncPlayerChatEvent e) {
		if(ChatManager.isTestingEnabled() && !ChatManager.isTesting("chat"))
			return;
		if(!ChatManager.isSelected("chat"))
			return;
		Storage c = getStorage();
		if (e.isCancelled()) {
			if (ChatItem.getInstance().getChatManager().size() == 1) { // only chat
				String msg = e.getMessage().toLowerCase();
				for(ItemSlot slot : ItemSlot.values()) {
					for (String rep : slot.getPlaceholders()) {
						if (msg.contains(rep)) {
							ChatItem.debug("You choose 'chat' manager but it seems to don't be the good choice. More informations here: https://github.com/dadus33-plugins/ChatItem/wiki");
							return;
						}
					}
				}
			}
			ChatItem.debug("Chat cancelled for " + e.getPlayer().getName());
			return;
		}
		Player p = e.getPlayer();
		String format = e.getFormat();
		// fix for v1: put back all placeholders
		for (int i = 0; i < 50 && ChatManager.containsSeparator(e.getMessage()); i++) {
			Chat chat = Chat.getFrom(e.getMessage());
			if(chat == null)
				break;
			String placeholder = chat.getSlot().getPlaceholders().get(0);
			format = format.replace(ChatManager.addSeparator(chat.getId()), placeholder);
			e.setMessage(ChatManager.replaceSeparator(chat, e.getMessage(), placeholder));
		}
		List<ItemSlot> slots = ItemSlot.getItemSlotsFromMessage(e.getMessage());
		if (slots.isEmpty()) { // if not found
			ChatItem.debug("(v2) Can't found placeholder in: " + e.getMessage() + " > " + PlayerNamerManager.getPlayerNamer().getName(p));
			return;
		}
		// each kind of placeholder has it's own action, showed where "addSeparator(index)" is
		List<ChatAction> actions = new ArrayList<>();
		String defMsg = e.getMessage();
		for (ItemSlot slot : slots) {
			ChatAction action = ChatManager.getChatAction(slot, p);
			if(!ChatManager.canUsePlaceholder(p, action, slot, e)) {
				if (e.isCancelled())
					return;
				continue;
			}
			defMsg = slot.replacePlaceholders(defMsg, ChatManager.addSeparator(actions.size()));
			actions.add(action);
		}
		if (actions.isEmpty())
			return;
		e.setCancelled(true);
		format = format.replace(Character.toString(ChatManager.SEPARATOR), actions.get(0).getSlot().getPlaceholders().get(0));
		ChatItem.debug("(v2) Using format: " + format + " and message: " + e.getMessage());
		if (Utils.countMatches(defMsg, Character.toString(ChatManager.SEPARATOR)) > c.limit) {
			if (!c.messageLimit.isEmpty())
				p.sendMessage(c.messageLimit);
			return;
		}
		String msg;
		if (format.contains("%1$s") || format.contains("%2$s")) {
			msg = (format.contains("%2$s") ? String.format(format, p.getDisplayName(), defMsg) : String.format(format, p.getDisplayName()));
		} else {
			msg = format.replace(e.getMessage(), defMsg);
		}
		String loggedMessage = msg, discordMessage = defMsg;
		for (int i = 0; i < actions.size(); i++) {
			String name = ChatManager.getNameForChatAction(p, actions.get(i), c);
			loggedMessage = loggedMessage.replace(ChatManager.addSeparator(i), name);
			discordMessage = discordMessage.replace(ChatManager.addSeparator(i), name);
		}
		Bukkit.getConsoleSender().sendMessage(c.replaceInConsole ? loggedMessage : msg); // show in log
		if (ChatItem.discordSrvSupport)
			DiscordSrvSupport.sendChatMessage(p, discordMessage, e);
		Set<Player> recipients = e.getRecipients().isEmpty() ? new HashSet<>(Bukkit.getOnlinePlayers()) : e.getRecipients();
		ChatItem.debug("(v2) Msg: " + ChatItem.replace(p, msg).replace(ChatColor.COLOR_CHAR, '&') + ", format: " + format + " to " + recipients.size() + " players, " + actions.size() + " actions");
		recipients.forEach((pl) -> ChatItem.getPlatform().sendMessage(pl, p, actions, ChatItem.replace(p, msg)));
		if (c.cooldown > 0 && !p.hasPermission("chatitem.ignore-cooldown"))
			ChatManager.applyCooldown(p);
	}
}
