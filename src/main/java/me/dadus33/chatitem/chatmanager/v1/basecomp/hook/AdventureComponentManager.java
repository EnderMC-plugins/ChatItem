package me.dadus33.chatitem.chatmanager.v1.basecomp.hook;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import me.dadus33.chatitem.C;
import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.Storage;
import me.dadus33.chatitem.chatmanager.Chat;
import me.dadus33.chatitem.chatmanager.ChatAction;
import me.dadus33.chatitem.chatmanager.ChatManager;
import me.dadus33.chatitem.chatmanager.v1.basecomp.IComponentManager;
import me.dadus33.chatitem.chatmanager.v1.json.JSONManipulator;
import me.dadus33.chatitem.chatmanager.v1.packets.ChatItemPacket;
import me.dadus33.chatitem.hook.DiscordSrvSupport;
import me.dadus33.chatitem.utils.ItemUtils;
import me.dadus33.chatitem.utils.PacketUtils;
import me.dadus33.chatitem.utils.ReflectionUtils;
import me.dadus33.chatitem.utils.Utils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class AdventureComponentManager implements IComponentManager {
	
	@Override
	public boolean hasConditions() {
		try {
			for (String cl : Arrays.asList("net.kyori.adventure.text.Component", "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"))
				Class.forName(cl);
		} catch (Throwable e) { // can't support this, adventure comp not found
			ChatItem.debug("Can't load AdventureComponentManager : " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void writeJson(ChatItemPacket packet, String json) {
	}

	public void writeComponentToPacket(ChatItemPacket packet, Component next) {
		if(packet.getContent().getSpecificModifier(Component.class).readSafely(0) == null && ReflectionUtils.hasObject(packet.getPacket(), "unsignedContent")) { // hard way
			ReflectionUtils.setField(getUnsignedContent(packet), "adventure", next);
		} else { // easy way
			packet.getContent().getSpecificModifier(Component.class).write(0, next);
		}
	}

	public Component getComponentFromPacket(ChatItemPacket packet) {
		Component comp = packet.getContent().getSpecificModifier(Component.class).readSafely(0);
		if (comp == null && packet.getPacketName().equalsIgnoreCase("ClientboundPlayerChatPacket") && ReflectionUtils.hasObject(packet.getPacket(), "unsignedContent")) { // if can get one more
			Object unsignedContent = getUnsignedContent(packet);
			if (unsignedContent != null && ReflectionUtils.hasObject(unsignedContent, "adventure"))
				comp = (Component) ReflectionUtils.getObject(unsignedContent, "adventure");
		}
		return comp;
	}

	private Object getUnsignedContent(ChatItemPacket packet) {
		Object unsignedContent = ReflectionUtils.getObject(packet.getPacket(), "unsignedContent");
		// it's an Optional since MC 26.1
		return unsignedContent instanceof Optional ? ((Optional<?>) unsignedContent).orElse(null) : unsignedContent;
	}
	
	@Override
	public String getBaseComponentAsJSON(ChatItemPacket packet) {
		Component comp = getComponentFromPacket(packet);
		if(comp == null) // if comp stay null
			return null;
		try {
			String json = GsonComponentSerializer.gson().serialize(comp);
			ChatItem.debug("AdventureJSON : " + json);
			JsonObject jsonObj = JSONManipulator.parseOrGet(json);
			if (jsonObj.has("with")) {
				JsonObject next = new JsonObject();
				next.add("extra", jsonObj.get("with"));
				return next.toString();
			}
			return json;
		} catch (JsonParseException e) { // ignore this and just let skip this
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Object manageContent(Player viewer, Chat chat, ChatItemPacket packet, String json, Storage c) throws Exception {
		return manage(viewer, chat, packet, c);
	}

	@Override
	public Object manageEmpty(Player viewer, Chat chat, ChatItemPacket packet, String json, Storage c) {
		return manage(viewer, chat, packet, c);
	}

	/**
	 * Create the component which replace the placeholder of the chat
	 *
	 * @param viewer the player that will see the message
	 * @param chat   the chat of the placeholder
	 * @param c      the config
	 * @return the component
	 */
	@SuppressWarnings("deprecation")
	private Component createReplacement(Player viewer, Chat chat, Storage c) {
		ChatAction action = chat.getAction();
		if (!action.hasItem())
			return Component.text(action.getChatText()).hoverEvent(HoverEvent.showText(Component.text(action.getHoverText()))).clickEvent(C.click(action));
		ItemStack item = action.getItem();
		if (ItemUtils.isEmpty(item)) {
			Component tooltip = Component.text("");
			for (String s : c.tooltipHand)
				tooltip = tooltip.append(Component.text(s));
			return Component.text(ChatManager.getNameForChatAction(viewer, chat, c)).hoverEvent(HoverEvent.showText(tooltip));
		}
		HoverEvent<?> hover;
		if(Utils.IS_PAPER)
			hover = item.asHoverEvent();
		else
			hover = HoverEvent.showItem(Key.key(item.getType().getKey().getKey()), item.getAmount(), BinaryTagHolder.binaryTagHolder(PacketUtils.getNbtTag(item)));
		return Component.text(ChatManager.getNameForChatAction(viewer, chat, c)).hoverEvent(hover);
	}

	private Object manage(Player viewer, Chat chat, ChatItemPacket packet, Storage c) {
		Component comp = getComponentFromPacket(packet);
		if (comp == null) {
			ChatItem.debug("The component is null.");
			return null;
		}
		// the message can contain many placeholders, each one with it's own chat
		List<Chat> chats = Chat.getAllFrom(C.string(comp));
		if (!chats.contains(chat))
			chats.add(0, chat);
		for (Chat other : chats)
			comp = comp.replaceText(TextReplacementConfig.builder().matchLiteral(ChatManager.addSeparator(other.getId())).replacement(createReplacement(viewer, other, c)).build());
		if(ChatItem.discordSrvSupport && DiscordSrvSupport.isSendingMessage() && viewer == chat.getPlayer())
			DiscordSrvSupport.sendChatMessage(chat.getPlayer(), comp, null);
		viewer.sendMessage(comp);
		return null;
	}
}
