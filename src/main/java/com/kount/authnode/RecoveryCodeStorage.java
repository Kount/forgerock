package com.kount.authnode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.forgerock.openam.shared.security.crypto.Hashes;

import com.iplanet.am.util.SystemProperties;
public enum RecoveryCodeStorage {
    /**
     * Recovery codes are hashed using a salted strong one-way hash function and so are not recoverable after initial
     * device registration.
     */
    HASHED(Hashes::isHash, Hashes::passwordHash, Hashes::isValid),
    /**
     * Recovery codes are saved in their original form, allowing recovery. (NB: the entire device profile may be
     * encrypted, providing some level of protection).
     */
    LEGACY_UNPROTECTED(any -> true, Function.identity(), RecoveryCodeStorage::constantTimeEquals);

    private static final String USE_INSECURE_PROPERTY = "org.forgerock.openam.devices.recovery.use_insecure_storage";

    private final Predicate<String> isTransformed;
    private final Function<String, String> transform;
    private final BiPredicate<String, String> equality;

    /**
     * Constructor.
     *
     * @param isTransformed predicate to test if a value has already been transformed into the storage format.
     * @param transform function to transform a value into the storage format.
     * @param equality binary predicate to test if a stored code and a submitted (plain) code are equal.
     */
    RecoveryCodeStorage(Predicate<String> isTransformed, Function<String, String> transform,
            BiPredicate<String, String> equality) {
        this.isTransformed = isTransformed;
        this.transform = transform;
        this.equality = equality;
    }

    /**
     * Gets the storage scheme that has been configured for use.
     *
     * @return the storage scheme to use for recovery codes.
     */
    public static RecoveryCodeStorage getSystemStorage() {
        boolean useInsecure = SystemProperties.getAsBoolean(USE_INSECURE_PROPERTY, false);
        return useInsecure ? LEGACY_UNPROTECTED : HASHED;
    }

    /**
     * Transforms a list of recovery codes into the storage form.
     *
     * @param values the values to transform.
     * @return the transformed recovery codes.
     */
    public List<String> transform(List<String> values) {
        return values.stream()
                .map(it -> isTransformed.test(it) ? it : transform.apply(it))
                .collect(Collectors.toList());
    }

    /**
     * Determines if the given submitted recovery code is in the given list of recovery codes.
     *
     * @param recoveryCodes the list of valid recovery codes, in stored format.
     * @param submittedCode the submitted recovery code, in plain format.
     * @return the index of the matching recovery code in the list, or -1 if no match.
     */
    public int find(List<String> recoveryCodes, String submittedCode) {
        for (int i = 0; i < recoveryCodes.size(); ++i) {
            if (equality.test(recoveryCodes.get(i), submittedCode)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
