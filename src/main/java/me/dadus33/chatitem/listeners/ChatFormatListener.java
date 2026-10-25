package me.dadus33.chatitem.listeners;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.Storage;
import me.dadus33.chatitem.utils.Version;

/**
 * Change how the player is shown in chat (instead of the default "&lt;Name&gt;"),
 * for all messages
 */
public class ChatFormatListener implements Listener {

	private static final Pattern HEX_COLOR = Pattern.compile("&#([0-9a-fA-F]{6})");

	// before the "chat" manager, which uses the format of the event
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent e) {
		Storage c = ChatItem.getInstance().getStorage();
		if (!c.chatFormatEnabled)
			return;
		Player p = e.getPlayer();
		String player = c.chatFormat.replace("{prefix}", c.chatFormatPrefix).replace("{suffix}", c.chatFormatSuffix).replace("{display-name}", p.getDisplayName())
				.replace("{name}", p.getName());
		// placeholders (PlaceholderAPI...) can give colors, so colors are translated after
		String format = Storage.color(translateHexColors(ChatItem.replace(p, player) + c.chatFormatSeparator));
		e.setFormat(format.replace("%", "%%") + "%2$s"); // the message is the second argument of the format
	}

	/**
	 * Translate "&#rrggbb" colors, used by LuckPerms prefixes for example (only
	 * since MC 1.16)
	 *
	 * @param s the text to translate
	 * @return the translated text
	 */
	private static String translateHexColors(String s) {
		if (!Version.getVersion().isNewerOrEquals(Version.V1_16))
			return s;
		Matcher matcher = HEX_COLOR.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			StringBuilder color = new StringBuilder("§x");
			for (char ch : matcher.group(1).toCharArray())
				color.append('§').append(ch);
			matcher.appendReplacement(sb, Matcher.quoteReplacement(color.toString()));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
