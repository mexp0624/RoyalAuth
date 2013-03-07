package org.royaldev.royalauth;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.royaldev.royalauth.commands.CmdChangePassword;
import org.royaldev.royalauth.commands.CmdLogin;
import org.royaldev.royalauth.commands.CmdLogout;
import org.royaldev.royalauth.commands.CmdRegister;
import org.royaldev.royalauth.commands.CmdRoyalAuth;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoyalAuth extends JavaPlugin {

    public Config c;
    public Logger log;

    public static File dataFolder;

    private final Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+)(\\-SNAPSHOT)?(\\-local\\-(\\d{8}\\.\\d{6})|\\-(\\d+))?");

    /**
     * Registers a command in the server. If the command isn't defined in plugin.yml
     * the NPE is caught, and a warning line is sent to the console.
     *
     * @param ce      CommandExecutor to be registered
     * @param command Command name as specified in plugin.yml
     * @param jp      Plugin to register under
     */
    private void registerCommand(CommandExecutor ce, String command, JavaPlugin jp) {
        try {
            jp.getCommand(command).setExecutor(ce);
        } catch (NullPointerException e) {
            getLogger().warning("Could not register command \"" + command + "\" - not registered in plugin.yml (" + e.getMessage() + ")");
        }
    }

    @Override
    public void onEnable() {
        dataFolder = getDataFolder();

        if (!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();

        c = new Config(this);
        log = getLogger();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new AuthListener(this), this);

        registerCommand(new CmdRoyalAuth(this), "royalauth", this);
        registerCommand(new CmdLogin(this), "login", this);
        registerCommand(new CmdLogout(this), "logout", this);
        registerCommand(new CmdRegister(this), "register", this);
        registerCommand(new CmdChangePassword(), "changepassword", this);

        //-- Hidendra's Metrics --//

        try {
            Matcher matcher = versionPattern.matcher(getDescription().getVersion());
            matcher.matches();
            // 1 = base version
            // 2 = -SNAPSHOT
            // 5 = build #
            String versionMinusBuild = (matcher.group(1) == null) ? "Unknown" : matcher.group(1);
            String build = (matcher.group(5) == null) ? "local build" : matcher.group(5);
            if (matcher.group(2) == null) build = "release";
            Metrics m = new Metrics(this);
            Metrics.Graph g = m.createGraph("Version"); // get our custom version graph
            g.addPlotter(
                    new Metrics.Plotter(versionMinusBuild + "~=~" + build) {
                        @Override
                        public int getValue() {
                            return 1; // this value doesn't matter
                        }
                    }
            ); // add the donut graph with major version inside and build outside
            m.addGraph(g); // add the graph
            if (!m.start())
                getLogger().info("You have Metrics off! I like to keep accurate usage statistics, but okay. :(");
            else getLogger().info("Metrics enabled. Thank you!");
        } catch (Exception ignore) {
            getLogger().warning("Could not start Metrics!");
        }

        log.info(getDescription().getName() + " v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        PConfManager.saveAllManagers();
    }

}
