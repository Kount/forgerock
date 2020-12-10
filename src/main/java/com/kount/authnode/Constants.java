package com.kount.authnode;

public class Constants {

	Constants() {
	}

	/** Kount Login Decisions */
	public static final String DECISION_OUTCOME_SUCCESS = "Success";
	public static final String DECISION_OUTCOME_FAILURE = "Failure";
	public static final String DECISION_OUTCOME_CHALLENGE = "Challenge";

	/** The Constant RESOURCE_LOCATION. */
	public static final String RESOURCE_LOCATION = "com/kount/authnode/";

	/** The Constant DATA_COLLECTOR. */
	public static final String DATA_COLLECTOR = RESOURCE_LOCATION + "dataCollector.js";

	/** The Constant BODY_CLASS. */
	public static final String BODY_CLASS = RESOURCE_LOCATION + "bodyClass.js";

	/** The Constant get trusted device path. */
	public static final String API_GET_TRUSTED_DEVICE_PATH = "/devices/{0}/clients/{1}/users";

	/** The Constant device not found. */
	public static final String DEVICE_NOT_FOUND = "Device not found";

	/** Trusted Device Constants */
	public static final String TRUSTED_DEVICE_STATE_UNASSIGNED = "UNASSIGNED";
	public static final String TRUSTED_DEVICE_STATE_BANNED = "BANNED";
	public static final String TRUSTED_DEVICE_STATE_TRUSTED = "TRUSTED";

	/** Default values */
	public static final String KOUNT_EVENT_DOMAIN = "https://example.com/events";
	public static final String KOUNT_LOGIN_DOMAIN = "https://www.example.com/login";
	public static final String KOUNT_LOGIN_UNIQUE_IDENTIFIER = "uid";
	public static final String KOUNT_PROFILER_MERCHANT_ID = "900900";
	public static final String KOUNT_PROFILER_DOMAIN = "example.com";
	public static final String KOUNT_TRUSTED_DEVICE_DOMAIN = "https://example.com/trusted-device";
	public static final String LOGIN_URL = "http://www.example.com/login";

	/** The constant Authentication Type in Events Node. */
	public static final String KOUNT_EVENTS_CONFIG_CHALLENGE = "Challenge";

	/** The constant UTC String Format for time. */
	public static final String UTC_TIMESTAMP = "yyyy-MM-dd'T'HH:mm:ss'.000Z'";

	/** The Constant for identifier of Time stored from Timer Node. */
	public static final String SET_TIMESTAMP = "KountTimerNodeStartTimestamp";
	public static final String COMPLETED_TIMESTAMP = "KountTimerNodeCompletedTimestamp";
}
