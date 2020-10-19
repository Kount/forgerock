package com.kount.authnode;
//TODO Why is ClientScriptResponse in this repository?
public abstract class ClientScriptResponse {

    private final String clientData;
    private final String credentialId;

    ClientScriptResponse(String clientData, String credentialId) {
        this.clientData = clientData;
        this.credentialId = credentialId;
    }

    /**
     * get the client data.
     *
     * @return the client data.
     */
    public String getClientData() {
        return clientData;
    }

    /**
     * Get the credential id.
     *
     * @return the credential id.
     */
    public String getCredentialId() {
        return credentialId;
    }
}
