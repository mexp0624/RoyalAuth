package org.royaldev.royalauth;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;

public class RUtils {

    public static int createLoginReminder(final Player p) {
        final long start = new Date().getTime();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                // getTime() + 10000 <- the runner has a delay of 10 seconds, so add that time back
                if ((new Date().getTime() + 10000) - start > RoyalAuth.instance.kickTime * 1000) {
                    p.kickPlayer("You did not log in fast enough!");
                    return;
                }
                p.sendMessage(ChatColor.RED + "You are not logged in!");
                p.sendMessage(ChatColor.RED + "To login, do " + ChatColor.GRAY + "/login [password]");
            }
        };
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(RoyalAuth.instance, r, 0, 200);
    }

    public static int createRegisterReminder(final Player p) {
        final long start = new Date().getTime();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if ((new Date().getTime() + 10000) - start > RoyalAuth.instance.kickTime * 1000) {
                    p.kickPlayer("You did not register fast enough!");
                    return;
                }
                p.sendMessage(ChatColor.RED + "You are not registered!");
                p.sendMessage(ChatColor.RED + "To register, do " + ChatColor.GRAY + "/register [password]");
            }
        };
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(RoyalAuth.instance, r, 0, 200);
    }

    public static void dispNoPerms(CommandSender cs) {
        cs.sendMessage(ChatColor.RED + "You don't have permission for that!");
    }

}
