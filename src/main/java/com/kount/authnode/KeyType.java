package com.kount.authnode;
import java.util.Set;
import org.forgerock.json.JsonException;
public enum KeyType {
    /**
     * RSA key.
     */
    RSA("RSA", Set.of("e", "kty", "n")),

    /**
     * Elliptical Curve Key.
     */
    EC("EC", Set.of("crv", "kty", "x", "y")),

    /**
     * Octet Key.
     */
    OCT("oct", Set.of("k", "kty")),

    /**
     * Octet key-pair.
     */
    OKP("OKP", Set.of("crv", "kty", "x"));

    /**
     * The value of the KeyType.
     */
    private final String value;
    private final Set<String> requiredFields;

    /**
     * Construct a KeyType.
     * @param value value to give that keytype
     * @param requiredFields the set of required fields for this key type.
     */
    KeyType(String value, Set<String> requiredFields) {
        this.value = value;
        this.requiredFields = requiredFields;
    }

    /**
     * Get the value of the KeyType.
     * @return the value of the KeyType
     */
    public String value() {
        return toString();
    }

    /**
     * The minimum set of fields that are required for a JWK of this type.
     *
     * @return the minimum set of fields for this type of JWK.
     */
    public Set<String> getRequiredFields() {
        return requiredFields;
    }

    /**
     * Get the KeyType given a string.
     * @param keyType string representing the KeyType
     * @return a KeyType or null if given null KeyType
     */
    public static KeyType getKeyType(String keyType) {
        if (keyType == null || keyType.isEmpty()) {
            return null;
        }
        try {
            return KeyType.valueOf(keyType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JsonException("Invalid key type");
        }
    }

    /**
     * Gets the value of the KeyType.
     * @return value of the KeyType
     */
    @Override
    public String toString() {
        return value;
    }
}
