package org.royaldev.royalauth.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.royaldev.royalauth.ConnectionManager;
import org.royaldev.royalauth.RUtils;
import org.royaldev.royalauth.RoyalAuth;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class RAuthListener implements Listener {

    /*
    * TODO: Create cancellers - done?
    * NOTE: There's a reason I didn't include teleports
    */

    private RoyalAuth plugin;
    private ConnectionManager cm;

    public RAuthListener(RoyalAuth instance) {
        plugin = instance;
        cm = plugin.cm;
    }

    public final Map<String, Integer> registerReminders = new HashMap<String, Integer>();
    public final Map<String, Integer> loginReminders = new HashMap<String, Integer>();

    @EventHandler
    public void onSameName(PlayerLoginEvent e) {
        String name = e.getPlayer().getName();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Another player is already online with this name!");
                break;
            }
        }
    }

    @EventHandler
    public void kickInvalid(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        String name = p.getName();
        if (!name.matches(plugin.validRegex))
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Your name contains invalid characters!");
    }

    @EventHandler
    public void onLogin(PlayerJoinEvent e) {
        if (!plugin.sessionEnabled) return;
        int sLength = plugin.sessionLength;
        boolean check = plugin.sessionCheckIP;
        Player p = e.getPlayer();
        if (!plugin.requireRegister) {
            cm.forceLogin(p.getName());
            return;
        }
        if (!cm.isLoggedIn(p.getName()) || !cm.isSessionValid(p, sLength))
            cm.logOut(p.getName()); // Log them out as they join - otherwise, exploits can be made
        else plugin.getLogger().info(p.getName() + " was logged in via session.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        cm.updateLocation(p.getName(), p.getLocation());
        long newDate = new Date().getTime();
        cm.updateDate(p.getName(), newDate);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent e) {
        Player p = e.getPlayer();
        cm.updateLocation(p.getName(), p.getLocation());
        long newDate = new Date().getTime();
        cm.updateDate(p.getName(), newDate);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (cm.isLoggedIn(e.getPlayer().getName())) return;
        if (cm.isRegistered(p.getName()) && !cm.isLoggedIn(p.getName())) {
            synchronized (loginReminders) {
                loginReminders.put(p.getName(), RUtils.createLoginReminder(p));
            }
            return;
        }
        synchronized (registerReminders) {
            registerReminders.put(p.getName(), RUtils.createRegisterReminder(p));
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        if (plugin.teleToSpawn) e.setTo(p.getWorld().getSpawnLocation());
        else e.setTo(e.getFrom());
        e.setCancelled(true); // Hopefully other events won't touch
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        String command = e.getMessage().substring(1);
        String rootcommand = command.split(" ")[0];
        // The line below this is terrible, but it's lazier than my other methods
        if (plugin.getCommand(rootcommand) != null && !plugin.getCommand(rootcommand).equals(plugin.getCommand("royalauth")))
            return;
        boolean allowed = false;
        for (String s : plugin.allowedCommands) {
            if (s.startsWith("%")) {
                if (command.equalsIgnoreCase(s.substring(1))) {
                    allowed = true;
                    break;
                }
                continue;
            }
            for (Plugin pl : plugin.getServer().getPluginManager().getPlugins()) {
                JavaPlugin jp = (JavaPlugin) pl;
                if (jp.getCommand(rootcommand) != null && rootcommand.equalsIgnoreCase(s)) {
                    allowed = true;
                    break;
                }
            }
        }
        if (!allowed) {
            p.sendMessage(ChatColor.RED + "Please login to do that command!");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        if (plugin.allowChat) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onTakeDamage(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onAnim(PlayerAnimationEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onInteractWithEnt(PlayerInteractEntityEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onInvOpen(InventoryOpenEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        if (cm.isLoggedIn(p.getName())) return;
        e.setCancelled(true);
    }

}
