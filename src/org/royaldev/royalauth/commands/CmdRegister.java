package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

import java.util.Date;

public class CmdRegister implements CommandExecutor {

    private RoyalAuth plugin;

    public CmdRegister(RoyalAuth instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("register")) {
            if (!plugin.isAuthorized(cs, "rauth.register")) {
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
            if (plugin.cm.isRegistered(p.getName())) {
                cs.sendMessage(ChatColor.RED + "You are already registered!");
                return true;
            }
            if (args.length > 1) {
                cs.sendMessage(ChatColor.RED + "Your password may not contain spaces!");
                return true;
            }
            String password = args[0];
            if (password.matches("(?i)\\[?password\\]?") || password.matches("(?i)\\[?" + p.getName() + "\\]?")) {
                cs.sendMessage(ChatColor.RED + "Replace password with an actual password, please!");
                return true;
            }
            boolean success = plugin.cm.register(p.getName(), password, new Date().getTime(), p.getAddress().getAddress().toString().replace("/", ""));
            if (success) {
                p.sendMessage(ChatColor.BLUE + "You have registered successfully!");
                Integer taskID = plugin.rListener.registerReminders.get(p.getName());
                if (taskID != null && taskID > -1)
                    plugin.getServer().getScheduler().cancelTask(taskID);
            } else p.sendMessage(ChatColor.RED + "Could not register you!");
            return true;
        }
        return false;
    }

}
