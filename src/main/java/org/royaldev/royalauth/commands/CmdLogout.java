package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

public class CmdLogout implements CommandExecutor {

    private final RoyalAuth plugin;

    public CmdLogout(RoyalAuth instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("logout")) {
            if (!cs.hasPermission("rauth.logout")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command is only available to players!");
                return true;
            }
            Player p = (Player) cs;
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (!ap.isLoggedIn()) {
                cs.sendMessage(ChatColor.RED + "You are not logged in!");
                return true;
            }
            cs.sendMessage(ChatColor.BLUE + "You have been logged out.");
            ap.logout(plugin);
            return true;
        }
        return false;
    }

}
