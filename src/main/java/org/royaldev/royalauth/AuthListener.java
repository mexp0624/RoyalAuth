package org.royaldev.royalauth;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
//import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class AuthListener implements Listener {

	private final RoyalAuth plugin;

	public AuthListener(RoyalAuth instance) {
		this.plugin = instance;
	}

//	@EventHandler(priority = EventPriority.MONITOR)
//	public void onJoin(AsyncPlayerPreLoginEvent e) {
//		System.out.println("[AsyncPlayerPreLoginEvent]KickMsg: " + e.getKickMessage());
//		System.out.println("[AsyncPlayerPreLoginEvent]NAME: " + e.getName());
//		System.out.println("[AsyncPlayerPreLoginEvent]UUID: " + e.getUniqueId());
//	}

	@EventHandler
	public void sameName(PlayerLoginEvent e) {
		//System.out.println("[PlayerLoginEvent]KickMag: " + e.getKickMessage());
		if (!Config.kickIfAlreadyOnline)
			return;
		AuthPlayer ap = AuthPlayer.getAuthPlayer(e.getPlayer());
		if (!ap.isLoggedIn())
			return;
		e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Language.ANOTHER_PLAYER_WITH_NAME.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent e) {
		if (!Config.requireLogin)
			return;
		Player p = e.getPlayer();
		if (Config.useLoginPermission && !p.hasPermission(Config.loginPermission))
			return;
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		ap.setLastJoinTimestamp(System.currentTimeMillis());
//		if (this.plugin.getServer().getOnlineMode() && Config.disableIfOnlineMode){
//			ap.setLoggedIn(true);
//			return;
//		}
		if (Config.sessionsEnabled && ap.isWithinSession()) {
			this.plugin.getLogger().info(p.getName() + Language.WAS_LOGGED_IN_VIA_SESSION);
			p.sendMessage(ChatColor.BLUE + Language.LOGGED_IN_VIA_SESSION.toString());
			ap.enableAfterLoginGodmode();
			ap.setLoggedIn(true);
			return;
		}
		ap.logout(this.plugin);
		if(Config.blind) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 1728000, 15, true));
		}
	}

	@EventHandler
	public void godModeAfterLogin(EntityDamageEvent e) {
		if (!Config.godModeAfterLogin)
			return;
		if (!(e.getEntity() instanceof Player))
			return;
		Player p = (Player) e.getEntity();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (!ap.isInAfterLoginGodmode())
			return;
		e.setDamage(0);
		e.setCancelled(true);
	}

	@EventHandler
	public void quit(PlayerQuitEvent e) {
		if (!Config.sessionsEnabled)
			return;
		AuthPlayer ap = AuthPlayer.getAuthPlayer(e.getPlayer());
		ap.setLastQuitTimestamp(System.currentTimeMillis());
		BukkitTask reminder = ap.getCurrentReminderTask();
		if (reminder != null)
			reminder.cancel();
		if (ap.isLoggedIn())
			ap.updateLastIPAddress();
	}

	@EventHandler
	public void kick(PlayerKickEvent e) {
		quit(new PlayerQuitEvent(e.getPlayer(), e.getLeaveMessage()));
	}

	@EventHandler
	public void onMove(PlayerMoveEvent e) {
		if (Config.allowMovementWalk)
			return;
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		Location to = e.getTo();
		Location from = e.getFrom();
		boolean walked = to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ();
		if (walked || !Config.allowMovementLook)
			e.setTo(e.getFrom());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		if (!Config.allowChat) {
			e.setCancelled(true);
			return;
		}
		e.setMessage(RUtils.colorize(Config.chatPrefix) + e.getMessage());
	}

	@EventHandler
	public void onCommand(PlayerCommandPreprocessEvent e) {
		if (Config.allowCommands)
			return;
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		String[] split = e.getMessage().split(" ");
		if (split.length < 1) {
			p.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
			return;
		}
		String root = split[0].substring(1); // the command label (remove /)
		for (String allowed : Config.allowedCommands) {
			if (!allowed.equalsIgnoreCase(e.getMessage().substring(1)))
				continue;
			return;
		}
		PluginCommand pc = this.plugin.getCommand(root);
		if (pc == null) {
			pc = this.plugin.getServer().getPluginCommand(root);
			if (pc != null) {
				if (Config.allowedCommands.contains(pc.getName()))
					return;
				for (String alias : pc.getAliases())
					if (Config.allowedCommands.contains(alias))
						return;
			}
			p.sendMessage(ChatColor.RED + Language.YOU_MUST_LOGIN.toString());
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (!Config.godMode)
			return;
		if (!(e.getEntity() instanceof Player))
			return;
		Player p = (Player) e.getEntity();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setDamage(0);
		e.setCancelled(true);
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent e) {
		if (!Config.validateUsernames)
			return;
		Player p = e.getPlayer();
		if (p.getName().matches(Config.usernameRegex))
			return;
		e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
		e.setKickMessage(Language.INVALID_USERNAME.toString());
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onDealDamage(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Player))
			return;
		Player p = (Player) e.getDamager();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onInventory(InventoryClickEvent e) {
		if (!(e.getWhoClicked() instanceof Player))
			return;
		Player p = (Player) e.getWhoClicked();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void sign(SignChangeEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void blockDamage(BlockDamageEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void enchantItem(EnchantItemEvent e) {
		Player p = e.getEnchanter();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onPrepareEnchant(PrepareItemEnchantEvent e) {
		Player p = e.getEnchanter();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void playerPortal(PlayerPortalEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onPickup(PlayerPickupItemEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onBreakHanging(HangingBreakByEntityEvent e) {
		if (!(e.getRemover() instanceof Player))
			return;
		Player p = (Player) e.getRemover();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onPlaceHanging(HangingPlaceEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onCraft(CraftItemEvent e) {
		if (!(e.getWhoClicked() instanceof Player))
			return;
		Player p = (Player) e.getWhoClicked();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onOpen(InventoryOpenEvent e) {
		if (!(e.getPlayer() instanceof Player))
			return;
		Player p = (Player) e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onAnimate(PlayerAnimationEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onEnterBed(PlayerBedEnterEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onEmpty(PlayerBucketEmptyEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onFill(PlayerBucketFillEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onFish(PlayerFishEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onGamemode(PlayerGameModeChangeEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onIntEntity(PlayerInteractEntityEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onItemConsume(PlayerItemConsumeEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void onShear(PlayerShearEntityEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void toggleSneak(PlayerToggleSneakEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void toggleFly(PlayerToggleFlightEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void toggleSprint(PlayerToggleSprintEvent e) {
		Player p = e.getPlayer();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void enterVehicle(VehicleEnterEvent e) {
		if (!(e.getEntered() instanceof Player))
			return;
		Player p = (Player) e.getEntered();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

	@EventHandler
	public void exitVehicle(VehicleExitEvent e) {
		if (!(e.getExited() instanceof Player))
			return;
		Player p = (Player) e.getExited();
		AuthPlayer ap = AuthPlayer.getAuthPlayer(p);
		if (ap.isLoggedIn())
			return;
		e.setCancelled(true);
	}

}
