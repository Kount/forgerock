package com.kount.authnode;

public enum CoseAlgorithm {

    /** the es256 algorithm. */
    ES256("ES256", -7, "SHA256withECDSA", KeyType.EC, "SHA-256"),

    /** the es512 algorithm. */
    ES512("ES512", -36, "SHA512withECDSA", KeyType.EC, "SHA-512"),

    /** the es384 algorithm. */
    ES384("ES384", -35, "SHA384withECDSA", KeyType.EC, "SHA-384"),

    /** the RS1 algorithm. */
    RS1("RS1", -65535, "SHA1withRSA", KeyType.RSA, "SHA-1"),

    /** the RS256 algorithm. */
    RS256("RS256", -257, "SHA256withRSA", KeyType.RSA, "SHA-256"),

    /** the RS384 algorithm. */
    RS384("RS384", -258, "SHA384withRSA", KeyType.RSA, "SHA-384"),

    /** the RS512 algorithm. */
    RS512("RS512", -259, "SHA512withRSA", KeyType.RSA, "SHA-512");

    private String name;
    private int coseNumber;
    private String exactAlgorithmName;
    private KeyType keyType;
    private String hashAlg;

    /**
     * The constructor.
     *
     * @param name the algorithm name.
     * @param coseNumber the value representing the algorithm.
     * @param exactAlgorithmName the name used to reference the algorithm internally.
     * @param keyType the key type to use with this algorithm.
     * @param hashAlg the exact name of the hashing algorithm used.
     */
    CoseAlgorithm(String name, int coseNumber, String exactAlgorithmName, KeyType keyType, String hashAlg) {
        this.name = name;
        this.coseNumber = coseNumber;
        this.exactAlgorithmName = exactAlgorithmName;
        this.keyType = keyType;
        this.hashAlg = hashAlg;
    }

    /**
     * Get the algorithm from the magic int value.
     *
     * @param coseNumber the int value of the algorithm defined by COSE.
     * @return the enum representing the algorithm.
     */
    public static CoseAlgorithm fromCoseNumber(int coseNumber) {
        for (CoseAlgorithm coseAlgorithm : CoseAlgorithm.values()) {
            if (coseAlgorithm.coseNumber == coseNumber) {
                return coseAlgorithm;
            }
        }
        return null;
    }

    /**
     * Get the name of the algorithm.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the numerical value representing the algorithm.
     *
     * @return the value.
     */
    public int getCoseNumber() {
        return coseNumber;
    }

    /**
     * Get the exact algorithm name used by security providers.
     *
     * @return the algorithm name.
     */
    public String getExactAlgorithmName() {
        return exactAlgorithmName;
    }

    /**
     * Get the exact name of the hashing algorithm used.
     *
     * @return the hashing algorithm used within this algorithm.
     */
    public String getHashAlg() {
        return hashAlg;
    }

    /**
     * Get the key type used with this algorithm.
     *
     * @return the key type used for this algorithm.
     */
    public KeyType getKeyType() {
        return keyType;
    }

    @Override
    public String toString() {
        return exactAlgorithmName;
    }
}