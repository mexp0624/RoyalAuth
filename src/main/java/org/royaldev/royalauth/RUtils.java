package org.royaldev.royalauth;

import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.regex.Pattern;

public class RUtils {

    public static void dispNoPerms(CommandSender cs) {
        cs.sendMessage(ChatColor.RED + "You do not have permission for that!");
    }

    /**
     * Converts color codes to processed codes
     *
     * @param message Message with raw color codes
     * @return String with processed colors
     */
    public static String colorize(final String message) {
        if (message == null) return null;
        return message.replaceAll("(?i)&([a-f0-9k-or])", ChatColor.COLOR_CHAR + "$1");
    }

    /**
     * Removes color codes that have not been processed yet (&char)
     * <p/>
     * This fixes a common exploit where color codes can be embedded into other codes:
     * &&aa (replaces &a, and the other letters combine to make &a again)
     *
     * @param message String with raw color codes
     * @return String without raw color codes
     */
    public static String decolorize(String message) {
        Pattern p = Pattern.compile("(?i)&[a-f0-9k-or]");
        boolean contains = p.matcher(message).find();
        while (contains) {
            message = message.replaceAll("(?i)&[a-f0-9k-or]", "");
            contains = p.matcher(message).find();
        }
        return message;
    }

    /**
     * Creates a task to remind the specified CommandSender with the specified message every specified interval.
     * <p/>
     * If kicks are enabled, this will kick the player after the specified (in the config).
     *
     * @param cs       CommandSender to send the message to
     * @param p        Plugin to register the task under
     * @param message  Message to send (will handle color codes and send new messages on \n)
     * @param interval Interval in ticks to send the message
     * @return Task created
     */
    private static BukkitTask createReminder(final CommandSender cs, Plugin p, final String message, final long interval) {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (Config.kickPlayers) {
                    AuthPlayer ap = AuthPlayer.getAuthPlayer(cs.getName());
                    if (!ap.isLoggedIn() && ap.getLastJoinTimestamp() + (Config.kickAfter * 1000L) <= System.currentTimeMillis()) {
                        Player p = ap.getPlayer();
                        if (p != null) p.kickPlayer("You took too long to login!");
                    }
                }
                for (String line : message.split("\\n")) cs.sendMessage(colorize(line));
            }
        };
        return p.getServer().getScheduler().runTaskTimer(p, r, 0L, interval);
    }

    public static BukkitTask createRegisterReminder(CommandSender cs, Plugin p) {
        return createReminder(cs, p, ChatColor.RED + "Please register with " + ChatColor.GRAY + "/register [password]" + ChatColor.RED + "!", Config.remindInterval * 20L);
    }

    public static BukkitTask createLoginReminder(CommandSender cs, Plugin p) {
        return createReminder(cs, p, ChatColor.RED + "Please login with " + ChatColor.GRAY + "/login [password]" + ChatColor.RED + "!", Config.remindInterval * 20L);
    }

    /**
     * Joins an array of strings with spaces
     *
     * @param array    Array to join
     * @param position Position to start joining from
     * @return Joined string
     */
    public static String getFinalArg(String[] array, int position) {
        StrBuilder sb = new StrBuilder();
        for (int i = position; i < array.length; i++) {
            sb.append(array[i]);
            sb.append(" ");
        }
        return sb.substring(0, sb.length() - 1);
    }

}
