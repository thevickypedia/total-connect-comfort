package main.tcc.api;

import main.tcc.LoggingFormatter;
import main.tcc.api.exceptions.AuthenticationError;
import main.tcc.api.exceptions.LoginUnexpectedError;
import main.tcc.api.exceptions.RedirectError;
import main.tcc.api.exceptions.TooManyAttemptsError;
import main.tcc.config;
import main.tcc.settings;

import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;


public class TCCWebAPI {
    public static Logger logger = Logger.getLogger(TCCWebAPI.class.getName());
    public static Integer locationId;

    public static void setupLogger() {
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LoggingFormatter());
        logger.addHandler(handler);
    }

    public static void authenticate() throws InterruptedException {
        for (int i = 1; i < 31; i++) {
            logger.info("Starting authentication attempt " + i);
            try {
                if (requests.do_authenticate()) {
                    return;
                } else {
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

    public static void main(String[] args) throws exceptions.NoDevicesFoundError {
        if (!config.env_file.exists()) {
            System.out.println("'" + config.env_filename + "' doesn't exist");
            return;
        }
        settings.__init__();
        setupLogger();
        try {
            authenticate();
        } catch (InterruptedException error) {
            logger.info(error.toString());
        }
        requests.get_devices();
    }
}
