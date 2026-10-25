package me.dadus33.chatitem;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.dadus33.chatitem.utils.Utils;

@SuppressWarnings("deprecation")
public class Translation {

	public static final String DEFAULT_LANGUAGE = "de_de";
	private static File folder;
	private static YamlConfiguration legacy, allLangsConfig;
	private static JsonObject messages;
	private static HashMap<String, String> allLangs = new HashMap<>();

	public static void load(ChatItem pl) {
		folder = new File(pl.getDataFolder(), "lang");
		if (!folder.exists())
			folder.mkdir();
		legacy = Utils.copyLoadFile(folder, "legacy.yml", "lang/legacy.yml");
		allLangsConfig = Utils.copyLoadFile(pl.getDataFolder(), "langs.yml", "langs.yml");

		allLangsConfig.getStringList("all").forEach(langKey -> {
			String resource = "lang/" + langKey + ".json";
			if (ChatItem.class.getClassLoader().getResource(resource) == null) { // old langs.yml can contains languages that are no longer included
				ChatItem.debug("Lang " + langKey + " isn't included in the plugin");
				return;
			}
			try {
				JsonObject content;
				try (Reader reader = new InputStreamReader(pl.getResource(resource), StandardCharsets.UTF_8)) {
					content = JsonParser.parseReader(reader).getAsJsonObject();
				}
				File langFile = new File(folder, langKey + ".json");
				if (!langFile.exists()) // put all languages in the folder, so they can be edited
					Utils.copyFile(resource, langFile);
				else // the file can come from an older version
					addMissingTranslations(langFile, content);
				allLangs.put(langKey, content.get("language.name").getAsString() + " (" + content.get("language.region").getAsString() + ")");
			} catch (Exception e) {
				pl.getLogger().severe("Failed to load lang with key " + langKey + " : " + e.getMessage() + " (" + e.getStackTrace()[0].toString() + ")");
			}
		});
		allLangs = allLangs.entrySet().stream().sorted(Entry.comparingByValue()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		loadLang(pl.getStorage().language);
	}

	/**
	 * Add translations of the plugin that are missing in the language file (for
	 * new items), without changing others
	 *
	 * @param langFile       the language file of the server
	 * @param defaultContent the language file included in the plugin
	 */
	private static void addMissingTranslations(File langFile, JsonObject defaultContent) {
		try {
			JsonObject actual;
			try (Reader reader = Files.newBufferedReader(langFile.toPath(), StandardCharsets.UTF_8)) {
				actual = JsonParser.parseReader(reader).getAsJsonObject();
			}
			int added = 0;
			for (Entry<String, JsonElement> entry : defaultContent.entrySet()) {
				if (!actual.has(entry.getKey())) {
					actual.add(entry.getKey(), entry.getValue());
					added++;
				}
			}
			if (added == 0)
				return;
			try (Writer writer = Files.newBufferedWriter(langFile.toPath(), StandardCharsets.UTF_8)) {
				new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(actual, writer);
				writer.write("\n");
			}
			ChatItem.getInstance().getLogger().info("Added " + added + " new translations in lang/" + langFile.getName());
		} catch (Exception e) {
			ChatItem.getInstance().getLogger().warning("Failed to update lang/" + langFile.getName() + ": " + e.getMessage());
		}
	}

	public static HashMap<String, String> getAllLangs() {
		return allLangs;
	}
	
	public static JsonObject getMessages() {
		return messages;
	}
	
	public static String getMessage(String key) {
		JsonElement result = messages.get(key);
		return result == null ? "" : result.getAsString();
	}
	
	public static void loadLang(String lang) {
		ChatItem pl = ChatItem.getInstance();
		File langFile = new File(folder, lang + ".json");
		if (!langFile.exists()) {
			if (pl.getResource("lang/" + lang + ".json") != null) {
				Utils.copyFile("lang/" + lang + ".json", langFile);
			} else {
				pl.getLogger().severe("Failed to find lang file for " + lang + ", using " + DEFAULT_LANGUAGE);
				if(!lang.equals(DEFAULT_LANGUAGE))
					loadLang(DEFAULT_LANGUAGE);
				return;
			}
		}

		try (Reader reader = Files.newBufferedReader(langFile.toPath(), StandardCharsets.UTF_8)) {
			messages = JsonParser.parseReader(reader).getAsJsonObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		pl.getLogger().info("Loaded translation for " + lang + ".");
	}

	public static String get(ItemStack item) {
		String key = (item.getType().isBlock() ? "block" : "item") + ".minecraft." + item.getType().name().toLowerCase();
		if (messages != null && messages.has(key)) {
			return messages.get(key).getAsString();
		} else
			ChatItem.debug("Failed to find translation for " + key);
		return legacy.getString(item.getType().name() + "." + item.getDurability(), item.getType().name().toLowerCase().replace("_", " "));
	}
	
	public static String getOr(ItemStack item, String def) {
		String key = (item.getType().isBlock() ? "block" : "item") + ".minecraft." + item.getType().name().toLowerCase();
		if (messages != null && messages.has(key)) {
			return messages.get(key).getAsString();
		} else
			ChatItem.debug("Failed to find translation for " + key);
		return legacy.getString(item.getType().name() + "." + item.getDurability(), def);
	}

	protected void thisMethodShouldNotBeUsed_InsteadToUpdateLangFileFromLocalInstall() throws Exception {
		File dir = new File("dir/to/result");
		File folder = new File("C:\\Dir\\To\\AppData\\Roaming\\.minecraft\\assets");// here we are looking for 1.19
		JsonObject json = JsonParser.parseReader(new FileReader(new File(folder, "indexes/1.19.json"))).getAsJsonObject().get("objects").getAsJsonObject();
		for (String key : json.keySet()) {
			if (key.startsWith("minecraft/lang")) { // only lang files
				String lang = key.toString().replace("minecraft/lang/", "");
				String hash = json.get(key).getAsJsonObject().get("hash").getAsString();
				File langFile = new File(folder, "objects/" + hash.substring(0, 2) + "/" + hash);
				JsonObject langs = JsonParser.parseReader(new FileReader(langFile)).getAsJsonObject();
				for (String langKey : new ArrayList<>(langs.keySet())) {
					if (!(langKey.startsWith("block.") || langKey.startsWith("language"))) { // only what we want
						langs.remove(langKey); // removing not needed
					}
				}
				File langDir = new File(dir, lang);
				Files.write(langDir.toPath(), Arrays.asList(langs.toString()));
				System.out.println("Copied " + lang); // using sysout as it's made for basic java, not mc
			}
		}
	}
}
