package main.tcc;

import io.github.cdimascio.dotenv.Dotenv;

public class settings {

    public static Dotenv dotenv = Dotenv.configure()
            .directory("src")
            .filename(".env")
            .load();
    public static String username = dotenv.get("TCC_USERNAME");
    public static String password = dotenv.get("TCC_PASSWORD");
    public static String device_name = dotenv.get("DEVICE_NAME");
    public static String base_url = "https://mytotalconnectcomfort.com/portal";
}
