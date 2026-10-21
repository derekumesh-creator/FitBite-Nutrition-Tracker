// Main.java
// Entry point of the application. It only launches the window.
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Swing windows must be created on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            NutritionTracker tracker = new NutritionTracker();
            tracker.setVisible(true);
        });
    }
}