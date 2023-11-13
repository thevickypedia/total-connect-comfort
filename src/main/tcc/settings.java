package main.tcc;

import io.github.cdimascio.dotenv.Dotenv;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class settings {

    public static Dotenv dotenv = Dotenv.configure()
            .directory("src")
            .filename(config.env_filename)
            .load();
    public static String username = dotenv.get("TCC_USERNAME", dotenv.get("tcc_username"));
    public static String password = dotenv.get("TCC_PASSWORD", dotenv.get("tcc_password"));
    public static String device_name = dotenv.get("DEVICE_NAME", dotenv.get("device_name"));
    public static String base_url = "https://mytotalconnectcomfort.com/portal";

    public static void __init__() {
        List<String> missing = new ArrayList<>();
        for (Field field : settings.class.getDeclaredFields()) {
            try {
                String key = field.getName();
                Object value = field.get(settings.base_url);
                if (value == null && !key.equals("device_name")) {
                    missing.add(key);
                }
            } catch (IllegalAccessException error) {
                System.out.println(error.getMessage());
            }
        }
        if (!missing.isEmpty()) {
            System.out.println("Missing mandatory key(s) " + missing);
            System.exit(1);
        }
    }
}
