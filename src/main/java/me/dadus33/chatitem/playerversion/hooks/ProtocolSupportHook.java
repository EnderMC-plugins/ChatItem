package me.dadus33.chatitem.playerversion.hooks;

import java.lang.reflect.Method;

import org.bukkit.entity.Player;

import me.dadus33.chatitem.playerversion.IPlayerVersion;
import me.dadus33.chatitem.utils.Version;

public class ProtocolSupportHook implements IPlayerVersion {

	// called with reflection as the ProtocolSupport API isn't published anymore
	private final Method getProtocolVersion, getId;

	public ProtocolSupportHook() throws ReflectiveOperationException {
		getProtocolVersion = Class.forName("protocolsupport.api.ProtocolSupportAPI").getMethod("getProtocolVersion", Player.class);
		getId = getProtocolVersion.getReturnType().getMethod("getId");
	}

	@Override
	public int getProtocolVersion(Player p) {
		try {
			return (int) getId.invoke(getProtocolVersion.invoke(null, p));
		} catch (Exception e) {
			e.printStackTrace();
			return Version.getVersion().MAX_VER;
		}
	}

}
