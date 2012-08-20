package org.royaldev.royalauth;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.royaldev.royalauth.commands.CmdChangePass;
import org.royaldev.royalauth.commands.CmdLogOut;
import org.royaldev.royalauth.commands.CmdLogin;
import org.royaldev.royalauth.commands.CmdRegister;
import org.royaldev.royalauth.commands.CmdRoyalAuth;
import org.royaldev.royalauth.listeners.RAuthListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RoyalAuth extends JavaPlugin {

    public static RoyalAuth instance;

    public ConnectionManager cm;
    public Logger log;

    public RAuthListener rListener;

    public static Permission permission = null;

    //--- Private methods ---//

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) permission = permissionProvider.getProvider();
        return permission != null;
    }

    //--- Public methods ---//

    /**
     * Checks to see if a command sender has a permission. If Vault is not installed, uses SuperPerms.
     * <p/>
     * If sender is RCon or on console, returns true.
     *
     * @param cs   CommandSender to check for
     * @param node Node to check
     * @return true if authorized, false if not
     */
    public boolean isAuthorized(CommandSender cs, String node) {
        if (cs instanceof RemoteConsoleCommandSender || cs instanceof ConsoleCommandSender)
            return true;
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            return permission.has(cs, node);
        } else {
            return cs.hasPermission(node);
        }
    }

    //--- Config ---//

    public String dbAddress;
    public String dbUser;
    public String dbPass;
    public String dbDatabase;
    public String dbPrefix;
    public String validRegex;

    public boolean teleToSpawn;
    public boolean allowChat;
    public boolean disableIfOnline;
    public boolean sessionCheckIP;
    public boolean sessionEnabled;
    public boolean requireRegister;

    public List<String> allowedCommands = new ArrayList<String>();

    public int dbPort;
    public int kickTime;
    public int sessionLength;

    public void reloadConfigVals() {
        FileConfiguration config = getConfig();
        dbAddress = config.getString("mysql.address", "localhost");
        dbUser = config.getString("mysql.username", "root");
        dbPass = config.getString("mysql.password", "");
        dbDatabase = config.getString("mysql.database", "minecraft");
        dbPrefix = config.getString("mysql.table_prefix", "ra_");
        validRegex = config.getString("player_regex", "(?i)[\\w\\-]{2,16}");

        teleToSpawn = config.getBoolean("teleport_to_spawn", true);
        allowChat = config.getBoolean("allow_chat_notloggedin", false);
        disableIfOnline = config.getBoolean("disable_if_onlinemode", true);
        sessionCheckIP = config.getBoolean("sessions.check_ip", true);
        sessionEnabled = config.getBoolean("session.enabled", true);
        requireRegister = config.getBoolean("require_register", true);

        allowedCommands = config.getStringList("allowed_commands");

        dbPort = config.getInt("mysql.port", 3306);
        kickTime = config.getInt("kick_not_logged_in", 300);
        sessionLength = config.getInt("sessions.length", 300);
    }

    public void onEnable() {

        //-- Globals --//

        instance = this;
        log = getLogger();

        //-- Hidendra's MetricsLite --//

        try {
            new MetricsLite(this).start();
        } catch (IOException e) {
            getLogger().warning("Could not start Metrics!");
        }

        //-- Permissions --//

        if (getServer().getPluginManager().getPlugin("Vault") != null)
            if (!setupPermissions())
                log.warning("Could not set up permissions! Defaulting to SuperPerms.");

        //-- Config --//

        if (!new File(getDataFolder() + File.separator + "config.yml").exists())
            saveDefaultConfig();
        reloadConfig();
        reloadConfigVals();

        //-- Checks --//

        if (getServer().getOnlineMode() && disableIfOnline) {
            log.info("Disabling because online mode is enabled.");
            log.info("This can be changed in the config.");
            setEnabled(false);
            return;
        }

        //-- MySQL --//

        log.info("Connecting to MySQL...");
        cm = new ConnectionManager(dbUser, dbPass, dbDatabase, dbPrefix, dbAddress, dbPort);
        if (!cm.isConnected()) {
            log.severe("Could not connect to database! Disabling!");
            if (getServer().getPluginManager().getPlugin("RoyalCommands") != null)
                log.info("You can do /pm enable RoyalAuth after editing the config to try again.");
            setEnabled(false);
            return;
        } else log.info("Connected!");

        //-- Listeners --//

        PluginManager pm = getServer().getPluginManager();
        rListener = new RAuthListener(this); // Add here because listener calls upon cm
        pm.registerEvents(rListener, this);

        //-- Commands --//

        getCommand("login").setExecutor(new CmdLogin(this));
        getCommand("register").setExecutor(new CmdRegister(this));
        getCommand("changepass").setExecutor(new CmdChangePass(this));
        getCommand("logout").setExecutor(new CmdLogOut(this));
        getCommand("royalauth").setExecutor(new CmdRoyalAuth(this));
    }

    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        for (Player p : getServer().getOnlinePlayers()) {
            if (!cm.isLoggedIn(p.getName())) continue;
            p.sendMessage(ChatColor.RED + "You have been logged out because of a reload.");
            cm.logOut(p.getName());
        }
    }

}
