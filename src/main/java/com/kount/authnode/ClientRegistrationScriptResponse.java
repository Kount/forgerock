package com.kount.authnode;

//TODO Why is ClientRegistrationScriptResponse in this repository?

public class ClientRegistrationScriptResponse extends ClientScriptResponse {

    private final byte[] attestationData;

    /**
     * Constructor for registration script data.
     *
     * @param clientData the clientData from the device
     * @param attestationData the attestationData from the device
     * @param credentialId the identifier of this credential
     */
    public ClientRegistrationScriptResponse(String clientData, byte[] attestationData, String credentialId) {
        super(clientData, credentialId);

        this.attestationData = attestationData;
    }

    /**
     * Retrieve the raw bytes of the attestation data.
     *
     * @return the attestation data for this registration.
     */
    public byte[] getAttestationData() {
        return attestationData;
    }

}
