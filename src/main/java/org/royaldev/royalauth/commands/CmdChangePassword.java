package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.Config;
import org.royaldev.royalauth.Hasher;
import org.royaldev.royalauth.RUtils;

import java.security.NoSuchAlgorithmException;

public class CmdChangePassword implements CommandExecutor {

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("changepassword")) {
            if (!cs.hasPermission("rauth.changepassword")) {
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
                cs.sendMessage(ChatColor.RED + "You must log in to use that command.");
                return true;
            }
            if (args.length < 2) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            String oldPassword;
            String newPassword = args[1];
            for (String disallowed : Config.disallowedPasswords) {
                if (!newPassword.equalsIgnoreCase(disallowed)) continue;
                cs.sendMessage(ChatColor.RED + "That password is disallowed! Use a different one.");
                return true;
            }
            try {
                oldPassword = Hasher.encrypt(args[0], ap.getHashType());
                newPassword = Hasher.encrypt(newPassword, Config.passwordHashType);
            } catch (NoSuchAlgorithmException e) {
                cs.sendMessage(ChatColor.RED + "The server admin has set this plugin up incorrectly.");
                cs.sendMessage(ChatColor.RED + "Your password could not be changed.");
                return true;
            }
            if (!ap.getPasswordHash().equals(oldPassword)) {
                cs.sendMessage(ChatColor.RED + "Your old password was incorrect.");
                return true;
            }
            ap.setHashedPassword(newPassword, Config.passwordHashType);
            cs.sendMessage(ChatColor.BLUE + "Your password has been changed.");
            return true;
        }
        return false;
    }

}
