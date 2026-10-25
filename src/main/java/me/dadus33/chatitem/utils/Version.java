package me.dadus33.chatitem.utils;

import java.net.InetSocketAddress;

import me.dadus33.chatitem.ChatItem;

public enum Version {
	
	// http://wiki.vg/Protocol_version_numbers
	UNSUPPORTED(0, 5, 7),
	V1_8(6, 47, 8),
	V1_9(49, 110, 9), // 1.9.X - Starts with 49 as 48 was an april fools update
	V1_10(201, 210, 10), // 1.10.X - Starts with 201 because why not.
	V1_11(301, 316, 11),
	V1_12(317, 340, 12),
	V1_13(341, 440, 13),
	V1_14(441, 500, 14),
	V1_15(550, 578, 15),
	V1_16(700, 754, 16),
	V1_17(755, 756, 17),
	V1_18(757, 758, 18),
	V1_19(759, 762, 19),
	V1_20(763, 765, 20),
	V1_20_6(766, 766, 20.6),
	V1_21(767, 769, 21), // 1.21 - 1.21.4
	V1_21_5(770, 773, 21.05), // 1.21.5 - 1.21.10 - Text components use snake_case keys (hover_event, click_event)
	V1_21_11(774, 774, 21.11),
	V26(775, 1000, 26), // 26.1 and newer - Year-based versions, no more obfuscation
	HIGHER(Integer.MAX_VALUE, -1, Integer.MAX_VALUE);

	// Latest version should always have the upper limit set to Integer.MAX_VALUE so
	// I don't have to update the plugin for every minor protocol change

	public final int MIN_VER;
	public final int MAX_VER;
	public final double index; // Represents how new the version is (0 - extremely old)

	private static final Version SERVER_VERSION = ChatItem.getPlatform().getMinecraftVersion();

	Version(int min, int max, double index) {
		this.MIN_VER = min;
		this.MAX_VER = max;
		this.index = index;
	}
	
	public boolean isNewerOrEquals(Version other) {
		return index >= other.index;
	}

	public boolean isNewerThan(Version other) {
		return index > other.index;
	}

	public static Version getVersionByName(String name) {
		try {
			return Version.valueOf(name.toUpperCase());
		} catch (Exception e) {}
		for (Version v : Version.values())
			if (name.toLowerCase().startsWith(v.name().toLowerCase()))
				return v;
		return HIGHER;
	}

	/**
	 * Get the version from a Minecraft version name, like "1.8.8", "1.21.11" or
	 * "26.3". Suffix like "-R0.1-SNAPSHOT" (from Bukkit#getBukkitVersion) are
	 * ignored.
	 *
	 * @param name the Minecraft version
	 * @return the version
	 */
	public static Version getVersionByMinecraftVersion(String name) {
		String mcVersion = name.split("-")[0];
		String[] parts = mcVersion.split("\\.");
		try {
			int major = Integer.parseInt(parts[0]);
			int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
			if (major >= 26)
				return V26;
			if (major == 1 && minor >= 8) {
				if (minor == 20 && patch >= 5)
					return V1_20_6;
				if (minor == 21)
					return patch >= 11 ? V1_21_11 : (patch >= 5 ? V1_21_5 : V1_21);
			}
		} catch (NumberFormatException e) {}
		return getVersionByName("v" + mcVersion.replace(".", "_"));
	}

	public static Version getVersion(int protocolVersion) {
		return getVersion(protocolVersion, HIGHER);
	}

	public static Version getVersion(int protocolVersion, Version def) {
		for (Version ver : Version.values()) {
			if (protocolVersion >= ver.MIN_VER && protocolVersion <= ver.MAX_VER) {
				return ver;
			}
		}
		ChatItem.debug("Failed to find version for protocol " + protocolVersion);
		return def;
	}

	public static Version getVersion() {
		return SERVER_VERSION;
	}

	public static String stringifyAdress(InetSocketAddress address) {
		if(address == null)
			return "0.0.0.0:0";
		String port = String.valueOf(address.getPort());
		String ip = address.getAddress().getHostAddress();
		return ip + ":" + port;
	}
}
