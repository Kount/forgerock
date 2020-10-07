package com.kount.authnode;

public class ClientAuthenticationScriptResponse extends ClientScriptResponse {

    private final String userHandle;
    private final byte[] authenticatorData;
    private final byte[] signature;

    /**
     * Constructor for authentication script data.
     *
     * @param clientData the client data.
     * @param authenticatorData the raw bytes of the authenticatorData.
     * @param credentialId the credentialId;
     * @param signature the raw bytes of the signature.
     * @param userHandle the userHandle or null.
     */
    public ClientAuthenticationScriptResponse(String clientData, byte[] authenticatorData, String credentialId,
                                              byte[] signature, String userHandle) {
        super(clientData, credentialId);

        this.authenticatorData = authenticatorData;
        this.signature = signature;
        this.userHandle = userHandle;
    }

    /**
     * Get the user handle, or null if no handle was requested/supplied.
     *
     * @return the user handle, or null.
     */
    public String getUserHandle() {
        return userHandle;
    }

    /**
     * Get the raw bytes of the authenticator data.
     *
     * @return the authenticator data for this authentication.
     */
    public byte[] getAuthenticatorData() {
        return authenticatorData;
    }

    /**
     * Get the raw bytes of the signature.
     *
     * @return the signature of the authentication.
     */
    public byte[] getSignature() {
        return signature;
    }

}
