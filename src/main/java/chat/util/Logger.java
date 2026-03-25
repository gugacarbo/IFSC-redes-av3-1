package chat.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public class Logger {
    private static final java.util.logging.Logger rootLogger = 
        java.util.logging.Logger.getLogger("chat");
    
    private static final String logDirectory = "logs";
    private static final int RETENTION_DAYS = 7;
    private static final DateTimeFormatter dateFormatter = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    static {
        try {
            File logDir = new File(logDirectory);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            cleanOldLogs(logDir);
            String logPattern = logDirectory + "/chat_%u_%g.log";
            java.util.logging.FileHandler fileHandler = new java.util.logging.FileHandler(logPattern, true);
            fileHandler.setFormatter(new java.util.logging.SimpleFormatter() {
                @Override
                public String format(java.util.logging.LogRecord lr) {
                    return String.format("[%s] [%s] %s%n",
                        LocalDate.now().format(dateFormatter),
                        lr.getLevel().getName(),
                        lr.getMessage());
                }
            });
            rootLogger.addHandler(fileHandler);
            rootLogger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("Failed to initialize file logging: " + e.getMessage());
        }
    }
    
    private static void cleanOldLogs(File logDir) {
        File[] oldLogs = logDir.listFiles((dir, name) -> name.startsWith("chat_") && name.endsWith(".log"));
        if (oldLogs != null) {
            LocalDate cutoff = LocalDate.now().minusDays(RETENTION_DAYS);
            for (File log : oldLogs) {
                String dateStr = log.getName().replace("chat_", "").replace(".log", "");
                try {
                    LocalDate logDate = LocalDate.parse(dateStr.substring(0, 10), dateFormatter);
                    if (logDate.isBefore(cutoff)) {
                        log.delete();
                    }
                } catch (Exception e) {
                }
            }
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
