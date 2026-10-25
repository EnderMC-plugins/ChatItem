package me.dadus33.chatitem;

import me.dadus33.chatitem.chatmanager.ChatAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class C {

	public static TextComponent text(String s) {
		return LegacyComponentSerializer.legacySection().deserialize(s);
	}

	public static String string(Component c) {
		return LegacyComponentSerializer.legacySection().serialize(c);
	}

	/**
	 * Create the click event of the action: run the command, or copy the text
	 *
	 * @param action the action of the message
	 * @return the click event or null if nothing should happen on click
	 */
	public static ClickEvent click(ChatAction action) {
		if (action.hasCommand())
			return ClickEvent.runCommand(action.getCommand());
		if (action.hasCopyText())
			return ClickEvent.copyToClipboard(action.getCopyText());
		return null;
	}
}
