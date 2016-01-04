package org.royaldev.royalauth;

import com.google.common.io.PatternFilenameFilter;

import org.apache.logging.log4j.LogManager;
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
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

public class RoyalAuth extends JavaPlugin {

	public static File dataFolder;
	//private final Pattern versionPattern = Pattern
	//		.compile("((\\d+\\.?){3})(\\-SNAPSHOT)?(\\-local\\-(\\d{8}\\.\\d{6})|\\-(\\d+))?");
	public Config c;
	public Logger log;
	public org.apache.logging.log4j.core.Logger CoreLog;
	private CommandFilter commandFilter = new CommandFilter();
	public boolean EN = true;

	/**
	 * Registers a command in the server. If the command isn't defined in
	 * plugin.yml the NPE is caught, and a warning line is sent to the
	 * console.
	 *
	 * @param ce
	 *                CommandExecutor to be registered
	 * @param command
	 *                Command name as specified in plugin.yml
	 * @param jp
	 *                Plugin to register under
	 */
	private void registerCommand(CommandExecutor ce, String command, JavaPlugin jp) {
		try {
			jp.getCommand(command).setExecutor(ce);
		} catch (NullPointerException e) {
			jp.getLogger().warning(String.format(Language.COULD_NOT_REGISTER_COMMAND.toString(), command,
					e.getMessage()));
		}
	}

	private void update() {
		final File userdataFolder = new File(dataFolder, "userdata");
		if (!userdataFolder.exists() || !userdataFolder.isDirectory())
			return;
		for (String fileName : userdataFolder.list(new PatternFilenameFilter("(?i)^.+\\.yml$"))) {
			String playerName = fileName.substring(0, fileName.length() - 4); // ".yml"
												// =
												// 4
			try {
				// noinspection ResultOfMethodCallIgnored
				UUID.fromString(playerName);
				continue;
			} catch (IllegalArgumentException ignored) {
			}
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
				this.getLogger().info(
						String.format(Language.CONVERTED_USERDATA.toString(), fileName, u));
			} else {
				this.getLogger().warning(String.format(Language.COULD_NOT_CONVERT_USERDATA.toString(),
						fileName));
			}
		}
	}

	private void saveLangFile(String name) {
		if (!new File(this.getDataFolder() + File.separator + "lang" + File.separator + name + ".properties")
				.exists())
			this.saveResource("lang" + File.separator + name + ".properties", false);
	}

	@Override
	public void onDisable() {
		this.getServer().getScheduler().cancelTasks(this);

		for (Player p : this.getServer().getOnlinePlayers()) {
			AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
			if (ap.isLoggedIn())
				ap.logout(this, false);
		}

		PConfManager.saveAllManagers();
		PConfManager.purge();
		
		//this.log.setFilter(commandFilter.prevFilter);
		//commandFilter.prevFilter = null;
		commandFilter.stop();
	}

	@Override
	public void onEnable() {
		RoyalAuth.dataFolder = this.getDataFolder();

		if (!new File(getDataFolder(), "config.yml").exists())
			this.saveDefaultConfig();

		this.c = new Config(this);
		//this.log = this.getLogger();
		
		// Filter logs
		this.CoreLog = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
		if(this.CoreLog != null){
			this.CoreLog.addFilter(commandFilter);
			commandFilter.start();
		}else{
			this.log.warning("getRootLogger() fail!!");
		}
		//commandFilter.prevFilter = log.getFilter();
		//this.log.setFilter(commandFilter);
		
		if (this.getServer().getOnlineMode() && Config.disableIfOnlineMode){
			this.EN = false;
		}

		this.saveLangFile("en_us");

		try {
			new Language.LanguageHelper(new File(this.getDataFolder(),
					this.getConfig().getString("general.language_file", "lang/en_us.properties")));
		} catch (IOException e) {
			this.log.severe("Could not load language file: " + e.getMessage());
			this.log.severe("Disabling plugin.");
			this.setEnabled(false);
			return;
		}

		if (Config.checkOldUserdata)
			this.update();

		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(new AuthListener(this), this);

		this.registerCommand(new CmdRoyalAuth(this), "royalauth", this);
		this.registerCommand(new CmdLogin(this), "login", this);
		this.registerCommand(new CmdLogout(this), "logout", this);
		this.registerCommand(new CmdRegister(this), "register", this);
		this.registerCommand(new CmdChangePassword(), "passwd", this);
		
		for (Player p : this.getServer().getOnlinePlayers()) {
			AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
			if (ap.isLoggedIn())
				continue;
			if (ap.isRegistered())
				ap.createLoginReminder(this);
			else
				ap.createRegisterReminder(this);
		}

		//this.log.info(this.getDescription().getName() + " v" + this.getDescription().getVersion() + " " + Language.ENABLED + ".");
	}

}
