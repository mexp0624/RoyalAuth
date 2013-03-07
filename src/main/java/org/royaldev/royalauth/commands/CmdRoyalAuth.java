package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.Config;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

public class CmdRoyalAuth implements CommandExecutor {

    private final RoyalAuth plugin;

    public CmdRoyalAuth(RoyalAuth instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("royalauth")) {
            if (!cs.hasPermission("rauth.royalauth")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            if (cs instanceof Player) {
                AuthPlayer ap = AuthPlayer.getAuthPlayer(cs.getName());
                if (!ap.isLoggedIn()) {
                    cs.sendMessage(ChatColor.RED + "You must log in to use that command.");
                    return true;
                }
            }
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("help")) {
                cs.sendMessage(ChatColor.BLUE + "RoyalAuth Administrative Help");
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " changepassword [player] [newpassword]" + ChatColor.BLUE + " - Changes any player's password.");
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " login [player]" + ChatColor.BLUE + " - Logs a player in.");
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " logout [player]" + ChatColor.BLUE + " - Logs a player out.");
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " help" + ChatColor.BLUE + " - Displays this help.");
            } else if (subcommand.equals("changepassword")) {
                if (args.length < 3) {
                    cs.sendMessage(ChatColor.RED + "Not enough arguments. Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    return true;
                }
                OfflinePlayer op = plugin.getServer().getPlayer(args[1]);
                if (op == null) op = plugin.getServer().getOfflinePlayer(args[1]);
                AuthPlayer ap = AuthPlayer.getAuthPlayer(op.getName());
                if (!ap.isRegistered()) {
                    cs.sendMessage(ChatColor.RED + "That player is not registered yet!");
                    return true;
                }
                if (ap.setPassword(args[2], Config.passwordHashType))
                    cs.sendMessage(ChatColor.BLUE + "Password changed.");
                else cs.sendMessage(ChatColor.RED + "Password could not be changed.");
            } else if (subcommand.equals("login")) {
                if (args.length < 2) {
                    cs.sendMessage(ChatColor.RED + "Not enough arguments. Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    return true;
                }
                OfflinePlayer op = plugin.getServer().getPlayer(args[1]);
                if (op == null) op = plugin.getServer().getOfflinePlayer(args[1]);
                AuthPlayer ap = AuthPlayer.getAuthPlayer(op.getName());
                Player p = ap.getPlayer();
                if (p == null) {
                    cs.sendMessage(ChatColor.RED + "That player is not online!");
                    return true;
                }
                ap.login();
                plugin.getLogger().info(p.getName() + " has logged in.");
                cs.sendMessage(ChatColor.BLUE + "That player has been logged in.");
            } else if (subcommand.equals("logout")) {
                if (args.length < 2) {
                    cs.sendMessage(ChatColor.RED + "Not enough arguments. Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    return true;
                }
                OfflinePlayer op = plugin.getServer().getPlayer(args[1]);
                if (op == null) op = plugin.getServer().getOfflinePlayer(args[1]);
                AuthPlayer ap = AuthPlayer.getAuthPlayer(op.getName());
                Player p = ap.getPlayer();
                if (p == null) {
                    cs.sendMessage(ChatColor.RED + "That player is not online!");
                    return true;
                }
                if (!ap.isLoggedIn()) {
                    cs.sendMessage(ChatColor.RED + "That player is not logged in!");
                    return true;
                }
                ap.logout(plugin);
                cs.sendMessage(ChatColor.BLUE + "Logged that player out.");
            } else {
                cs.sendMessage(ChatColor.RED + "Invalid subcommand. Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
            }
            return true;
            //AuthPlayer ap = AuthPlayer.getAuthPlayer(args[0]);
        }
        return false;
    }

}
