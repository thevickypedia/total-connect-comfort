package main.tcc.api;

public class exceptions {
    public static class TCCWebAPIError extends Exception {
        public TCCWebAPIError(String message) {
            super(message);
        }
    }

    public static class AuthenticationError extends TCCWebAPIError {
        public AuthenticationError(String message) {
            super(message);
        }
    }

    public static class TooManyAttemptsError extends TCCWebAPIError {
        public TooManyAttemptsError(String message) {
            super(message);
        }
    }

    public static class RedirectError extends TCCWebAPIError {
        public RedirectError(String message) {
            super(message);
        }
    }

    public static class LoginUnexpectedError extends TCCWebAPIError {
        public LoginUnexpectedError(String message) {
            super(message);
        }
    }

    public static class NoDevicesFoundError extends TCCWebAPIError {
        public NoDevicesFoundError(String message) {
            super(message);
        }
    }
}
