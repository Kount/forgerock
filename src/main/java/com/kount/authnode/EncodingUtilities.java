package com.kount.authnode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO Why is EncodingUtilities in this repository?
public class EncodingUtilities {

    private static final Logger logger = LoggerFactory.getLogger(EncodingUtilities.class);

    private EncodingUtilities() { }

    /**
     * Decode a Base64URL encoded string.
     *
     * @param encodedValue the base64URL encoded value.
     * @return the decoded value.
     */
    public static byte[] base64UrlDecode(String encodedValue) {
        return Base64url.decode(encodedValue);
    }

    /**
     * Return the SHA-256 hash of the string value.
     *
     * @param value a string value.
     * @return the hash of the string value.
     */
    public static byte[] getHash(String value) {
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            logger.error("failed to perform a SHA-256 hash", e);
        }
        return hash;
    }

    /**
     * Returns a base64 URL encoded hash of the String input.
     * @param input the input string.
     * @return the base64 URL hashed string value.
     */
    public static String base64UrlEncode(String input) {
        return Base64url.encode(input.getBytes());
    }
}
