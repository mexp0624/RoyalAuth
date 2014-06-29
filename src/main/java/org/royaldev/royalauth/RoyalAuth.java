package org.royaldev.royalauth;

import com.google.common.io.PatternFilenameFilter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.royaldev.royalauth.commands.CmdChangePassword;
import org.royaldev.royalauth.commands.CmdLogin;
import org.royaldev.royalauth.commands.CmdLogout;
import org.royaldev.royalauth.commands.CmdRegister;
import org.royaldev.royalauth.commands.CmdRoyalAuth;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoyalAuth extends JavaPlugin {

    public static File dataFolder;
    private final Pattern versionPattern = Pattern.compile("((\\d+\\.?){3})(\\-SNAPSHOT)?(\\-local\\-(\\d{8}\\.\\d{6})|\\-(\\d+))?");
    public Config c;
    public Logger log;

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
            jp.getLogger().warning(String.format(Language.COULD_NOT_REGISTER_COMMAND.toString(), command, e.getMessage()));
        }
    }

    private void update() {
        final File userdataFolder = new File(dataFolder, "userdata");
        if (!userdataFolder.exists() || !userdataFolder.isDirectory()) return;
        for (String fileName : userdataFolder.list(new PatternFilenameFilter("(?i)^.+\\.yml$"))) {
            String playerName = fileName.substring(0, fileName.length() - 4); // ".yml" = 4
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(playerName);
                continue;
            } catch (IllegalArgumentException ignored) {}
            UUID u;
            try {
                u = RUtils.getUUID(playerName);
            } catch (Exception ex) {
                u = this.getServer().getOfflinePlayer(playerName).getUniqueId();
            }
            if (u == null) {
                this.getLogger().warning(Language.ERROR.toString());
                continue;
            }
            if (new File(fileName).renameTo(new File(u + ".yml"))) {
                this.getLogger().info(String.format(Language.CONVERTED_USERDATA.toString(), fileName, u));
            } else {
                this.getLogger().warning(String.format(Language.COULD_NOT_CONVERT_USERDATA.toString(), fileName));
            }
        }
    }

    private void saveLangFile(String name) {
        if (!new File(this.getDataFolder() + File.separator + "lang" + File.separator + name + ".properties").exists())
            this.saveResource("lang" + File.separator + name + ".properties", false);
    }

    @Override
    public void onDisable() {
        this.getServer().getScheduler().cancelTasks(this);

        for (Player p : this.getServer().getOnlinePlayers()) {
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn()) ap.logout(this, false);
        }

        PConfManager.saveAllManagers();
        PConfManager.purge();
    }

    @Override
    public void onEnable() {
        RoyalAuth.dataFolder = this.getDataFolder();

        if (!new File(getDataFolder(), "config.yml").exists()) this.saveDefaultConfig();

        this.c = new Config(this);
        this.log = this.getLogger();

        this.saveLangFile("en_us");

        try {
            new Language.LanguageHelper(new File(this.getDataFolder(), this.getConfig().getString("general.language_file", "lang/en_us.properties")));
        } catch (IOException e) {
            this.log.severe("Could not load language file: " + e.getMessage());
            this.log.severe("Disabling plugin.");
            this.setEnabled(false);
            return;
        }

        if (Config.checkOldUserdata) this.update();

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new AuthListener(this), this);

        this.registerCommand(new CmdRoyalAuth(this), "royalauth", this);
        this.registerCommand(new CmdLogin(this), "login", this);
        this.registerCommand(new CmdLogout(this), "logout", this);
        this.registerCommand(new CmdRegister(this), "register", this);
        this.registerCommand(new CmdChangePassword(), "changepassword", this);

        //-- Hidendra's Metrics --//

        try {
            Matcher matcher = versionPattern.matcher(getDescription().getVersion());
            if (matcher.matches()) {
                // 1 = base version
                // 3 = -SNAPSHOT
                // 6 = build #
                String versionMinusBuild = (matcher.group(1) == null) ? "Unknown" : matcher.group(1);
                String build = (matcher.group(6) == null) ? "local build" : matcher.group(6);
                if (matcher.group(3) == null) build = "release";
                Metrics m = new Metrics(this);
                Metrics.Graph g = m.createGraph("Version"); // get our custom version graph
                g.addPlotter(new Metrics.Plotter(versionMinusBuild + "~=~" + build) {
                    @Override
                    public int getValue() {
                        return 1; // this value doesn't matter
                    }
                }); // add the donut graph with major version inside and build outside
                m.addGraph(g); // add the graph
                if (!m.start()) this.getLogger().info(Language.METRICS_OFF.toString());
                else this.getLogger().info(Language.METRICS_ENABLED.toString());
            }
        } catch (Exception ignore) {
            this.getLogger().warning(Language.COULD_NOT_START_METRICS.toString());
        }

        for (Player p : this.getServer().getOnlinePlayers()) {
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn()) continue;
            if (ap.isRegistered()) ap.createLoginReminder(this);
            else ap.createRegisterReminder(this);
        }

        this.log.info(this.getDescription().getName() + " v" + this.getDescription().getVersion() + " " + Language.ENABLED + ".");
    }

}
