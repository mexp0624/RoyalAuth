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

    private final static Map<UUID, AuthPlayer> authPlayers = new HashMap<UUID, AuthPlayer>();
    private final PConfManager pcm;
    private UUID playerUUID;
    private String lastIPAddress;
    private long lastJoinTimestamp = 0L;
    private long lastLoginTimestamp = 0L;
    private long lastQuitTimestamp = 0L;
    private BukkitTask reminderTask = null;

    private AuthPlayer(UUID u) {
        playerUUID = u;
        pcm = PConfManager.getPConfManager(u);
        lastJoinTimestamp = pcm.getLong("timestamps.join", 0L);
        lastLoginTimestamp = pcm.getLong("timestamps.login", 0L);
        lastQuitTimestamp = pcm.getLong("timestamps.quit", 0L);
    }

    private AuthPlayer(Player p) {
        this(p.getUniqueId());
    }

    /**
     * Gets the AuthPlayer for the name of a player.
     *
     * @param u UUID to get AuthPlayer of
     * @return AuthPlayer
     */
    public static AuthPlayer getAuthPlayer(UUID u) {
        synchronized (authPlayers) {
            if (authPlayers.containsKey(u)) return authPlayers.get(u);
            final AuthPlayer ap = new AuthPlayer(u);
            authPlayers.put(u, ap);
            return ap;
        }
    }

    /**
     * Queries Mojang's API to get a UUID for the name, and then gets the AuthPlayer for that UUID.
     *
     * @param s Name
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
     * @param p Player to get AuthPlayer of
     * @return AuthPlayer
     */
    public static AuthPlayer getAuthPlayer(Player p) {
        return getAuthPlayer(p.getUniqueId());
    }

    /**
     * Checks if the AP has a password set.
     *
     * @return true if registered, false if not
     */
    public boolean isRegistered() {
        return pcm.isSet("login.password");
    }

    /**
     * Checks if the AP has logged in.
     *
     * @return true if logged in, false if not
     */
    public boolean isLoggedIn() {
        return pcm.getBoolean("login.logged_in");
    }

    /**
     * Sets the AP's logged in status. In most cases, login() or logout() should be used.
     *
     * @param loggedIn true if logged in, false if not
     */
    public void setLoggedIn(final boolean loggedIn) {
        pcm.set("login.logged_in", loggedIn);
    }

    /**
     * Checks if the AP is within a login session. Will return false if sessions are disabled.
     *
     * @return true if in a session, false if not or sessions are off
     */
    public boolean isWithinSession() {
        if (!Config.sessionsEnabled) return false;
        if (lastLoginTimestamp <= 0L || lastQuitTimestamp <= 0L) return false;
        if (!isLoggedIn()) return false;
        if (Config.sessionsCheckIP && !getCurrentIPAddress().equals(lastIPAddress)) return false;
        long validUntil = Config.sessionLength * 60000L + lastQuitTimestamp;
        return validUntil > System.currentTimeMillis();
    }

    /**
     * Changes the player's password. Requires the old password for security verification.
     *
     * @param hashedPassword    The password hash of the new password.
     * @param oldHashedPassword The password hash of the old password.
     * @return true if password changed, false if not
     */
    public boolean setHashedPassword(String hashedPassword, String oldHashedPassword, final String hashType) {
        if (!getPasswordHash().equals(oldHashedPassword)) return false;
        pcm.set("login.password", hashedPassword);
        pcm.set("login.hash", hashType.toUpperCase());
        return true;
    }

    /**
     * Changes the player's password. Requires the old password for security verification.
     *
     * @param rawPassword    Plaintext new password
     * @param rawOldPassword Plaintext old password
     * @param hashType       Hashtypes to be used on these passwords
     * @return true if password changed, false if not
     */
    public boolean setPassword(String rawPassword, String rawOldPassword, final String hashType) {
        String oldPasswordHash = (!getHashType().equalsIgnoreCase(hashType)) ? getHashType() : hashType;
        try {
            rawPassword = Hasher.encrypt(rawPassword, hashType);
            rawOldPassword = Hasher.encrypt(rawOldPassword, oldPasswordHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        return setHashedPassword(rawPassword, rawOldPassword, hashType);
    }

    /**
     * Gets the hashed password currently set for the AP.
     *
     * @return Hashed password
     */
    public String getPasswordHash() {
        return pcm.getString("login.password");
    }

    /**
     * Gets the type of hash used to make the AP's password.
     *
     * @return Digest type
     */
    public String getHashType() {
        return pcm.getString("login.hash", "RAUTH");
    }

    /**
     * Gets the PConfManager of this AP.
     *
     * @return PConfManager
     */
    public PConfManager getConfiguration() {
        return pcm;
    }

    /**
     * Turns on the god mode for post-login if enabled in the config. Will auto-expire.
     */
    public void enableAfterLoginGodmode() {
        if (Config.godModeAfterLogin)
            pcm.set("godmode_expires", System.currentTimeMillis() + Config.godModeLength * 1000L);
    }

    /**
     * Checks if the player is in godmode from post-login godmode.
     *
     * @return true if in godmode, false if otherwise
     */
    public boolean isInAfterLoginGodmode() {
        if (!Config.godModeAfterLogin) return false;
        long expires = pcm.getLong("godmode_expires", 0L);
        return expires >= System.currentTimeMillis();
    }

    /**
     * Logs an AP in. Does everything necessary to ensure a full login.
     */
    public void login() {
        Player p = getPlayer();
        if (p == null) throw new IllegalArgumentException("That player is not online!");
        setLoggedIn(true);
        setLastLoginTimestamp(System.currentTimeMillis());
        BukkitTask reminder = getCurrentReminderTask();
        if (reminder != null) reminder.cancel();
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
            if (pcm.isSet("login.lastlocation")) p.teleport(pcm.getLocation("login.lastlocation"));
            pcm.set("login.lastlocation", null);
        }
        enableAfterLoginGodmode();
        setLoggedIn(true);
    }

    /**
     * Logs a player out. This does the same as forcing a player to rejoin.
     * <p/>
     * This will schedule reminders for the player.
     *
     * @param plugin Plugin to register reminder events under
     */
    public void logout(Plugin plugin) {
        logout(plugin, true);
    }

    /**
     * Logs a player out. This does the same as forcing a player to rejoin.
     *
     * @param plugin          Plugin to register reminder events under
     * @param createReminders If reminders should be created for the player
     */
    public void logout(Plugin plugin, boolean createReminders) {
        Player p = getPlayer();
        if (p == null) throw new IllegalArgumentException(Language.PLAYER_NOT_ONLINE.toString());
        setLoggedIn(false);
        if (createReminders) {
            if (isRegistered()) createLoginReminder(plugin);
            else createRegisterReminder(plugin);
        }
        final PConfManager pcm = getConfiguration();
        if (Config.adventureMode) {
            if (!pcm.isSet("login.gamemode")) pcm.set("login.gamemode", p.getGameMode().name());
            p.setGameMode(GameMode.ADVENTURE);
        }
        if (Config.teleportToSpawn) {
            if (!pcm.isSet("login.lastlocation")) pcm.setLocation("login.lastlocation", p.getLocation());
            p.teleport(p.getLocation().getWorld().getSpawnLocation());
        }
        setLoggedIn(false);
    }

    /**
     * Sets the last time that an AP logged in.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastLoginTimestamp(final long timestamp) {
        lastLoginTimestamp = timestamp;
        pcm.set("timestamps.login", timestamp);
    }

    /**
     * Sets the last time that an AP quit.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastQuitTimestamp(final long timestamp) {
        lastQuitTimestamp = timestamp;
        pcm.set("timestamps.quit", timestamp);
    }

    /**
     * Sets the AP's password.
     *
     * @param newPasswordHash An already encrypted password
     * @param hashType        What type of hash was used to encrypt the password (Java type)
     * @return true
     */
    public boolean setHashedPassword(String newPasswordHash, final String hashType) {
        pcm.set("login.password", newPasswordHash);
        pcm.set("login.hash", hashType);
        return true;
    }

    /**
     * Sets the AP's password.
     *
     * @param rawPassword Unencrypted password
     * @param hashType    What type of hash was used to encrypt the password (Java type)
     * @return true if password set, false if otherwise
     */
    public boolean setPassword(String rawPassword, final String hashType) {
        try {
            rawPassword = Hasher.encrypt(rawPassword, hashType);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        pcm.set("login.password", rawPassword);
        pcm.set("login.hash", hashType);
        return true;
    }

    /**
     * Sets the AP's last IP address.
     *
     * @param ip IP Address (IPv6 or IPv4 will work, as long as they are consistent)
     */
    public void setLastIPAddress(String ip) {
        lastIPAddress = ip.replace("/", "");
    }

    /**
     * Updates the AP's IP address automatically
     */
    public void updateLastIPAddress() {
        String ip = getCurrentIPAddress();
        if (ip.isEmpty()) return;
        setLastIPAddress(ip);
    }

    /**
     * Gets the AuthPlayer's current IP address.
     *
     * @return IP address in String form or empty string if the player was null
     */
    public String getCurrentIPAddress() {
        Player p = getPlayer();
        if (p == null) return "";
        InetSocketAddress isa = p.getAddress();
        if (isa == null) return "";
        return isa.getAddress().toString().replace("/", "");
    }

    /**
     * Gets the current task sending reminders to the AP.
     *
     * @return Task or null if no task
     */
    public BukkitTask getCurrentReminderTask() {
        return reminderTask;
    }

    /**
     * Creates a task to remind the AP to login.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createLoginReminder(Plugin p) {
        reminderTask = RUtils.createLoginReminder(getPlayer(), p);
        return getCurrentReminderTask();
    }

    /**
     * Creates a task to remind the AP to register.
     *
     * @param p Plugin to register task under
     * @return Task created
     */
    public BukkitTask createRegisterReminder(Plugin p) {
        reminderTask = RUtils.createRegisterReminder(getPlayer(), p);
        return getCurrentReminderTask();
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
     * Gets the last time this AP joined the server. If this is 0, they have never joined.
     *
     * @return Timestamp in milliseconds from epoch
     */
    public long getLastJoinTimestamp() {
        return lastJoinTimestamp;
    }

    /**
     * Sets the last time that an AP joined the server.
     *
     * @param timestamp Time in milliseconds from epoch
     */
    public void setLastJoinTimestamp(final long timestamp) {
        lastJoinTimestamp = timestamp;
        pcm.set("timestamps.join", timestamp);
    }

}
