package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.Config;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

public class CmdRegister implements CommandExecutor {

    private final RoyalAuth plugin;

    public CmdRegister(RoyalAuth instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("register")) {
            if (!cs.hasPermission("rauth.register")) {
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
            if (ap.isLoggedIn() || ap.isRegistered()) {
                cs.sendMessage(ChatColor.RED + "You are already registered!");
                return true;
            }
            String rawPassword = args[0]; // no space support
            for (String disallowed : Config.disallowedPasswords) {
                if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
                cs.sendMessage(ChatColor.RED + "That password is disallowed! Use a different one.");
                return true;
            }
            if (ap.setPassword(rawPassword, Config.passwordHashType)) {
                plugin.getLogger().info(p.getName() + " has registered.");
                cs.sendMessage(ChatColor.BLUE + "Your password has been set, and you have been registered.");
                BukkitTask reminder = ap.getCurrentReminderTask();
                if (reminder != null) reminder.cancel();
                ap.createLoginReminder(plugin);
            } else cs.sendMessage(ChatColor.RED + "Your password could not be set!");
            return true;
        }
        return false;
    }

}
