package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

public class CmdLogin implements CommandExecutor {

    private RoyalAuth plugin;

    public CmdLogin(RoyalAuth instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("login")) {
            if (!plugin.isAuthorized(cs, "rauth.login")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }
            Player p = (Player) cs;
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            if (plugin.cm.isLoggedIn(p.getName())) {
                p.sendMessage(ChatColor.RED + "You are already logged in!");
                return true;
            }
            if (args.length > 1) {
                cs.sendMessage(ChatColor.RED + "Your password may not contain spaces!");
                return true;
            }
            String password = args[0];
            Boolean success = plugin.cm.attemptLogin(p.getName(), password);
            if (success == null) {
                cs.sendMessage(ChatColor.RED + "This plugin has been set up incorrectly. Alert a server administrator, as you cannot login until this is fixed.");
                plugin.log.severe("Cannot log in " + p.getName() + " because an invalid encryption type is specified in the config!");
                return true;
            }
            if (success) {
                p.sendMessage(ChatColor.BLUE + "You have logged in successfully!");
                Integer taskID = plugin.rListener.loginReminders.get(p.getName());
                if (taskID != null && taskID > -1)
                    plugin.getServer().getScheduler().cancelTask(taskID);
                plugin.log.info(p.getName() + " logged in.");
                if (password.matches("(?i)\\[?password\\]?") || password.matches("(?i)\\[?" + p.getName() + "\\]?"))
                    cs.sendMessage(ChatColor.RED + "You are using a very insecure password. Please change it as soon as possible.");
                if (plugin.teleToSpawn) {
                    Location l = plugin.cm.getLocation(p.getName());
                    if (l != null) p.teleport(l);
                }
            } else {
                plugin.log.warning(p.getName() + " used the wrong password!");
                p.sendMessage(ChatColor.RED + "Invalid password; try again.");
            }
            return true;
        }
        return false;
    }

}
