package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

import java.util.Date;

public class CmdRoyalAuth implements CommandExecutor {

    private RoyalAuth plugin;

    public CmdRoyalAuth(RoyalAuth instance) {
        plugin = instance;
    }

    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("royalauth")) {
            if (args.length < 1) {
                cs.sendMessage(cmd.getDescription());
                return false;
            }
            String subcommand = args[0];
            if (subcommand.equalsIgnoreCase("help") || subcommand.equals("?")) {
                if (!plugin.isAuthorized(cs, "rauth.admin.help")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                cs.sendMessage(ChatColor.GRAY + "/" + label + " reload" + ChatColor.BLUE + " - Reloads RoyalAuth and displays version number.");
                cs.sendMessage(ChatColor.GRAY + "/" + label + " register [player] [pass]" + ChatColor.BLUE + " - Registers a player by force.");
                cs.sendMessage(ChatColor.GRAY + "/" + label + " logout [player]" + ChatColor.BLUE + " - Logs a player out by force.");
                cs.sendMessage(ChatColor.GRAY + "/" + label + " changepass [player] [newpass]" + ChatColor.BLUE + " - Changes a player's password.");
            } else if (subcommand.equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.reloadConfigVals();
                cs.sendMessage(ChatColor.BLUE + "Reloaded RoyalAuth " + ChatColor.GRAY + "v" + plugin.getDescription().getVersion() + ChatColor.BLUE + ".");
            } else if (subcommand.equalsIgnoreCase("logout")) {
                if (!plugin.isAuthorized(cs, "rauth.admin.logout")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 2) {
                    cs.sendMessage(ChatColor.RED + "Not enough arguments!");
                    cs.sendMessage(ChatColor.RED + "Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + " for help.");
                    return true;
                }
                String name = args[1];
                Player t = plugin.getServer().getPlayer(name);
                if (t != null) name = t.getName();
                if (!plugin.cm.isLoggedIn(name)) {
                    cs.sendMessage(ChatColor.RED + "That player isn't logged in!");
                    return true;
                }
                plugin.cm.logOut(name);
                cs.sendMessage(ChatColor.BLUE + "You have logged out " + ChatColor.GRAY + name + ChatColor.BLUE + ".");
                if (t != null)
                    t.sendMessage(ChatColor.RED + "You have been logged out by " + ChatColor.GRAY + cs.getName() + ChatColor.RED + ".");
            } else if (subcommand.equalsIgnoreCase("register")) {
                if (!plugin.isAuthorized(cs, "rauth.admin.register")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 3) {
                    cs.sendMessage(ChatColor.RED + "Not enough arguments!");
                    cs.sendMessage(ChatColor.RED + "Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + " for help.");
                    return true;
                }
                String name = args[1];
                String password = args[2];
                Player p = plugin.getServer().getPlayer(name);
                if (p != null) name = p.getName();
                if (plugin.cm.isRegistered(name)) {
                    cs.sendMessage(ChatColor.RED + "That player is already registered!");
                    return true;
                }
                String ip = (p != null) ? p.getAddress().getAddress().toString().replace("/", "") : "0.0.0.0";
                boolean success = plugin.cm.register(name, password, new Date().getTime(), ip);
                if (success)
                    cs.sendMessage(ChatColor.BLUE + "Registered " + ChatColor.GRAY + name + ChatColor.BLUE + " successfully.");
                else
                    cs.sendMessage(ChatColor.RED + "Could not register " + ChatColor.GRAY + name + ChatColor.RED + "!");
                if (p != null && success)
                    p.sendMessage(ChatColor.BLUE + "You have been registered by " + ChatColor.GRAY + cs.getName() + ChatColor.BLUE + ".");
            } else if (subcommand.equalsIgnoreCase("changepass")) {
                if (!plugin.isAuthorized(cs, "rauth.admin.changepass")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                if (args.length < 3) {
                    cs.sendMessage(ChatColor.RED + "Not enough arguments!");
                    cs.sendMessage(ChatColor.RED + "Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + " for help.");
                    return true;
                }
                String name = args[1];
                String password = args[2];
                Player p = plugin.getServer().getPlayer(name);
                if (p != null) name = p.getName();
                if (!plugin.cm.isRegistered(name)) {
                    cs.sendMessage(ChatColor.RED + "That player is not registered!");
                    return true;
                }
                boolean success = plugin.cm.changePassword(name, password);
                if (success)
                    cs.sendMessage(ChatColor.BLUE + "You have changed the password of " + ChatColor.GRAY + name + ChatColor.BLUE + ".");
                else
                    cs.sendMessage(ChatColor.RED + "Could not change the password of " + ChatColor.GRAY + name + ChatColor.RED + ".");
                if (p != null && success)
                    p.sendMessage(ChatColor.BLUE + "Your password has been changed by " + ChatColor.GRAY + cs.getName() + ChatColor.BLUE + ".");
            } else {
                cs.sendMessage(ChatColor.RED + "Invalid subcommand! Try " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + " for help.");
            }
            return true;
        }
        return false;
    }

}
