package schule.ngb.carrot.util;

import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Digest {

	public static boolean md5Available() {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			return true;
		} catch( NoSuchAlgorithmException ex ) {
			return false;
		}
	}

	public static String md5( String content ) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(StandardCharsets.UTF_8.encode(content));
			return String.format("%032x", new BigInteger(1, md5.digest()));
		} catch( NoSuchAlgorithmException ex ) {
			return "";
		}
	}
	public static boolean sha1Available() {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			return true;
		} catch( NoSuchAlgorithmException ex ) {
			return false;
		}
	}

	public static String sha1( String content ) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			sha1.update(StandardCharsets.UTF_8.encode(content));
			return String.format("%032x", new BigInteger(1, sha1.digest()));
		} catch( NoSuchAlgorithmException ex ) {
			return "";
		}
	}

	public static String encodeBase64( String content ) {
		return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
	}

	public static String decodeBase64( String content ) {
		byte[] decodedBytes = Base64.getDecoder().decode(content);
		return new String(decodedBytes);
	}

	public static String encodeURL( String content ) {
		return URLEncoder.encode(content, StandardCharsets.US_ASCII);
	}

	public static String decodeURL( String content ) {
		return URLDecoder.decode(content, StandardCharsets.US_ASCII);
	}

}
