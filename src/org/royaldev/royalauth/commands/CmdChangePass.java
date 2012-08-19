package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

public class CmdChangePass implements CommandExecutor {

    private RoyalAuth plugin;

    public CmdChangePass(RoyalAuth instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("changepass")) {
            if (!plugin.isAuthorized(cs, "rauth.changepass")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command is only available to players!");
                return true;
            }
            if (args.length < 2) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            Player p = (Player) cs;
            String oldPass = args[0];
            String newPass = args[1];
            if (!plugin.cm.isRegistered(p.getName())) {
                cs.sendMessage(ChatColor.RED + "You are not registered yet!");
                return true;
            }
            boolean success = plugin.cm.changePassword(p.getName(), oldPass, newPass);
            if (success)
                cs.sendMessage(ChatColor.BLUE + "Your password has been changed.");
            else
                cs.sendMessage(ChatColor.RED + "Could not change your password. Did you spell your old password correctly?");
            return true;
        }
        return false;
    }

}
