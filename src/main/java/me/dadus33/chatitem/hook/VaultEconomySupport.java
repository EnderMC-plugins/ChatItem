package me.dadus33.chatitem.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import net.milkbowl.vault.economy.Economy;

/**
 * Only use this class when Vault is enabled
 */
public class VaultEconomySupport {

	private static Economy economy = null;

	private static Economy getEconomy() {
		if (economy == null) {
			RegisteredServiceProvider<Economy> rs = Bukkit.getServicesManager().getRegistration(Economy.class);
			if (rs != null) // an economy plugin is setup
				economy = rs.getProvider();
		}
		return economy;
	}

	public static boolean isAvailable() {
		return getEconomy() != null;
	}

	public static String getBalance(Player p) {
		Economy eco = getEconomy();
		return eco == null ? "0" : eco.format(eco.getBalance(p));
	}
}
