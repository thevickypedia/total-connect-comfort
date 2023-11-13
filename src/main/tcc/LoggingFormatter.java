package main.tcc;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggingFormatter extends Formatter {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd-yyyy hh:mm:ss a");

    @Override
    public String format(LogRecord record) {
        String timeFormat = dateFormat.format(new Date(record.getMillis()));
        String className = record.getSourceClassName();
        String methodName = record.getSourceMethodName();
        String message = formatMessage(record);
        return String.format("%s - [%s] - %s - %s%n", timeFormat, className, methodName, message);
    }
}
