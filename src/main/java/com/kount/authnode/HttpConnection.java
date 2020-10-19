package com.kount.authnode;

import java.net.HttpURLConnection;
import java.net.ProtocolException;

public final class HttpConnection {
	 public static void setUpHttpPostConnection(HttpURLConnection connection , String API_KEY) throws ProtocolException {
		 connection.setRequestProperty("accept", "application/json");
			connection.setRequestProperty ("Authorization", "Bearer "+API_KEY);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
	 }
}
