package main.tcc.api;

import main.tcc.LoggingFormatter;
import main.tcc.api.exceptions.AuthenticationError;
import main.tcc.api.exceptions.LoginUnexpectedError;
import main.tcc.api.exceptions.RedirectError;
import main.tcc.api.exceptions.TooManyAttemptsError;
import main.tcc.settings;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    public static boolean _validate_auth_response(HttpURLConnection connection)
            throws AuthenticationError, TooManyAttemptsError, RedirectError, LoginUnexpectedError {
        String response_url = connection.getURL().toString();
        logger.info(response_url);
        try {
            String response = getResponse(connection);
            logger.info(response);
            if (response.contains("The email or password provided is incorrect") |
                    response.contains("The email address is not in the correct format")) {
                throw new AuthenticationError(
                        String.format("Email (%s) and/or password not accepted", settings.username)
                );
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
                jsonInputStringBuilder.append("  \"").append(key)
                        .append("\": \"").append(data.get(key)).append("\",\n");
            } else {
                jsonInputStringBuilder.append("\"").append(key)
                        .append("\": \"").append(data.get(key)).append("\", ");
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

    public static boolean _do_authenticate(URL url)
            throws TooManyAttemptsError, RedirectError, AuthenticationError, LoginUnexpectedError {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            JSONObject data = new JSONObject();
            data.put("UserName", settings.username);
            data.put("Password", settings.password);
            String jsonInputString = stringifyJSON(data, true);

            /*  Alternative approach
            String jsonInputString = String.format(
                    "{\n  \"UserName\": \"%s\",\n  \"Password\": \"%s\"\n}",
                    settings.username, settings.password
            );
            */

            try (OutputStream stream = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                stream.write(input);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return _validate_auth_response(connection);
            } else {
                throw new ConnectException(String.format("%d - %s", responseCode, connection.getResponseMessage()));
            }
        } catch (IOException error) {
            logger.warning(error.toString());
            return true;
        }
    }

    public static void authenticate() throws InterruptedException {
        for (int i = 1; i < 31; i++) {
            logger.info("Starting authentication attempt " + i);
            try {
                URL url = new URL(settings.base_url);
                if (_do_authenticate(url)) {
                    return;
                }
            } catch (LoginUnexpectedError | MalformedURLException | AuthenticationError error) {
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
