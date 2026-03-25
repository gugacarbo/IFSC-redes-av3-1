package chat.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class Logger {
    private static final java.util.logging.Logger rootLogger = 
        java.util.logging.Logger.getLogger("chat");
    
    private static final String logDirectory = "logs";
    private static final DateTimeFormatter dateFormatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    static {
        try {
            java.io.File logDir = new java.io.File(logDirectory);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            String logFile = logDirectory + "/chat_" + LocalDate.now().format(dateFormatter) + ".log";
            java.util.logging.FileHandler fileHandler = new java.util.logging.FileHandler(logFile, true);
            fileHandler.setFormatter(new java.util.logging.SimpleFormatter());
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("Failed to initialize file logging: " + e.getMessage());
        }
    }
    
    public static void info(String message) {
        rootLogger.info(message);
    }
    
    public static void debug(String message) {
        rootLogger.fine(message);
    }
    
    public static void warn(String message) {
        rootLogger.warning(message);
    }
    
    public static void error(String message) {
        rootLogger.severe(message);
    }
    
    public static void error(String message, Throwable throwable) {
        rootLogger.severe(message);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        rootLogger.severe(sw.toString());
    }
}
