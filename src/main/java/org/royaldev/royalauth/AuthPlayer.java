package org.royaldev.royalauth;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthPlayer {

	private final static Map<UUID, AuthPlayer> authPlayers = new HashMap<>();
	private final PConfManager pcm;
	private UUID playerUUID;
	private String lastIPAddress;
	private long lastJoinTimestamp = 0L;
	private long lastLoginTimestamp = 0L;
	private long lastQuitTimestamp = 0L;
	private BukkitTask reminderTask = null;

	private AuthPlayer(UUID u) {
		this.playerUUID = u;
		this.pcm = PConfManager.getPConfManager(u);
		this.lastJoinTimestamp = this.pcm.getLong("timestamps.join", 0L);
		this.lastLoginTimestamp = this.pcm.getLong("timestamps.login", 0L);
		this.lastQuitTimestamp = this.pcm.getLong("timestamps.quit", 0L);
	}

	private AuthPlayer(Player p) {
		this(p.getUniqueId());
	}

	/**
	 * Gets the AuthPlayer for the name of a player.
	 *
	 * @param u
	 *                UUID to get AuthPlayer of
	 * @return AuthPlayer
	 */
	public static AuthPlayer getAuthPlayer(UUID u) {
		synchronized (AuthPlayer.authPlayers) {
			if (AuthPlayer.authPlayers.containsKey(u))
				return AuthPlayer.authPlayers.get(u);
			final AuthPlayer ap = new AuthPlayer(u);
			AuthPlayer.authPlayers.put(u, ap);
			return ap;
		}
	}

	/**
	 * Queries Mojang's API to get a UUID for the name, and then gets the
	 * AuthPlayer for that UUID.
	 *
	 * @param s
	 *                Name
	 * @return AuthPlayer or null if there was an error
	 */
	public static AuthPlayer getAuthPlayer(String s) {
		UUID u;
		try {
			u = RUtils.getUUID(s);
		} catch (Exception ex) {
			return null;
		}
		return AuthPlayer.getAuthPlayer(u);
	}

	/**
	 * Gets the AuthPlayer that represents a Player.
	 *
	 * @param p
	 *                Player to get AuthPlayer of
	 * @return AuthPlayer
	 */
	public static AuthPlayer getAuthPlayer(Player p) {
		return AuthPlayer.getAuthPlayer(p.getUniqueId());
	}

	/**
	 * Checks if the AP has a password set.
	 *
	 * @return true if registered, false if not
	 */
	public boolean isRegistered() {
		return this.pcm.isSet("login.password");
	}

	/**
	 * Checks if the AP has logged in.
	 *
	 * @return true if logged in, false if not
	 */
	public boolean isLoggedIn() {
		return this.pcm.getBoolean("login.logged_in");
	}

	/**
	 * Sets the AP's logged in status. In most cases, login() or logout()
	 * should be used.
	 *
	 * @param loggedIn
	 *                true if logged in, false if not
	 */
	public void setLoggedIn(final boolean loggedIn) {
		this.pcm.set("login.logged_in", loggedIn);
	}

	/**
	 * Checks if the AP is within a login session. Will return false if
	 * sessions are disabled.
	 *
	 * @return true if in a session, false if not or sessions are off
	 */
	public boolean isWithinSession() {
		if (!Config.sessionsEnabled)
			return false;
		if (this.lastLoginTimestamp <= 0L || this.lastQuitTimestamp <= 0L)
			return false;
		if (!this.isLoggedIn())
			return false;
		if (Config.sessionsCheckIP && !getCurrentIPAddress().equals(this.lastIPAddress))
			return false;
		long validUntil = Config.sessionLength * 60000L + this.lastQuitTimestamp;
		return validUntil > System.currentTimeMillis();
	}

	/**
	 * Gets the hashed password currently set for the AP.
	 *
	 * @return Hashed password
	 */
	protected String getPasswordHash() {
		return this.pcm.getString("login.password");
	}
	
	/**
	 * Gets the hash salt currently set for the AP.
	 *
	 * @return hash salt
	 */
	protected String getSalt() {
		return this.pcm.getString("login.salt");
	}
	
	/**
	 * Set the hash salt currently set for the AP.
	 *
	 * @param salt
	 */
	protected void setSalt(String salt) {
		this.pcm.set("login.salt", salt);
	}

	/**
	 * Sets the AP's password.
	 *
	 * @param newSalt
	 * @param newPasswordHash
	 *                An already encrypted password
	 * @param hashType
	 *                What type of hash was used to encrypt the password
	 *                (Java type)
	 * @return true
	 */
	public boolean setHashedPassword(String newPasswordHash,final String newSalt, final String hashType) {
		this.setSalt(newSalt);
		this.pcm.set("login.password", newPasswordHash);
		this.pcm.set("login.hash", hashType);
		return true;
	}

	/**
	 * Sets the AP's password.
	 *
	 * @param rawPassword
	 *                Unencrypted password
	 * @param hashType
	 *                What type of hash was used to encrypt the password
	 *                (Java type)
	 * @return true if password set, false if otherwise
	 */
	public boolean setPassword(String rawPassword, final String hashType) {
		String salt = RUtils.genSalt();
		try {
			rawPassword = Hasher.encrypt(rawPassword, salt, hashType);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
		this.setHashedPassword(rawPassword, salt, hashType);
		return true;
	}

	/**
	 * Check the password is right or not.
	 *
	 * @param rawPassword
	 * @return true if password right, false if not
	 */
	public boolean checkPassword(String rawpasswd) throws NoSuchAlgorithmException {
		final String hash = this.getPasswordHash();
		final String salt = this.getSalt();
		final String check;
		String hashType = (!getHashType().equalsIgnoreCase(Config.passwordHashType)) ? getHashType() : Config.passwordHashType;
		if(salt != null){
			check = Hasher.encrypt(rawpasswd, salt, hashType);
		}else{
			check = Hasher.encrypt(rawpasswd, hashType);
		}
		return check.equals(hash);
	}
	
	/**
	 * Check the password needs update to new format.
	 *
	 * @return needs update or not
	 */
	public boolean checkPwdUpdate() {
		return (this.getSalt() == null);
	}

	/**
	 * Gets the type of hash used to make the AP's password.
	 *
	 * @return Digest type
	 */
	public String getHashType() {
		return this.pcm.getString("login.hash", "mexpAUTH");
	}

	/**
	 * Gets the PConfManager of this AP.
	 *
	 * @return PConfManager
	 */
	public PConfManager getConfiguration() {
		return this.pcm;
	}

	/**
	 * Turns on the god mode for post-login if enabled in the config. Will
	 * auto-expire.
	 */
	public void enableAfterLoginGodmode() {
		if (Config.godModeAfterLogin)
			this.pcm.set("godmode_expires", System.currentTimeMillis() + Config.godModeLength * 1000L);
	}

	/**
	 * Checks if the player is in godmode from post-login godmode.
	 *
	 * @return true if in godmode, false if otherwise
	 */
	public boolean isInAfterLoginGodmode() {
		if (!Config.godModeAfterLogin)
			return false;
		final long expires = pcm.getLong("godmode_expires", 0L);
		return expires >= System.currentTimeMillis();
	}

	/**
	 * Logs an AP in. Does everything necessary to ensure a full login.
	 */
	public void login() {
		final Player p = getPlayer();
		if (p == null)
			throw new IllegalArgumentException("That player is not online!");
		this.setLoggedIn(true);
		this.setLastLoginTimestamp(System.currentTimeMillis());
		final BukkitTask reminder = this.getCurrentReminderTask();
		if (reminder != null)
			reminder.cancel();
		final PConfManager pcm = getConfiguration();
		if (Config.adventureMode) {
			if (pcm.isSet("login.gamemode")) {
				try {
					p.setGameMode(GameMode.valueOf(pcm.getString("login.gamemode", "SURVIVAL")));
				} catch (IllegalArgumentException e) {
					p.setGameMode(GameMode.SURVIVAL);
				}
			}
			pcm.set("login.gamemode", null);
		}
		if (Config.teleportToSpawn) {
			if (pcm.isSet("login.lastlocation"))
				p.teleport(pcm.getLocation("login.lastlocation"));
			pcm.set("login.lastlocation", null);
		}else{
			// not get in ground
			p.teleport(p.getLocation().add(0, 0.5, 0));
		}
		this.enableAfterLoginGodmode();
		this.setLoggedIn(true);
	}

	/**
	 * Logs a player out. This does the same as forcing a player to rejoin.
	 * <p/>
	 * This will schedule reminders for the player.
	 *
	 * @param plugin
	 *                Plugin to register reminder events under
	 */
	public void logout(Plugin plugin) {
		this.logout(plugin, true);
	}

	/**
	 * Logs a player out. This does the same as forcing a player to rejoin.
	 *
	 * @param plugin
	 *                Plugin to register reminder events under
	 * @param createReminders
	 *                If reminders should be created for the player
	 */
	public void logout(Plugin plugin, boolean createReminders) {
		final Player p = getPlayer();
		if (p == null)
			throw new IllegalArgumentException(Language.PLAYER_NOT_ONLINE.toString());
		this.setLoggedIn(false);
		if (createReminders) {
			if (this.isRegistered())
				this.createLoginReminder(plugin);
			else
				this.createRegisterReminder(plugin);
		}
		final PConfManager pcm = getConfiguration();
		if (Config.adventureMode) {
			if (!pcm.isSet("login.gamemode"))
				pcm.set("login.gamemode", p.getGameMode().name());
			p.setGameMode(GameMode.ADVENTURE);
		}
		if (Config.teleportToSpawn) {
			if (!pcm.isSet("login.lastlocation"))
				pcm.setLocation("login.lastlocation", p.getLocation());
			p.teleport(p.getLocation().getWorld().getSpawnLocation());
		}
		this.setLoggedIn(false);
	}

	/**
	 * Sets the last time that an AP logged in.
	 *
	 * @param timestamp
	 *                Time in milliseconds from epoch
	 */
	public void setLastLoginTimestamp(final long timestamp) {
		this.lastLoginTimestamp = timestamp;
		this.pcm.set("timestamps.login", timestamp);
	}

	/**
	 * Sets the last time that an AP quit.
	 *
	 * @param timestamp
	 *                Time in milliseconds from epoch
	 */
	public void setLastQuitTimestamp(final long timestamp) {
		this.lastQuitTimestamp = timestamp;
		this.pcm.set("timestamps.quit", timestamp);
	}

	/**
	 * Sets the AP's last IP address.
	 *
	 * @param ip
	 *                IP Address (IPv6 or IPv4 will work, as long as they
	 *                are consistent)
	 */
	public void setLastIPAddress(String ip) {
		this.lastIPAddress = ip.replace("/", "");
	}

	/**
	 * Updates the AP's IP address automatically
	 */
	public void updateLastIPAddress() {
		final String ip = getCurrentIPAddress();
		if (ip.isEmpty())
			return;
		this.setLastIPAddress(ip);
	}

	/**
	 * Gets the AuthPlayer's current IP address.
	 *
	 * @return IP address in String form or empty string if the player was
	 *         null
	 */
	public String getCurrentIPAddress() {
		final Player p = this.getPlayer();
		if (p == null)
			return "";
		final InetSocketAddress isa = p.getAddress();
		if (isa == null)
			return "";
		return isa.getAddress().toString().replace("/", "");
	}

	/**
	 * Gets the current task sending reminders to the AP.
	 *
	 * @return Task or null if no task
	 */
	public BukkitTask getCurrentReminderTask() {
		return this.reminderTask;
	}

	/**
	 * Creates a task to remind the AP to login.
	 *
	 * @param p
	 *                Plugin to register task under
	 * @return Task created
	 */
	public BukkitTask createLoginReminder(Plugin p) {
		this.reminderTask = RUtils.createLoginReminder(getPlayer(), p);
		return this.getCurrentReminderTask();
	}

	/**
	 * Creates a task to remind the AP to register.
	 *
	 * @param p
	 *                Plugin to register task under
	 * @return Task created
	 */
	public BukkitTask createRegisterReminder(Plugin p) {
		this.reminderTask = RUtils.createRegisterReminder(getPlayer(), p);
		return this.getCurrentReminderTask();
	}

	/**
	 * Gets the Player object represented by this AuthPlayer.
	 *
	 * @return Player or null if player not online
	 */
	public Player getPlayer() {
		return Bukkit.getPlayer(playerUUID);
	}

	/**
	 * Gets the UUID associated with this AuthPlayer.
	 *
	 * @return UUID
	 */
	public UUID getUniqueId() {
		return this.playerUUID;
	}

	/**
	 * Gets the last time this AP joined the server. If this is 0, they have
	 * never joined.
	 *
	 * @return Timestamp in milliseconds from epoch
	 */
	public long getLastJoinTimestamp() {
		return this.lastJoinTimestamp;
	}

	/**
	 * Sets the last time that an AP joined the server.
	 *
	 * @param timestamp
	 *                Time in milliseconds from epoch
	 */
	public void setLastJoinTimestamp(final long timestamp) {
		this.lastJoinTimestamp = timestamp;
		this.pcm.set("timestamps.join", timestamp);
	}

}
