package com.kount.authnode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.forgerock.util.Reject;

import com.fasterxml.jackson.annotation.JsonIgnore;

//TODO Why is DeviceSettings in this repoistory?
public abstract class DeviceSettings {

    @JsonIgnore
    private final RecoveryCodeStorage codeStorage = RecoveryCodeStorage.getSystemStorage();

    /**
     * The unique identifier of the device settings.
     */
    protected String uuid;
    /**
     * The recovery codes associated with this device.
     */
    protected List<String> recoveryCodes = new ArrayList<>();

    /**
     * Configures the internal UUID.
     */
    public DeviceSettings() {
        uuid = UUID.randomUUID().toString();
    }

    /**
     * Sets the UUID for this device.
     *
     * @param uuid The UUID.
     */
    public void setUUID(String uuid) {
        Reject.ifNull(uuid, "UUID can not be null.");
        this.uuid = uuid;
    }

    /**
     * Gets the UUID from this device which is used as reference and set
     * on creation.
     *
     * @return UUID the UUID.
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Sets the remaining recovery codes for this device.
     *
     * @param recoveryCodes the remaining recovery codes for this device. Can not be null.
     */
    public void setRecoveryCodes(List<String> recoveryCodes) {
        Reject.ifNull(recoveryCodes);
        // Ensure that the codes are hashed and not recoverable
        this.recoveryCodes = codeStorage.transform(recoveryCodes);
    }

    /**
     * Gets the remaining recovery codes for this device. Necessary for serializing.
     *
     * The format depends on the selected {@link RecoveryCodeStorage}, and could be either plain text or hashed.
     *
     * @return List of recovery codes.
     */
    public List<String> getRecoveryCodes() {
        return this.recoveryCodes;
    }

    /**
     * Overwrites the recovery codes of this device with those of the given device.
     *
     * @param otherSettings the other device settings.
     */
    public void copyRecoveryCodesFrom(DeviceSettings otherSettings) {
        setRecoveryCodes(otherSettings.recoveryCodes);
    }

    /**
     * Attempts to use a recovery code. If the code matches any valid recovery code then that code is removed from
     * the valid code list. The caller should re-save the device profile in this case.
     *
     * @param codeAttempt the code submitted by the user.
     * @return true if the code was valid, or false if it is not recognised.
     */
    public boolean useRecoveryCode(String codeAttempt) {
        int index = codeStorage.find(recoveryCodes, codeAttempt);
        if (index >= 0) {
            recoveryCodes.remove(index);
            return true;
        }

        return false;
    }
}
