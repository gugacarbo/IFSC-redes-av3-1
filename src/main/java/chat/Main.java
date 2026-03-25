package chat;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import chat.util.Logger;

public class Main {
    public static void main(String[] args) {
        configureLookAndFeel();
        System.out.println("UDP Chat P2P - Starting...");
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Logger.info("System look and feel applied: " + UIManager.getLookAndFeel().getName());
        } catch (ClassNotFoundException e) {
            Logger.error("System Look and Feel class not found, using cross-platform", e);
            fallbackToCrossPlatform();
        } catch (InstantiationException e) {
            Logger.error("Failed to instantiate system Look and Feel, using cross-platform", e);
            fallbackToCrossPlatform();
        } catch (IllegalAccessException e) {
            Logger.error("Access denied for system Look and Feel, using cross-platform", e);
            fallbackToCrossPlatform();
        } catch (UnsupportedLookAndFeelException e) {
            Logger.error("System Look and Feel unsupported, using cross-platform", e);
            fallbackToCrossPlatform();
        }
    }

    private static void fallbackToCrossPlatform() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            Logger.info("Cross-platform look and feel applied: " + UIManager.getLookAndFeel().getName());
        } catch (Exception e) {
            Logger.error("Failed to apply cross-platform look and feel", e);
        }
    }
}
