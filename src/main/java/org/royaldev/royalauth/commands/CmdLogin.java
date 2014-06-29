package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.Config;
import org.royaldev.royalauth.Hasher;
import org.royaldev.royalauth.Language;
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
                cs.sendMessage(ChatColor.RED + Language.COMMAND_NO_CONSOLE.toString());
                return true;
            }
            Player p = (Player) cs;
            AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
            if (ap.isLoggedIn()) {
                cs.sendMessage(ChatColor.RED + Language.ALREADY_LOGGED_IN.toString());
                return true;
            }
            String rawPassword = RUtils.getFinalArg(args, 0); // support spaces
            for (String disallowed : Config.disallowedPasswords) {
                if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
                cs.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
            }
            String hashType = (!ap.getHashType().equalsIgnoreCase(Config.passwordHashType)) ? ap.getHashType() : Config.passwordHashType;
            try {
                rawPassword = Hasher.encrypt(rawPassword, hashType);
            } catch (NoSuchAlgorithmException e) {
                cs.sendMessage(ChatColor.RED + Language.COULD_NOT_LOG_IN.toString());
                cs.sendMessage(ChatColor.RED + Language.ADMIN_SET_UP_INCORRECTLY.toString());
                cs.sendMessage(ChatColor.RED + Language.CONTACT_ADMIN.toString());
                return true;
            }
            String realPassword = ap.getPasswordHash();
            if (rawPassword.equals(realPassword)) {
                ap.login();
                plugin.getLogger().info(p.getName() + " " + Language.HAS_LOGGED_IN);
                cs.sendMessage(ChatColor.BLUE + Language.LOGGED_IN_SUCCESSFULLY.toString());
            } else {
                plugin.getLogger().warning(p.getName() + Language.USED_INCORRECT_PASSWORD);
                cs.sendMessage(ChatColor.RED + Language.INCORRECT_PASSWORD.toString());
            }
            return true;
        }
        return false;
    }

}
