package main.tcc.api;

import main.tcc.LoggingFormatter;

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

    public static void authenticate() throws InterruptedException {
        for (int i = 1; i < 31; i++) {
            logger.info("Starting authentication attempt " + i);
            System.out.println("Do AUTH");
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
