package main.tcc.api;

import main.tcc.LoggingFormatter;
import main.tcc.api.exceptions.AuthenticationError;
import main.tcc.api.exceptions.LoginUnexpectedError;
import main.tcc.api.exceptions.RedirectError;
import main.tcc.api.exceptions.TooManyAttemptsError;
import main.tcc.settings;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;


public class TCCWebAPI {
    public static Logger logger = Logger.getLogger(TCCWebAPI.class.getName());

    public static void setupLogger() {
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LoggingFormatter());
        logger.addHandler(handler);
    }

    public static String getResponse(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringify = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringify.append(line);
        }
        connection.disconnect();
        return stringify.toString();
    }

    public static boolean validate_auth_response(HttpResponse response, String response_url)
            throws AuthenticationError, TooManyAttemptsError, RedirectError, LoginUnexpectedError {
        logger.info(response_url);
        try {
            String responseBody = EntityUtils.toString(response.getEntity());
            logger.info(responseBody);
            if (responseBody.contains("The email or password provided is incorrect") | responseBody.contains("The email address is not in the correct format")) {
                throw new AuthenticationError(String.format("Email (%s) and/or password not accepted", settings.username));
            }
            if (response_url.contains("TooManyAttempts")) {
                throw new TooManyAttemptsError("Too many attempts");
            }
            if (!response_url.contains("portal/")) {
                throw new RedirectError("Couldn't redirect to portal page");
            }
            if (response_url.contains("/Error")) {
                throw new LoginUnexpectedError("Unexpected login error received");
            }
            return true;
        } catch (IOException error) {
            logger.warning(error.toString());
            return false;
        }
    }

    public static String stringifyJSON(JSONObject data, boolean newline) {
        // Create a formatted JSON string
        StringBuilder jsonInputStringBuilder = new StringBuilder("{");
        if (newline) {
            jsonInputStringBuilder.append("\n");
        }
        for (String key : data.keySet()) {
            if (newline) {
                jsonInputStringBuilder.append("  \"").append(key).append("\": \"").append(data.get(key)).append("\",\n");
            } else {
                jsonInputStringBuilder.append("\"").append(key).append("\": \"").append(data.get(key)).append("\", ");
            }
        }
        String jsonInputString = jsonInputStringBuilder.toString();
        jsonInputString = jsonInputString.substring(0, jsonInputString.length() - 2); // Remove the trailing comma
        if (newline) {
            jsonInputString += "\n}";
        } else {
            jsonInputString += "}";
        }
        return jsonInputString;
    }

    public static boolean do_authenticate() throws AuthenticationError, TooManyAttemptsError, RedirectError, LoginUnexpectedError {
        // Create an HTTP client
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // Create an HTTP POST request
        HttpPost httpPost = new HttpPost(settings.base_url);

        try {
            // Set the request parameters
            String jsonRequest = "{\"UserName\":\"" + settings.username + "\",\"Password\":\"" + settings.password + "\"}";
            StringEntity requestEntity = new StringEntity(jsonRequest);
            requestEntity.setContentType("application/json");
            httpPost.setEntity(requestEntity);

            // Execute the POST request
            HttpResponse response = httpClient.execute(httpPost);

            // Get the response content
            String responseBody = EntityUtils.toString(response.getEntity());

            System.out.println("Response Status Code: " + response.getStatusLine().getStatusCode());
            System.out.println("Response Body: " + responseBody);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode <= 400) {
                boolean validated = validate_auth_response(response, httpPost.getURI().toString());
                try {
                    httpClient.close();
                } catch (Exception error) {
                    logger.warning(error.toString());
                }
                return validated;
            } else {
                throw new ConnectException(String.format("%d - %s", responseCode, responseBody));
            }
        } catch (IOException error) {
            logger.warning(error.toString());
        }
        return true;
    }

    public static void authenticate() throws InterruptedException {
        for (int i = 1; i < 31; i++) {
            logger.info("Starting authentication attempt " + i);
            try {
                if (do_authenticate()) {
                    return;
                } else {
                    // todo: check how to proceed
                    System.out.println("Unexpected");
                    break;
                }
            } catch (LoginUnexpectedError | AuthenticationError error) {
                logger.warning(error.toString());
                break;
            } catch (TooManyAttemptsError | RedirectError error) {
                logger.warning(error.toString());
                long exponent = (long) Math.pow(2, i);  // Math.pow returns a double type but sleep only accepts long
                logger.info(String.format("Unable to authenticate at this moment, sleeping for %d seconds", exponent));
                TimeUnit.SECONDS.sleep(exponent);
            }
        }
    }

    public static void main(String[] args) {
        setupLogger();
        try {
            authenticate();
        } catch (InterruptedException error) {
            logger.info(error.toString());
        }
    }
}
