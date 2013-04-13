package org.royaldev.royalauth;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;

public class Config {

    private final RoyalAuth plugin;

    public Config(RoyalAuth instance) {
        plugin = instance;
        File config = new File(plugin.getDataFolder(), "config.yml");
        if (!config.exists()) {
            if (!config.getParentFile().mkdirs()) plugin.getLogger().warning("Could not create config.yml directory.");
            plugin.saveDefaultConfig();
        }
        reloadConfiguration();
    }

    public void reloadConfiguration() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        disableIfOnlineMode = c.getBoolean("login.disable_if_online_mode");
        requireLogin = c.getBoolean("login.require");
        kickIfAlreadyOnline = c.getBoolean("login.kick_if_already_online");

        allowChat = c.getBoolean("login.restrictions.chat.allowed");
        chatPrefix = c.getString("login.restrictions.chat.prefix");

        allowCommands = c.getBoolean("login.restrictions.commands.allowed");
        allowedCommands = c.getStringList("login.restrictions.commands.exempt");

        allowMovementWalk = c.getBoolean("login.restrictions.movement.walk");
        allowMovementLook = c.getBoolean("login.restrictions.movement.look_around");

        godMode = c.getBoolean("login.godmode.enabled");
        godModeAfterLogin = c.getBoolean("login.godmode.after_login.enabled");
        godModeLength = c.getLong("login.godmode.after_login.length");

        remindEnabled = c.getBoolean("login.remind.enabled");
        remindInterval = c.getLong("login.remind.interval");

        saveUserdataInterval = c.getLong("saving.interval");

        sessionsEnabled = c.getBoolean("sessions.enabled");
        sessionsCheckIP = c.getBoolean("sessions.check_ip");
        sessionLength = c.getLong("sessions.length");

        disallowedPasswords = c.getStringList("passwords.disallowed");
        passwordHashType = c.getString("passwords.hash_type");

        validateUsernames = c.getBoolean("usernames.verify");
        usernameRegex = c.getString("usernames.regex");

        adventureMode = c.getBoolean("login.adventure_mode");
        teleportToSpawn = c.getBoolean("login.teleport_to_spawn");

        kickPlayers = c.getBoolean("login.remind.kick.enabled");
        kickAfter = c.getLong("login.remind.kick.wait");

        //-- Check for invalid inputs and set to default if invalid --//

        if (remindInterval < 1L) remindInterval = 30L;
        if (saveUserdataInterval < 1L) saveUserdataInterval = 5L;
        if (sessionLength < 1L) sessionLength = 15L;
        if (kickAfter < 0L) kickAfter = 0L;
        if (godModeLength <= 0L) godModeLength = 10L;
    }

    public static boolean disableIfOnlineMode;
    public static boolean requireLogin;
    public static boolean kickIfAlreadyOnline;

    public static boolean allowChat;
    public static String chatPrefix;

    public static boolean allowCommands;
    public static List<String> allowedCommands;

    public static boolean allowMovementWalk;
    public static boolean allowMovementLook;

    public static boolean godMode;
    public static boolean godModeAfterLogin;
    public static long godModeLength;

    public static boolean remindEnabled;
    public static long remindInterval;

    public static long saveUserdataInterval;

    public static boolean sessionsEnabled;
    public static boolean sessionsCheckIP;
    public static long sessionLength;

    public static List<String> disallowedPasswords;
    public static String passwordHashType;

    public static boolean validateUsernames;
    public static String usernameRegex;

    public static boolean adventureMode;
    public static boolean teleportToSpawn;

    public static boolean kickPlayers;
    public static long kickAfter;

}
