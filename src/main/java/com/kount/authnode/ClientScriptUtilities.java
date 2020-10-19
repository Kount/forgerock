package com.kount.authnode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static com.kount.authnode.EncodingUtilities.base64UrlDecode;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//TODO Why is ClientScriptUtilites in this repository?

@Singleton
public class ClientScriptUtilities {

    private final Logger logger = LoggerFactory.getLogger(ClientScriptUtilities.class);
    private static final String NUMBER_ENCODING_DELIMITER = ",";
    private static final String WAITING_MESSAGE_KEY = "waiting";
    private static final String SPINNER_SCRIPT = "org/forgerock/openam/auth/nodes/webauthn/webauthn-spinner.js";
    private static final String BUNDLE = ClientScriptUtilities.class.getName();

    /** Delimits various sections in client's responses. */
    static final String RESPONSE_DELIMITER = "::";

    /**
     * Gets a JavaScript script as a String.
     *
     * @param scriptFileName the filename of the script.
     * @return the script as an executable string.
     * @throws NodeProcessException if the file doesn't exist.
     */
    public String getScriptAsString(String scriptFileName) throws NodeProcessException {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(scriptFileName);
        String script;
        try {
            script = IOUtils.toString(resourceStream, "UTF-8");
        } catch (IOException e) {
            logger.error("Failed to get the script, fatal error!", e);
            throw new NodeProcessException(e);
        }
        return script;
    }

    /**
     * Gets the generic spinner script as a localized String.
     * @param locales the locale used for translation.
     * @return the spinner script as an executable String.
     * @throws NodeProcessException if the file doesn't exist.
     */
    String getSpinnerScript(PreferredLocales locales) throws NodeProcessException {
        ResourceBundle bundle = locales
                .getBundleInPreferredLocale(BUNDLE, AbstractDecisionNode.OutcomeProvider.class.getClassLoader());
        String spinnerScript = getScriptAsString(SPINNER_SCRIPT);
        return String.format(spinnerScript, bundle.getString(WAITING_MESSAGE_KEY));
    }

    /**
     * Get the public key credential parameters as a JavaScript String.
     * @param coseAlgorithms the algorithms allowed.
     * @return the public key credential params for the browser API call.
     */
    String getPubKeyCredParams(Set<CoseAlgorithm> coseAlgorithms) {
        List<Object> array = array();
        for (CoseAlgorithm coseAlgorithm : coseAlgorithms) {
            array.add(object(field("type", "public-key"), field("alg", coseAlgorithm.getCoseNumber())));
        }
        return json(array).toString();
    }

    /**
     * Parses the response from the client authentication script.
     * @param encodedResponse the response as an encoded String.
     * @param useSuppliedUserHandle whether to parse the user handle as the user identifier
     * @return the response as a rich data object.
     */
    ClientAuthenticationScriptResponse parseClientAuthenticationResponse(String encodedResponse,
                                                                         boolean useSuppliedUserHandle) {
        String[] resultsArray = encodedResponse.split(RESPONSE_DELIMITER);
        return new ClientAuthenticationScriptResponse(resultsArray[0],
                getBytesFromNumberEncoding(resultsArray[1]), resultsArray[3],
                getBytesFromNumberEncoding(resultsArray[2]),
                useSuppliedUserHandle ? new String(base64UrlDecode(resultsArray[4])) : null);
    }

    /**
     * Parses the response from the client registration script.
     *
     * @param encodedResponse the response as an encoded String.
     * @return the response as a rich data object.
     */
    ClientRegistrationScriptResponse parseClientRegistrationResponse(String encodedResponse) {
        String[] resultsArray = encodedResponse.split(RESPONSE_DELIMITER);
        ClientRegistrationScriptResponse response = new ClientRegistrationScriptResponse(resultsArray[0],
                getBytesFromNumberEncoding(resultsArray[1]), resultsArray[2]);
        return response;
    }

    private byte[] getBytesFromNumberEncoding(String data) {
        String[] numbers = data.split(NUMBER_ENCODING_DELIMITER);
        byte[] results = new byte[numbers.length];

        for (int i = 0; i < numbers.length; i++) {
            results[i] = Byte.parseByte(numbers[i]);
        }

        return results;
    }

    String getDevicesAsJavaScript(List<WebAuthnDeviceSettings> devices) {
        StringBuilder sb = new StringBuilder();

        for (WebAuthnDeviceSettings device : devices) {
            sb.append(getDeviceAsJavaScript(device)).append(",");
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String getDeviceAsJavaScript(WebAuthnDeviceSettings authenticatorEntry) {
        String credentialId = authenticatorEntry.getCredentialId();
        String decodedId = Arrays.toString(base64UrlDecode(credentialId));

        JsonValue js = json(object(
                        field("type", "public-key"),
                        field("id", "{ID_REPLACE}")));

        return js.toString().replace("\"{ID_REPLACE}\"", "new Int8Array(" + decodedId + ").buffer");
    }
}
