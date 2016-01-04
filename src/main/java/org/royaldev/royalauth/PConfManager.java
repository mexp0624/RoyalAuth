package org.royaldev.royalauth;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PConfManager extends YamlConfiguration {

	private static final Map<UUID, PConfManager> pcms = new HashMap<>();
	private final Object saveLock = new Object();
	private File pconfl = null;

	/**
	 * Player configuration manager
	 *
	 * @param p
	 *                Player to manage
	 */
	PConfManager(OfflinePlayer p) {
		super();
		File dataFolder = RoyalAuth.dataFolder;
		this.pconfl = new File(
				dataFolder + File.separator + "userdata" + File.separator + p.getUniqueId() + ".yml");
		try {
			load(this.pconfl);
		} catch (Exception ignored) {
		}
	}

	/**
	 * Player configuration manager.
	 *
	 * @param u
	 *                Player to manage
	 */
	PConfManager(UUID u) {
		super();
		File dataFolder = RoyalAuth.dataFolder;
		this.pconfl = new File(dataFolder + File.separator + "userdata" + File.separator + u + ".yml");
		try {
			load(this.pconfl);
		} catch (Exception ignored) {
		}
	}

	/**
	 * No outside construction, please.
	 */
	// @SuppressWarnings("unused")
	PConfManager() {
	}

	public static PConfManager getPConfManager(Player p) {
		return PConfManager.getPConfManager(p.getUniqueId());
	}

	public static PConfManager getPConfManager(UUID u) {
		synchronized (PConfManager.pcms) {
			if (PConfManager.pcms.containsKey(u))
				return PConfManager.pcms.get(u);
			final PConfManager pcm = new PConfManager(u);
			PConfManager.pcms.put(u, pcm);
			return pcm;
		}
	}

	public static void saveAllManagers() {
		synchronized (PConfManager.pcms) {
			for (PConfManager pcm : PConfManager.pcms.values())
				pcm.forceSave();
		}
	}

	public static void purge() {
		synchronized (PConfManager.pcms) {
			PConfManager.pcms.clear();
		}
	}

	public boolean exists() {
		return this.pconfl.exists();
	}

	public boolean createFile() {
		try {
			return this.pconfl.createNewFile();
		} catch (IOException ignored) {
			return false;
		}
	}

	public void forceSave() {
		synchronized (this.saveLock) {
			try {
				save(this.pconfl);
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * Gets a Location from config
	 * <p/>
	 * This <strong>will</strong> throw an exception if the saved Location
	 * is invalid or missing parts.
	 *
	 * @param path
	 *                Path in the yml to fetch from
	 * @return Location or null if path does not exist or if config doesn't
	 *         exist
	 */
	public Location getLocation(String path) {
		if (this.get(path) == null)
			return null;
		String world = this.getString(path + ".w");
		double x = this.getDouble(path + ".x");
		double y = this.getDouble(path + ".y") + 0.5f;
		double z = this.getDouble(path + ".z");
		float pitch = this.getFloat(path + ".pitch");
		float yaw = this.getFloat(path + ".yaw");
		return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
	}

	/**
	 * Sets a location in config
	 *
	 * @param value
	 *                Location to set
	 * @param path
	 *                Path in the yml to set
	 */
	public void setLocation(String path, Location value) {
		this.set(path + ".w", value.getWorld().getName());
		this.set(path + ".x", value.getX());
		this.set(path + ".y", value.getY());
		this.set(path + ".z", value.getZ());
		this.set(path + ".pitch", value.getPitch());
		this.set(path + ".yaw", value.getYaw());
	}

	public float getFloat(String path) {
		return (float) this.getDouble(path);
	}
}
