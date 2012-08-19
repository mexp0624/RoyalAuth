package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

public class CmdLogOut implements CommandExecutor {

    private RoyalAuth plugin;

    public CmdLogOut(RoyalAuth instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("logout")) {
            if (!plugin.isAuthorized(cs, "rauth.logout")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command is only available to players!");
                return true;
            }
            Player p = (Player) cs;
            if (!plugin.cm.isRegistered(p.getName())) {
                cs.sendMessage(ChatColor.RED + "You are not registered!");
                return true;
            }
            if (!plugin.cm.isLoggedIn(p.getName())) {
                cs.sendMessage(ChatColor.RED + "You are not logged in!");
                return true;
            }
            boolean success = plugin.cm.logOut(p.getName());
            if (success) cs.sendMessage(ChatColor.BLUE + "You have been logged out.");
            else cs.sendMessage(ChatColor.RED + "Could not log you out!");
            return true;
        }
        return false;
    }

}
