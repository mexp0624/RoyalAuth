package org.royaldev.royalauth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

	private static String getType(String type) {
		type = type.trim();
		if (type.equalsIgnoreCase("md5"))
			return "MD5";
		else if (type.equalsIgnoreCase("sha-512") || type.equalsIgnoreCase("sha512"))
			return "SHA-512";
		else if (type.equalsIgnoreCase("sha-256") || type.equalsIgnoreCase("sha256"))
			return "SHA-256";
		else if (type.equalsIgnoreCase("rauth"))
			return "RAUTH";
		else if (type.equalsIgnoreCase("mexpauth"))
			return "MAUTH";
		else
			return type;
	}

	private static String hash(String data, String type) throws NoSuchAlgorithmException {
		String rtype = Hasher.getType(type);
		if (rtype.equals("RAUTH")) {
			rtype = "SHA-512";
		}
		MessageDigest md = MessageDigest.getInstance(rtype);
		md.update(data.getBytes());
		byte byteData[] = md.digest();
		StringBuilder sb = new StringBuilder();
		for (byte aByteData : byteData)
			sb.append(Integer.toString((aByteData & 0xFF) + 0x100, 16).substring(1));
		return sb.toString();
	}

	public static String encrypt(String data, String type) throws NoSuchAlgorithmException {
		final String rtype = Hasher.getType(type);
		if (rtype.equals("RAUTH")) {
			for (int i = 0; i < 25; i++)
				data = Hasher.hash(data, rtype);
			return data;
		} else if(rtype.equals("MAUTH")) {
			data = Hasher.hash(data, "SHA-512") + Hasher.hash(data, "MD5") ;
			return data;
		} else {
			return Hasher.hash(data, rtype);
		}
	}
	
	public static String encrypt(String data, String salt, String type) throws NoSuchAlgorithmException {
		return Hasher.encrypt(salt + ":" + data,  type);
	}
}
