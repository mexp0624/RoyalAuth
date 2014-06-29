package org.royaldev.royalauth.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalauth.AuthPlayer;
import org.royaldev.royalauth.Config;
import org.royaldev.royalauth.Language;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

public class CmdRoyalAuth implements CommandExecutor {

    private final RoyalAuth plugin;

    public CmdRoyalAuth(RoyalAuth instance) {
        plugin = instance;
    }

    @Override
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
                AuthPlayer ap = AuthPlayer.getAuthPlayer(((Player) cs).getUniqueId());
                if (!ap.isLoggedIn()) {
                    cs.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
                    return true;
                }
            }
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("help")) {
                cs.sendMessage(ChatColor.BLUE + Language.ADMIN_HELP.toString());
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " changepassword [player] [newpassword]" + ChatColor.BLUE + " - " + Language.HELP_CHANGEPASSWORD);
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " login [player]" + ChatColor.BLUE + " - " + Language.HELP_LOGIN);
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " logout [player]" + ChatColor.BLUE + " - " + Language.HELP_LOGOUT);
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " register [player] [password]" + ChatColor.BLUE + " - " + Language.HELP_REGISTER);
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " reload" + ChatColor.BLUE + " - " + Language.HELP_RELOAD);
                cs.sendMessage(ChatColor.GRAY + "  /" + label + " help" + ChatColor.BLUE + " - " + Language.HELP_HELP);
            } else if (subcommand.equals("changepassword")) {
                if (args.length < 3) {
                    cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    return true;
                }
                AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                if (ap == null) {
                    cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                    return true;
                }
                if (!ap.isRegistered()) {
                    cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_REGISTERED.toString());
                    return true;
                }
                if (ap.setPassword(args[2], Config.passwordHashType))
                    cs.sendMessage(ChatColor.BLUE + Language.PASSWORD_CHANGED.toString());
                else cs.sendMessage(ChatColor.RED + Language.PASSWORD_COULD_NOT_BE_CHANGED.toString());
            } else if (subcommand.equals("login")) {
                if (args.length < 2) {
                    cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    return true;
                }
                AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                if (ap == null) {
                    cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                    return true;
                }
                Player p = ap.getPlayer();
                if (p == null) {
                    cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_ONLINE.toString());
                    return true;
                }
                ap.login();
                plugin.getLogger().info(p.getName() + " " + Language.HAS_LOGGED_IN);
                cs.sendMessage(ChatColor.BLUE + Language.PLAYER_LOGGED_IN.toString());
            } else if (subcommand.equals("logout")) {
                if (args.length < 2) {
                    cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    return true;
                }
                AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                if (ap == null) {
                    cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                    return true;
                }
                Player p = ap.getPlayer();
                if (p == null) {
                    cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_ONLINE.toString());
                    return true;
                }
                if (!ap.isLoggedIn()) {
                    cs.sendMessage(ChatColor.RED + Language.PLAYER_NOT_LOGGED_IN.toString());
                    return true;
                }
                ap.logout(plugin);
                cs.sendMessage(ChatColor.BLUE + Language.PLAYER_LOGGED_OUT.toString());
            } else if (subcommand.equals("register")) {
                if (args.length < 3) {
                    cs.sendMessage(ChatColor.RED + Language.NOT_ENOUGH_ARGUMENTS.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
                    return true;
                }
                AuthPlayer ap = AuthPlayer.getAuthPlayer(args[1]);
                if (ap == null) {
                    cs.sendMessage(ChatColor.RED + Language.ERROR_OCCURRED.toString());
                    return true;
                }
                final String name = RUtils.forceGetName(ap.getUniqueId());
                if (ap.isRegistered()) {
                    cs.sendMessage(ChatColor.RED + Language.PLAYER_ALREADY_REGISTERED.toString());
                    return true;
                }
                String rawPassword = args[2];
                for (String disallowed : Config.disallowedPasswords) {
                    if (!rawPassword.equalsIgnoreCase(disallowed)) continue;
                    cs.sendMessage(ChatColor.RED + Language.DISALLOWED_PASSWORD.toString());
                    return true;
                }
                if (ap.setPassword(rawPassword, Config.passwordHashType))
                    cs.sendMessage(ChatColor.BLUE + String.format(Language.REGISTERED_SUCCESSFULLY.toString(), ChatColor.GRAY + name + ChatColor.BLUE));
                else
                    cs.sendMessage(ChatColor.RED + String.format(Language.COULD_NOT_REGISTER.toString(), ChatColor.GRAY + name + ChatColor.RED));
            } else if (subcommand.equals("reload")) {
                plugin.c.reloadConfiguration();
                cs.sendMessage(ChatColor.BLUE + Language.CONFIGURATION_RELOADED.toString());
            } else {
                cs.sendMessage(ChatColor.RED + Language.INVALID_SUBCOMMAND.toString() + " " + Language.TRY + " " + ChatColor.GRAY + "/" + label + " help" + ChatColor.RED + ".");
            }
            return true;
        }
        return false;
    }

}
