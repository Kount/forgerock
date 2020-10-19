package com.kount.authnode;

import java.util.Objects;

import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonIgnore;

//TODO Why are WebAuthNDevice Settings in this repository?
public class WebAuthnDeviceSettings extends DeviceSettings {

    private String credentialId;
    private String algorithm;
    private String deviceName;

    @JsonIgnore
    private JWK key;

    /**
     * Empty no-arg constructor for Jackson usage, due to presence of non-default constructor.
     */
    public WebAuthnDeviceSettings() {
        //This section intentionally left blank.
    }

    /**
     * Construct a new WebAuthnDeviceSettings object with the provided values.
     *
     * @param credentialId The authenticator's identifier.
     * @param key The public key associated with the identifier.
     * @param algorithm The alg this credential uses.
     * @param name User-friendly name of this registered device.
     */
    WebAuthnDeviceSettings(String credentialId, JWK key, String algorithm, String name) {
        super();
        setCredentialId(credentialId);
        setKey(key);
        setAlgorithm(algorithm);
        setDeviceName(name);
    }

    private void setDeviceName(String name) {
        this.deviceName = name;
    }

    private void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Get the user-friend name for this credential.
     *
     * @return the name of this credential.
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Get the algorithm to use for this credential.
     *
     * @return the algorithm name
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the credential ID used to reference the user's stored authentication key.
     *
     * @param credentialId The communication id. Can not be null.
     */
    private void setCredentialId(String credentialId) {
        Reject.ifTrue(StringUtils.isBlank(credentialId), "credentialId can not be null.");
        this.credentialId = credentialId;
    }

    /**
     * The name of the issuer when this device profile was issued.
     *
     * @param key The name of the issuer.
     */
    public void setKey(JWK key) {
        Reject.ifNull(key, "Key can not be null");
        this.key = key;
    }

    /**
     * Get the public key for the WebAuthn device.
     *
     * @return The identifier.
     */
    public JWK getKey() {
        return key;
    }

    /**
     * Get the credential Id for the WebAuthn device.
     *
     * @return The credential Id.
     */
    public String getCredentialId() {
        return credentialId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WebAuthnDeviceSettings that = (WebAuthnDeviceSettings) o;

        return Objects.equals(credentialId, that.credentialId)
                && Objects.equals(key, that.key)
                && Objects.equals(uuid, that.uuid)
                && Objects.equals(algorithm, that.algorithm)
                && Objects.equals(deviceName, that.deviceName)
                && Objects.equals(recoveryCodes, that.recoveryCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credentialId, key, uuid, algorithm, deviceName, recoveryCodes);
    }

    @Override
    public String toString() {
        return "WebAuthnDeviceSettings {"
                + "UUID='" + uuid + '\''
                + ", deviceName='" + deviceName + '\''
                + ", credentialId='" + credentialId + '\''
                + ", algorithm='" + algorithm + '\''
                + '}';
    }
}
