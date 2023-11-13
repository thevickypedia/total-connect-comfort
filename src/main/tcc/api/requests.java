package main.tcc.api;

import main.tcc.settings;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class requests {
    public static Logger logger = Logger.getLogger(TCCWebAPI.class.getName());

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

    public static boolean validate_auth_response(String responseBody, String response_url)
            throws exceptions.AuthenticationError, exceptions.TooManyAttemptsError, exceptions.RedirectError, exceptions.LoginUnexpectedError {
        logger.info(response_url);
        if (responseBody.contains("The email or password provided is incorrect") |
                responseBody.contains("The email address is not in the correct format") |
                responseBody.contains("Login was unsuccessful.")) {
            throw new exceptions.AuthenticationError(String.format("Email (%s) and/or password not accepted", settings.username));
        }
        if (response_url.contains("TooManyAttempts")) {
            throw new exceptions.TooManyAttemptsError("Too many attempts");
        }
        if (!response_url.contains("portal/") && !responseBody.contains("/portal/Device/Alerts?locationId=")) {
            throw new exceptions.RedirectError("Couldn't redirect to portal page");
        }
        if (response_url.contains("/Error")) {
            throw new exceptions.LoginUnexpectedError("Unexpected login error received");
        }
        Pattern pattern = Pattern.compile("locationId=(\\d+)");
        Matcher matcher = pattern.matcher(responseBody);
        if (matcher.find()) {
            TCCWebAPI.locationId = Integer.parseInt(matcher.group(1));
        } else {
            logger.warning("No locationId was found");
            return false;
        }
        return true;
    }

    public static StringEntity buildRequest() {
        JSONObject data = new JSONObject();
        data.put("UserName", settings.username);
        data.put("Password", settings.password);
        String jsonRequest = stringifyJSON(data, false);
        try {
            StringEntity requestEntity = new StringEntity(jsonRequest);
            requestEntity.setContentType("application/json");
            return requestEntity;
        } catch (UnsupportedEncodingException error) {
            logger.warning(error.getMessage());
            return null;
        }
    }

    public static List<Object> post_device_list(int page_num) {
        String url = settings.base_url + "/Device/GetZoneListData?locationId=" + TCCWebAPI.locationId + "&page=" + page_num;
        List<Object> data = makeRequest(url);
        int responseCode = (int) data.get(0);
        String responseBody = (String) data.get(1);
        HttpPost httpPost = (HttpPost) data.get(2);
        if (responseCode == 200) {
            // todo: more processing required
            System.out.println(responseCode);
            System.out.println(responseBody);
            System.out.println(httpPost);
            System.exit(1);
        }
        return data;
    }

    public static void get_devices() throws exceptions.NoDevicesFoundError {
        List<Object> devices = new ArrayList<>();
        for (int page = 1; page <= 6; page++) {
            logger.info("Attempting to get devices for location id, page: " + TCCWebAPI.locationId + ", " + page);
            List<Object> data = post_device_list(page);
            if (page == 1 && data.isEmpty()) {
                throw new exceptions.NoDevicesFoundError("No devices in page 1");
            } else if (data.isEmpty()) {
                logger.info("Page " + page + " is empty");
                break;
            }
            if (data.isEmpty()) {
                logger.info("Page " + page + " is empty");
                break;
            }
            devices.add(data);
        }
    }

    public static List<Object> makeRequest(String url) {
        List<Object> returnVal = new ArrayList<>();
        CloseableHttpClient httpClient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
        HttpPost httpPost = new HttpPost(url);
        try {
            StringEntity requestEntity = buildRequest();
            httpPost.setEntity(requestEntity);
            HttpResponse response = httpClient.execute(httpPost);
            int responseCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            returnVal.add(responseCode);
            returnVal.add(responseBody);
            returnVal.add(httpPost);
        } catch (IOException error) {
            logger.warning(error.toString());
        }
        return returnVal;
    }

    public static boolean do_authenticate() throws exceptions.AuthenticationError, exceptions.TooManyAttemptsError, exceptions.RedirectError, exceptions.LoginUnexpectedError {
        try {
            List<Object> requested = makeRequest(settings.base_url);
            int responseCode = (int) requested.get(0);
            String responseBody = (String) requested.get(1);
            HttpPost httpPost = (HttpPost) requested.get(2);
            if (responseCode <= 400) {
                return validate_auth_response(responseBody, httpPost.getURI().toString());
            } else {
                throw new ConnectException(String.format("%d - %s", responseCode, responseBody));
            }
        } catch (IOException error) {
            logger.warning(error.toString());
        }
        return true;
    }
}
