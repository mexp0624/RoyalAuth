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
import org.royaldev.royalauth.RoyalAuth;

import java.security.NoSuchAlgorithmException;

public class CmdLogin implements CommandExecutor {

    private final RoyalAuth plugin;

    public CmdLogin(RoyalAuth instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("login")) {
            if (!cs.hasPermission("rauth.login")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command is only available to players!");
                return true;
            }
            Player p = (Player) cs;
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn()) {
                cs.sendMessage(ChatColor.RED + "You are already logged in!");
                return true;
            }
            String rawPassword = RUtils.getFinalArg(args, 0); // support spaces
            for (String disallowed : Config.disallowedPasswords) {
                if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
                cs.sendMessage(ChatColor.RED + "That password is disallowed! Please change it to something else.");
            }
            String hashType = (!ap.getHashType().equalsIgnoreCase(Config.passwordHashType)) ? ap.getHashType() : Config.passwordHashType;
            try {
                rawPassword = Hasher.encrypt(rawPassword, hashType);
            } catch (NoSuchAlgorithmException e) {
                cs.sendMessage(ChatColor.RED + "You could not be logged in.");
                cs.sendMessage(ChatColor.RED + "Your server administrator has set this plugin up incorrectly.");
                cs.sendMessage(ChatColor.RED + "Please contact him/her to resolve this issue.");
                return true;
            }
            String realPassword = ap.getPasswordHash();
            if (rawPassword.equals(realPassword)) {
                ap.login();
                plugin.getLogger().info(p.getName() + " has logged in.");
                cs.sendMessage(ChatColor.BLUE + "You have been logged in successfully.");
            } else {
                plugin.getLogger().warning(p.getName() + " tried to log in with an incorrect password!");
                cs.sendMessage(ChatColor.RED + "That password was incorrect. Please try again.");
            }
            return true;
        }
        return false;
    }

}
