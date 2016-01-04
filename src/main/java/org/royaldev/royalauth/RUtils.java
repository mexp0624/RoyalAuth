package org.royaldev.royalauth;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.royaldev.royalauth.tools.NameFetcher;
import org.royaldev.royalauth.tools.UUIDFetcher;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

public class RUtils {

	public static void dispNoPerms(CommandSender cs) {
		cs.sendMessage(ChatColor.RED + Language.NO_PERMISSION.toString());
	}

	/**
	 * Converts color codes to processed codes
	 *
	 * @param message
	 *                Message with raw color codes
	 * @return String with processed colors
	 */
	public static String colorize(final String message) {
		if (message == null)
			return null;
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	/**
	 * Removes color codes that have not been processed yet (&char)
	 * <p/>
	 * This fixes a common exploit where color codes can be embedded into
	 * other codes: &&aa (replaces &a, and the other letters combine to make
	 * &a again)
	 *
	 * @param message
	 *                String with raw color codes
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
	 * Creates a task to remind the specified CommandSender with the
	 * specified message every specified interval.
	 * <p/>
	 * If kicks are enabled, this will kick the player after the specified
	 * (in the config).
	 *
	 * @param p
	 *                CommandSender to send the message to
	 * @param pl
	 *                Plugin to register the task under
	 * @param message
	 *                Message to send (will handle color codes and send new
	 *                messages on \n)
	 * @param interval
	 *                Interval in ticks to send the message
	 * @return Task created
	 */
	private static BukkitTask createReminder(final Player p, Plugin pl, final String message, final long interval) {
		final Runnable r = new Runnable() {
			@Override
			public void run() {
				if (Config.kickPlayers) {
					AuthPlayer ap = AuthPlayer.getAuthPlayer(p.getUniqueId());
					if (!ap.isLoggedIn() && ap.getLastJoinTimestamp()
							+ (Config.kickAfter * 1000L) <= System.currentTimeMillis()) {
						Player p = ap.getPlayer();
						if (p != null)
							p.kickPlayer(Language.TOOK_TOO_LONG_TO_LOG_IN.toString());
					}
				}
				for (String line : message.split("\\n"))
					p.sendMessage(colorize(line));
			}
		};
		return pl.getServer().getScheduler().runTaskTimer(pl, r, 0L, interval);
	}

	public static BukkitTask createRegisterReminder(Player p, Plugin pl) {
		return createReminder(p, pl,
				ChatColor.RED + Language.PLEASE_REGISTER_WITH.toString() + " " + ChatColor.GRAY
						+ "/register [password]" + ChatColor.RED + "!",
				Config.remindInterval * 20L);
	}

	public static BukkitTask createLoginReminder(Player p, Plugin pl) {
		return createReminder(p, pl,
				ChatColor.RED + Language.PLEASE_LOG_IN_WITH.toString() + " " + ChatColor.GRAY
						+ "/login [password]" + ChatColor.RED + "!",
				Config.remindInterval * 20L);
	}

	/**
	 * Joins an array of strings with spaces
	 *
	 * @param array
	 *                Array to join
	 * @param position
	 *                Position to start joining from
	 * @return Joined string
	 */
	public static String getFinalArg(String[] array, int position) {
		final StringBuilder sb = new StringBuilder();
		for (int i = position; i < array.length; i++)
			sb.append(array[i]).append(" ");
		return sb.substring(0, sb.length() - 1);
	}

	public static UUID getUUID(String name) throws Exception {
		final Map<String, UUID> m = new UUIDFetcher(Arrays.asList(name)).call();
		for (Map.Entry<String, UUID> e : m.entrySet()) {
			if (e.getKey().equalsIgnoreCase(name))
				return e.getValue();
		}
		throw new Exception("Couldn't find name in results.");
	}

	public static String getName(UUID u) throws Exception {
		return new NameFetcher(Arrays.asList(u)).call().get(u);
	}

	public static String forceGetName(UUID u) {
		String name;
		try {
			name = RUtils.getName(u);
		} catch (Exception ex) {
			name = u.toString();
		}
		return name;
	}
	
	public static String genSalt(){
		final char[] chars = "!@$# ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		return sb.toString();
	}

}
