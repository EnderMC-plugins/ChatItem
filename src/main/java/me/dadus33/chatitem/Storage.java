package me.dadus33.chatitem;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;

import me.dadus33.chatitem.utils.Colors;

public class Storage {

	public String handName, nameFormat, amountFormat, commandFormat, manager, permissionName, buggedClientAction, language;
	public final String messageNoPermission, messageDeny, messageReload, messageCooldown, messageLimit, messageIgnoredItem;
	public final String SECONDS, minutes, hours;
	public final String updateMessage, updateHover;
	public final List<String> tooltipHand, tooltipBuggedClient, ignoredItems;
	public boolean colorIfColored, addAmountForced, letMessageThrough, debug, handDisabled, showNoPermissionMessage, checkUpdate, permissionEnabled, cmdBroadcast, cmdShow, discordSrvSendMsg, replaceInConsole, chatFormatEnabled;
	public String chatFormat, chatFormatPrefix, chatFormatSuffix, chatFormatSeparator;
	public int configVersion, limit, cooldown;
	private FileConfiguration conf;

	public Storage(FileConfiguration cnf) {
		this.conf = cnf;
		configVersion = conf.getInt("config-version", 13);
		checkConfigVersion();
		this.manager = conf.getString("manager", "auto");
		if(manager.equalsIgnoreCase("both"))
			manager = "all";
		language = conf.getString("general.language", Translation.DEFAULT_LANGUAGE);
		debug = conf.getBoolean("debug", false);
		nameFormat = color(conf.getString("general.name-format", "&a&o{name}{amount}&r"));
		amountFormat = color(conf.getString("general.amount-format", "x{times}"));
		commandFormat = color(conf.getString("general.command-format", "&6%name%'s item is %item%"));
		colorIfColored = conf.getBoolean("general.color-if-already-colored", true);
		letMessageThrough = conf.getBoolean("general.let-message-through", true);
		addAmountForced = conf.getBoolean("general.force-add-amount", true);
		limit = conf.getInt("general.limit", 8);
		cooldown = conf.getInt("general.cooldown", 60);
		showNoPermissionMessage = conf.getBoolean("general.show-no-permission-message.normal", true);
		permissionEnabled = conf.getBoolean("general.permission.enabled", false);
		permissionName = conf.getString("general.permission.name", "chatitem.use");
		handDisabled = conf.getBoolean("general.hand.disabled", false);
		handName = color(conf.getString("general.hand.name", "&a&o{display-name}&a&o's hand"));
		tooltipHand = conf.getStringList("general.hand.tooltip").stream().map(Storage::color).collect(Collectors.toList());
		buggedClientAction = conf.getString("general.bugged_client.action", "show_both");
		tooltipBuggedClient = conf.getStringList("general.bugged_client.tooltip").stream().map(Storage::color).collect(Collectors.toList());
		ignoredItems = conf.getStringList("general.ignored_items");
		checkUpdate = conf.getBoolean("general.check-update", true);
		cmdShow = conf.getBoolean("general.sub-commands.broadcast", true);
		cmdBroadcast = conf.getBoolean("general.sub-commands.show", true);
		replaceInConsole = conf.getBoolean("general.replace-in-console", true);
		messageDeny = color(conf.getString("messages.deny-message", "&c&lYou have no item in hand!"));
		messageReload = color(conf.getString("messages.reload-success", "&b&lSuccessful reload!"));
		messageNoPermission = color(conf.getString("messages.no-permission", "&c&lI'm sorry, but you are not allowed to use the placeholder in chat!"));
		messageCooldown = color(conf.getString("messages.cooldown-message", "&c&lYou can only use items in chat once a minute! You have {remaining} left!"));
		updateMessage = color(conf.getString("messages.join-update.message", "&cA new version of ChatItem is available. &aClick here to download."));
		updateHover = color(conf.getString("messages.join-update.hover", "&6Click to go to spigot page !"));
		messageLimit = color(conf.getString("messages.limit-message", "&c&lYou can only add 8 item placeholders per message!"));
		messageIgnoredItem = color(conf.getString("messages.ignored-item", "&cThis item is not allowed to be showed."));
		discordSrvSendMsg = conf.getBoolean("discord-srv-send-our", true);
		chatFormatEnabled = conf.getBoolean("chat-format.enabled", false);
		chatFormat = conf.getString("chat-format.format", "{prefix}{name}");
		chatFormatPrefix = conf.getString("chat-format.prefix", "%luckperms_prefix%");
		chatFormatSuffix = conf.getString("chat-format.suffix", "");
		chatFormatSeparator = conf.getString("chat-format.separator", "&7: &f");

		SECONDS = color(conf.getString("messages.seconds", " seconds"));
		minutes = color(conf.getString("messages.minutes", " minutes"));
		hours = color(conf.getString("messages.hours", " hours"));
		colorStringList(tooltipHand);
	}

	public static String color(String s) {
		return s == null || s.isEmpty() ? s : Colors.translateAlternateColorCodes('&', s);
	}

	private static void colorStringList(List<String> input) {
		for (int i = 0; i < input.size(); ++i) {
			input.set(i, color(input.get(i)));
		}
	}

	private void checkConfigVersion() {
		int latestVersion = ChatItem.CFG_VER;
		if (latestVersion == configVersion)
			return;
		ChatItem pl = ChatItem.getInstance();
		File file = new File(pl.getDataFolder(), "config.yml");
		File backup = new File(pl.getDataFolder(), "config-old-v" + configVersion + ".yml");
		try {
			Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			pl.getLogger().severe("Failed to backup the config, it will not be updated: " + e.getMessage());
			return;
		}
		if (configVersion < latestVersion) {
			// keep all settings, and add new options
			pl.getLogger().warning("Updating the configuration from version " + configVersion + " to " + latestVersion + "...");
			int added = addMissingOptions(pl.getConfig());
			pl.getConfig().set("config-version", latestVersion);
			pl.saveConfig();
			pl.getLogger().warning("Configuration updated: " + added + " new options added, your settings are kept. Old file: " + backup.getName());
		} else {
			pl.getLogger().warning("ChatItem detected an invalid configuration file. Replacing it with the default config... Old file: " + backup.getName());
			performOverwrite();
		}
		pl.reloadConfig();
		conf = pl.getConfig();
	}

	/**
	 * Add options of the default config that are missing in the config
	 *
	 * @param config the config to update
	 * @return the amount of added options
	 */
	private int addMissingOptions(FileConfiguration config) {
		Configuration defaults = config.getDefaults();
		if (defaults == null)
			return 0;
		List<String> newKeys = new ArrayList<>();
		for (String key : defaults.getKeys(true)) {
			if (!config.isSet(key))
				newKeys.add(key);
		}
		int added = 0;
		for (String key : newKeys) {
			if (defaults.isConfigurationSection(key))
				continue;
			config.set(key, defaults.get(key));
			added++;
		}
		for (String key : newKeys) { // when all keys exist, put comments of new keys and sections
			try {
				config.setComments(key, defaults.getComments(key));
				config.setInlineComments(key, defaults.getInlineComments(key));
			} catch (NoSuchMethodError e) {
				break; // comments are only supported since MC 1.18.1
			}
		}
		return added;
	}

	private void performOverwrite() {
		ChatItem.getInstance().saveResource("config.yml", true);
	}

}
