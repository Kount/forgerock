package com.kount.authnode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.netty.shaded.io.netty.handler.codec.http.HttpMethod;

/**
 * The Class HttpConnection.
 */
public final class HttpConnection {

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(HttpConnection.class);

	/**
	 * Sets the up http connection.
	 *
	 * @param connection the connection
	 * @param apiKey     the api key
	 * @param method     the method
	 * @return the http URL connection
	 * @throws ProtocolException the protocol exception
	 */
	public HttpURLConnection setUpHttpConnection(HttpURLConnection connection, String apiKey, String method)
			throws ProtocolException {
		connection.setRequestProperty("accept", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setRequestMethod(method);
		connection.setDoOutput(true);
		return connection;
	}

	/**
	 * HTTP Post Request
	 *
	 * @param uri     the uri
	 * @param payload the payload
	 * @param apiKey  the api key
	 * @return the http URL connection
	 */
	public HttpURLConnection post(String uri, String payload, String apiKey) {
		HttpURLConnection connection = null;
		try {
			logger.debug("Start HttpConnection.post(), uri:" + uri);
			URL url = new URL(uri);
			connection = (HttpURLConnection) url.openConnection();
			
			// Now it's "open", we can set the request method, headers etc.
			setUpHttpConnection(connection, apiKey, HttpMethod.GET.toString());

			OutputStream os = connection.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			osw.write(payload);
			osw.flush();
			osw.close();
			os.close(); // don't forget to close the OutputStream
			connection.connect();
			logger.debug("End Response HttpConnection.post(), responseCode:" + connection.getResponseCode());
		} catch (Exception e) {
			logger.error("ERROR: HttpConnection.post(), Message:" + e.getMessage());
		}
		return connection;
	}

	/**
	 * HTTP Get Request
	 *
	 * @param uri    the uri
	 * @param apiKey the api key
	 * @return the http URL connection
	 */
	public HttpURLConnection get(String uri, String apiKey) {
		HttpURLConnection connection = null;
		try {
			logger.debug("Start HttpConnection.GET(), uri:" + uri);
			URL url = new URL(uri);
			connection = (HttpURLConnection) url.openConnection();

			setUpHttpConnection(connection, apiKey, HttpMethod.GET.toString());

			connection.connect();
			logger.debug("End Response HttpConnection.get(), responseCode:" + connection.getResponseCode());
		} catch (Exception e) {
			logger.error("ERROR: HttpConnection.get(), Message:" + e.getMessage());
		}
		return connection;
	}

	/**
	 * Parses the response.
	 *
	 * @param connection the connection
	 * @return the HTTP response
	 */
	public HTTPResponse parseResponse(HttpURLConnection connection) {
		logger.debug("In HttpConnection.parseResponse()");
		HTTPResponse httpResponse = new HTTPResponse();
		try {
			httpResponse = new HTTPResponse();
			InputStreamReader in = new InputStreamReader((InputStream) connection.getContent());
			BufferedReader buff = new BufferedReader(in);
			String line;
			StringBuilder builder = new StringBuilder();
			do {
				line = buff.readLine();
				builder.append(line).append("\n");
			} while (line != null);
			buff.close();

			httpResponse.setResponseCode(connection.getResponseCode());
			httpResponse.setBuilder(builder);
		} catch (Exception e) {
			httpResponse = null;
			logger.error("ERROR: HttpConnection.get(), Unable to parse response for API : "
					+ ((connection != null) ? connection.getURL() : "") + " Message:" + e.getMessage());
		}

		return httpResponse;
	}

	/**
	 * The Class HTTPResponse.
	 */
	static class HTTPResponse {

		/** The response code. */
		int responseCode;

		/** The builder. */
		StringBuilder builder = new StringBuilder();

		/**
		 * Gets the response code.
		 *
		 * @return the response code
		 */
		public int getResponseCode() {
			return responseCode;
		}

		/**
		 * Sets the response code.
		 *
		 * @param responseCode the new response code
		 */
		public void setResponseCode(int responseCode) {
			this.responseCode = responseCode;
		}

		/**
		 * Gets the builder.
		 *
		 * @return the builder
		 */
		public StringBuilder getBuilder() {
			return builder;
		}

		/**
		 * Sets the builder.
		 *
		 * @param builder the new builder
		 */
		public void setBuilder(StringBuilder builder) {
			this.builder = builder;
		}

	}
}
