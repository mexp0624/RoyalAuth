package org.royaldev.royalauth;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConnectionManager {

    private String user;
    private String pass;
    private String database;
    private String prefix;
    private String address;
    private int port;

    private int pingerID = -1;
    private int connecID = -1;

    private final List<String> loggedin = new ArrayList<String>();

    private Connection c = null;

    private boolean offline = false;

    /**
     * Closes all old connections to a database and removes the pinger.
     *
     * @throws SQLException If a close function errors
     */
    private void endOldConnections() throws SQLException {
        if (c != null) c.close();

        if (pingerID > -1)
            RoyalAuth.instance.getServer().getScheduler().cancelTask(pingerID);
    }

    /**
     * Starts connections and makes the pinger.
     *
     * @throws SQLException
     */
    private void initiateConnection() throws SQLException {
        endOldConnections();

        String url = "jdbc:mysql://" + address + ":" + port + "/" + database;

        try {
            c = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            if (offline) {
                RoyalAuth.instance.getLogger().severe("Could not reestablish connection to MySQL. Will try every five seconds.");
                if (connecID == -1) connecID = startConnectionMaker();
                return;
            } else throw e;
        }
        if (!c.isClosed()) pingerID = makePinger();
        else pingerID = -1;
        if (pingerID < 0)
            RoyalAuth.instance.getLogger().warning("Could not start MySQL pinger!");
        if (offline && isConnected()) {
            if (connecID > -1) Bukkit.getScheduler().cancelTask(connecID);
            RoyalAuth.instance.getLogger().info("Reestablished connection to MySQL successfully.");
            offline = false;
        }
    }

    private int startConnectionMaker() {
        RoyalAuth plugin = RoyalAuth.instance;

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    initiateConnection();
                } catch (SQLException ignored) {
                }
            }
        };

        return plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, r, 0, 100);
    }

    /**
     * Makes the pinger to keep the connections alive. Pings every 15 seconds.
     * <p/>
     * The pinger automatically tries to reconnect if the connection is lost.
     *
     * @return Task ID of the pinger, or -1 if it could not start
     */
    private int makePinger() {
        RoyalAuth plugin = RoyalAuth.instance;
        final PreparedStatement s;
        try {
            s = c.prepareStatement("SELECT 1;");
        } catch (SQLException e) {
            return -1;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (c.isClosed()) {
                        RoyalAuth.instance.getLogger().severe("Lost connection to database. Attempting to restore.");
                        offline = true;
                        initiateConnection();
                        return;
                    }
                    s.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };

        return plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, r, 0, 300);
    }

    /**
     * Makes the default tables in the database for use by plugin
     *
     * @return true if all statements worked, false if not
     * @throws SQLException
     */
    private boolean makeDefaultTables() throws SQLException {
        Statement stmt = c.createStatement();
        boolean use = stmt.execute("USE `" + database + "`;");
        boolean users = stmt.execute("CREATE TABLE IF NOT EXISTS `" + prefix + "users` (name text, password text, date long, ip text);");
        boolean locs = stmt.execute("CREATE TABLE IF NOT EXISTS `" + prefix + "locations` (name text, world text, x int, y int, z int, pitch float, yaw float);");
        return use && users && locs;
    }

    /**
     * MySQL connection manager for rAuth.
     * <p/>
     * Making a new ConnectionManager starts connections and registers a pinger.
     *
     * @param username     Username for server
     * @param password     Password of user
     * @param databasename Database name to use
     * @param prefix       Prefix on tables
     * @param address      Address of the server
     * @param port         Port of the server
     */
    public ConnectionManager(String username, String password, String databasename, String prefix, String address, int port) {
        user = username;
        pass = password;
        database = databasename;
        this.prefix = prefix;
        this.address = address;
        this.port = port;
        try {
            initiateConnection();
        } catch (SQLException e) {
            RoyalAuth.instance.getLogger().severe("Could not connect to MySQL database: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            makeDefaultTables();
        } catch (SQLException e) {
            RoyalAuth.instance.getLogger().severe("Could not make default tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks if the connection is alive.
     *
     * @return true if connected, false if otherwise
     */
    public boolean isConnected() {
        return c != null;
    }

    /**
     * Checks to see if a player is registered
     *
     * @param name Name of player
     * @return true if registered, false if not
     */
    public boolean isRegistered(String name) {
        try {
            PreparedStatement s = c.prepareStatement("SELECT 1 FROM `" + prefix + "users` WHERE `name` = ?;");
            s.setString(1, name);
            ResultSet rs = s.executeQuery();
            rs.last();
            return rs.getRow() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Registers a player
     *
     * @param name     Name of player
     * @param password Password of player in plaintext - will be encrypted
     * @param date     Date registered
     * @param ip       IP of player
     * @return true if registered, false if otherwise
     */
    public boolean register(final String name, final String password, final long date, final String ip) {
        String encPass;
        try {
            encPass = RASha.encrypt(password, "RAUTH");
        } catch (NoSuchAlgorithmException e) {
            RoyalAuth.instance.getLogger().severe("Invalid encryption method specified in config: " + e.getMessage());
            return false;
        }
        try {
            PreparedStatement s = c.prepareStatement("INSERT INTO `" + prefix + "users` VALUES (?, ?, ?, ?);");
            s.setString(1, name);
            s.setString(2, encPass);
            s.setLong(3, date);
            s.setString(4, ip);
            s.execute();
            synchronized (loggedin) {
                loggedin.add(name);
            }
        } catch (SQLException e) {
            RoyalAuth.instance.log.warning("Could not register " + name + ": " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Checks to see if a player is logged in.
     *
     * @param name Name of player
     * @return true if logged in, false if otherwise
     */
    public boolean isLoggedIn(String name) {
        synchronized (loggedin) {
            return loggedin.contains(name);
        }
    }

    /**
     * Sets the location the player will be sent to when they are logged in.
     * <p/>
     * This should only be run if the player has no location. Use
     * {@link #updateLocation(String, org.bukkit.Location)} instead.
     *
     * @param name Name of player
     * @param loc  Location for the player to be sent to
     * @return true if location was set, false if not
     */
    public boolean setLocation(String name, Location loc) {
        try {
            PreparedStatement s = c.prepareStatement("INSERT INTO `" + prefix + "locations` VALUES (?, ?, ?, ?, ?, ?, ?);");
            s.setString(1, name);
            s.setString(2, loc.getWorld().getName());
            s.setDouble(3, loc.getX());
            s.setDouble(4, loc.getY());
            s.setDouble(5, loc.getZ());
            s.setFloat(6, loc.getPitch());
            s.setFloat(7, loc.getYaw());
            return s.execute();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates the location a player will be sent to upon login.
     *
     * @param name Name of player
     * @param loc  New location for the player to be sent to
     * @return true if location was updated, false if not
     */
    public boolean updateLocation(String name, Location loc) {
        try {
            PreparedStatement s = c.prepareStatement("SELECT 1 FROM `" + prefix + "locations` WHERE name = ?;");
            s.setString(1, name);
            ResultSet rs = s.executeQuery();
            if (!rs.last()) return setLocation(name, loc);
            s = c.prepareStatement("UPDATE `" + prefix + "locations` SET world = ?, x = ?, y = ?, z = ?, pitch = ?, yaw = ? WHERE name = ?;");
            s.setString(1, loc.getWorld().getName());
            s.setDouble(2, loc.getX());
            s.setDouble(3, loc.getY());
            s.setDouble(4, loc.getZ());
            s.setFloat(5, loc.getPitch());
            s.setFloat(6, loc.getYaw());
            s.setString(7, name);
            s.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Attempts to log in a player
     *
     * @param name     Name of player to log in
     * @param password Password of player
     * @return true if logged in, false if otherwise (invalid password), null if config error
     */
    public Boolean attemptLogin(String name, String password) {
        if (!isRegistered(name)) return false;
        try {
            password = RASha.encrypt(password, "RAUTH");
        } catch (NoSuchAlgorithmException e) {
            RoyalAuth.instance.getLogger().severe("Invalid encryption method specified in config: " + e.getMessage());
            return null;
        }
        try {
            PreparedStatement s = c.prepareStatement("SELECT 1 FROM `" + prefix + "users` WHERE name = ? AND password = ?;");
            s.setString(1, name);
            s.setString(2, password);
            ResultSet rs = s.executeQuery();
            rs.last();
            boolean exists = rs.getRow() > 0;
            if (exists) synchronized (loggedin) {
                loggedin.add(name);
            }
            return exists;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Forcibly adds a player to the logged in list - used in sessions.
     * <p/>
     * This method does not require a password.
     *
     * @param name Name of player to log in
     */
    public void forceLogin(String name) {
        synchronized (loggedin) {
            if (!loggedin.contains(name)) loggedin.add(name);
        }
        if (RoyalAuth.instance.teleToSpawn) {
            Location l = getLocation(name);
            Player p = Bukkit.getPlayer(name);
            if (l == null || p == null) return;
            p.teleport(l);
        }
    }

    /**
     * Logs a player out
     *
     * @param name Name of player to log out
     * @return true if logged out, false if wasn't logged in
     */
    public boolean logOut(String name) {
        synchronized (loggedin) {
            if (loggedin.contains(name)) loggedin.remove(name);
            else return false;
        }
        Player p = Bukkit.getPlayer(name);
        if (p == null) return true;
        if (RoyalAuth.instance.teleToSpawn) p.teleport(p.getWorld().getSpawnLocation());
        return true;
    }

    /**
     * Updates the date column in the user database.
     * <p/>
     * Date is used for sessions - it records the last logout.
     *
     * @param name Name of player to update for
     * @param date New date to insert
     * @return true if success, false if otherwise
     */
    public boolean updateDate(String name, long date) {
        try {
            PreparedStatement s = c.prepareStatement("UPDATE `" + prefix + "users` SET date=? WHERE name=?;");
            s.setLong(1, date);
            s.setString(2, name);
            return s.execute();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the location a player will be sent to upon login.
     *
     * @param name Name of player to get location for
     * @return The location the player will be sent to or null
     */
    public Location getLocation(String name) {
        try {
            PreparedStatement s = c.prepareStatement("SELECT x, y, z, pitch, yaw, world FROM `" + prefix + "locations` WHERE name = ?;");
            s.setString(1, name);
            ResultSet r = s.executeQuery();
            boolean success = r.absolute(1);
            if (!success) return null;
            String world = r.getString("world");
            int x = r.getInt("x");
            int y = r.getInt("y");
            int z = r.getInt("z");
            float pitch = r.getFloat("pitch");
            float yaw = r.getFloat("yaw");
            World w = Bukkit.getServer().getWorld(world);
            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if the player has a valid session.
     *
     * @param p      Player to check for
     * @param length Length of session (in seconds)
     * @return true if the session is valid, false if not
     */
    public boolean isSessionValid(Player p, long length) {
        if (c == null) return false;
        length = length * 1000;
        long dates;
        String ip;
        try {
            PreparedStatement s = c.prepareStatement("SELECT date, ip FROM `" + prefix + "users` WHERE name = ?;");
            s.setString(1, p.getName());
            ResultSet rs = s.executeQuery();
            boolean success = rs.absolute(1);
            if (!success) return false;
            dates = rs.getLong("date");
            ip = rs.getString("ip");
        } catch (Exception e) {
            return false;
        }
        Date sDate = new Date(dates); // last time player logged out
        Date cDate = new Date();
        String playerip = p.getAddress().getAddress().toString().replace("/", "");
        return !(RoyalAuth.instance.sessionCheckIP && !playerip.equals(ip)) && sDate.getTime() + length >= cDate.getTime();
    }

    /**
     * Changes a player's password in the database. This method does not require the original password.
     * <p/>
     * Will retur false if not registered.
     *
     * @param name    Name of player to update for
     * @param newPass New password in plaintext - will be encrypted
     * @return true if success, false if otherwise
     */
    public boolean changePassword(String name, String newPass) {
        String encPass;
        try {
            encPass = RASha.encrypt(newPass, "RAUTH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        try {
            PreparedStatement s = c.prepareStatement("UPDATE `" + prefix + "users` SET `password` = ? WHERE `name` = ?;");
            s.setString(1, encPass);
            s.setString(2, name);
            s.execute();
            s = c.prepareStatement("SELECT `password` FROM `" + prefix + "users` WHERE `name` = ?;");
            s.setString(1, name);
            ResultSet rs = s.executeQuery();
            rs.last();
            return rs.getString("password").equals(encPass);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Changes a player's password in the database. This method requires the original password.
     * <p/>
     * Will retur false if not registered.
     *
     * @param name    Name of player to update for
     * @param oldPass Old password in plaintext - will be encrypted
     * @param newPass New password in plaintext - will be encrypted
     * @return true if success, false if otherwise (wrong password)
     */
    public boolean changePassword(String name, String oldPass, String newPass) {
        String newEncPass;
        String oldEncPass;
        try {
            newEncPass = RASha.encrypt(newPass, "RAUTH");
            oldEncPass = RASha.encrypt(oldPass, "RAUTH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
        try {
            PreparedStatement s = c.prepareStatement("UPDATE `" + prefix + "users` SET `password` = ? WHERE `name` = ? AND `password` = ?;");
            s.setString(1, newEncPass);
            s.setString(2, name);
            s.setString(3, oldEncPass);
            s.execute();
            s = c.prepareStatement("SELECT `password` FROM `" + prefix + "users` WHERE `name` = ?;");
            s.setString(1, name);
            ResultSet rs = s.executeQuery();
            rs.last();
            return rs.getString("password").equals(newEncPass);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
