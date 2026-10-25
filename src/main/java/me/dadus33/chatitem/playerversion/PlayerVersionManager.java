package me.dadus33.chatitem.playerversion;

import me.dadus33.chatitem.ChatItem;
import me.dadus33.chatitem.playerversion.hooks.DefaultVersionHook;
import me.dadus33.chatitem.playerversion.hooks.ProtocolSupportHook;
import me.dadus33.chatitem.playerversion.hooks.ViaVersionHook;
import org.bukkit.Bukkit;

public class PlayerVersionManager {

    private static final IPlayerVersion playerVersionAdapter;

    static {
        ChatItem pl = ChatItem.getInstance();
        IPlayerVersion adapter = null;
        if (Bukkit.getPluginManager().getPlugin("ProtocolSupport") != null) {
            try {
                adapter = new ProtocolSupportHook();
                pl.getLogger().info("Loading ProtocolSupport support ...");
            } catch (ReflectiveOperationException e) {
                pl.getLogger().warning("Failed to load ProtocolSupport support: " + e.getMessage());
            }
        }
        if (adapter == null && Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
            adapter = new ViaVersionHook();
            pl.getLogger().info("Loading ViaVersion support ...");
        }
        playerVersionAdapter = adapter == null ? new DefaultVersionHook() : adapter;
    }

    public static IPlayerVersion getPlayerVersionAdapter() {
        return playerVersionAdapter;
    }
}
