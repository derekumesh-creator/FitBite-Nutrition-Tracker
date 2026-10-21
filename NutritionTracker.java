// NutritionTracker.java
// The main window of the FitBite application (built with Java Swing).
// Supports day-wise tracking and saves all data to data/fitbite-data.csv.
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NutritionTracker extends JFrame {

    // ---------- Colors used across the interface ----------
    private static final Color DARK_GREEN  = new Color(27, 120, 55);
    private static final Color MID_GREEN   = new Color(46, 160, 80);
    private static final Color LIGHT_GREEN = new Color(232, 245, 233);
    private static final Color WHITE       = Color.WHITE;
    private static final Color RED         = new Color(200, 60, 60);

    // ---------- File storage ----------
    // The data folder is created next to where you run the program.
    private static final Path DATA_FOLDER = Paths.get("data");
    private static final Path DATA_FILE   = DATA_FOLDER.resolve("fitbite-data.csv");
    private static final String CSV_HEADER =
            "date,quantityGrams,calories,protein,carbohydrates,fats,name";

    // Example output: 21 October 2026
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    // ---------- Data ----------
    // allFoods holds the entries of EVERY date (this is what gets saved to the file).
    private final ArrayList<Food> allFoods = new ArrayList<>();
    // visibleFoods holds only the entries of the selected date.
    // Row i of the table matches index i of this list.
    private final ArrayList<Food> visibleFoods = new ArrayList<>();
    // The day currently being displayed
    private LocalDate selectedDate = LocalDate.now();

    // ---------- Date controls ----------
    private JLabel dateHeadingLabel;
    private JTextField dateField;

    // ---------- Form fields ----------
    private JTextField nameField;
    private JTextField quantityField;
    private JTextField caloriesField;
    private JTextField proteinField;
    private JTextField carbsField;
    private JTextField fatsField;

    // ---------- Table ----------
    private JTable foodTable;
    private DefaultTableModel tableModel;

    // ---------- Summary labels (the big numbers on the cards) ----------
    private JLabel totalCaloriesLabel;
    private JLabel totalProteinLabel;
    private JLabel totalCarbsLabel;
    private JLabel totalFatsLabel;

    public NutritionTracker() {
        setTitle("FitBite – Nutrition Tracker");
        setSize(1000, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // centers the window on the screen
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);

        // Load saved data, then show today's entries
        String loadWarning = loadFoods();
        showSelectedDate();

        // If some lines of the file were unreadable, tell the user once the window is up
        if (loadWarning != null) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, loadWarning,
                    "Data File Warning", JOptionPane.WARNING_MESSAGE));
        }
    }

    // =====================================================================
    //  UI CONSTRUCTION
    // =====================================================================

    /** Green banner at the top with the title and subtitle. */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(DARK_GREEN);
        header.setBorder(new EmptyBorder(12, 25, 12, 25));

        JLabel title = new JLabel("FitBite");
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(WHITE);

        JLabel subtitle = new JLabel("Your Personal Nutrition Tracker");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitle.setForeground(LIGHT_GREEN);

        header.add(title);
        header.add(subtitle);
        return header;
    }

    /** Everything below the header: date bar, cards, form, table and buttons. */
    private JPanel createMainPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(WHITE);
        main.setBorder(new EmptyBorder(12, 25, 15, 25));

        // Top part = date bar + summary cards + entry form stacked vertically
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(WHITE);
        top.add(createDatePanel());
        top.add(Box.createVerticalStrut(10));
        top.add(createSummaryPanel());
        top.add(Box.createVerticalStrut(10));
        top.add(createFormPanel());

        main.add(top, BorderLayout.NORTH);
        main.add(createTablePanel(), BorderLayout.CENTER);
        main.add(createButtonPanel(), BorderLayout.SOUTH);
        return main;
    }

    /** Date bar: big heading plus Previous Day / Today / Next Day and a typed date box. */
    private JPanel createDatePanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 4));
        panel.setBackground(LIGHT_GREEN);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MID_GREEN, 2),
                new EmptyBorder(6, 10, 6, 10)));

        // Row 1: "Nutrition Log — 21 October 2026"
        dateHeadingLabel = new JLabel("", SwingConstants.CENTER);
        dateHeadingLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        dateHeadingLabel.setForeground(DARK_GREEN);

        // Row 2: the navigation controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setBackground(LIGHT_GREEN);

        JButton previousButton = createButton("Previous Day", DARK_GREEN, 130, 32);
        JButton todayButton    = createButton("Today", MID_GREEN, 90, 32);
        JButton nextButton     = createButton("Next Day", DARK_GREEN, 130, 32);

        JLabel typeLabel = new JLabel("   Go to date (yyyy-MM-dd):");
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        typeLabel.setForeground(DARK_GREEN);

        dateField = new JTextField(10);
        dateField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        dateField.setHorizontalAlignment(SwingConstants.CENTER);

        JButton goButton = createButton("Go", MID_GREEN, 60, 32);

        previousButton.addActionListener(e -> changeDate(selectedDate.minusDays(1)));
        nextButton.addActionListener(e -> changeDate(selectedDate.plusDays(1)));
        todayButton.addActionListener(e -> changeDate(LocalDate.now()));
        goButton.addActionListener(e -> goToTypedDate());
        dateField.addActionListener(e -> goToTypedDate()); // Enter key also works

        controls.add(previousButton);
        controls.add(todayButton);
        controls.add(nextButton);
        controls.add(typeLabel);
        controls.add(dateField);
        controls.add(goButton);

        panel.add(dateHeadingLabel);
        panel.add(controls);
        return panel;
    }

    /** Four cards: Total Calories, Protein, Carbohydrates, Fats (for the selected day). */
    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 15, 0));
        panel.setBackground(WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        totalCaloriesLabel = new JLabel("0.0", SwingConstants.CENTER);
        totalProteinLabel  = new JLabel("0.0", SwingConstants.CENTER);
        totalCarbsLabel    = new JLabel("0.0", SwingConstants.CENTER);
        totalFatsLabel     = new JLabel("0.0", SwingConstants.CENTER);

        panel.add(createCard("Total Calories (kcal)", totalCaloriesLabel));
        panel.add(createCard("Total Protein (g)", totalProteinLabel));
        panel.add(createCard("Total Carbohydrates (g)", totalCarbsLabel));
        panel.add(createCard("Total Fats (g)", totalFatsLabel));
        return panel;
    }

    /** Builds one summary card around the given value label. */
    private JPanel createCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(LIGHT_GREEN);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MID_GREEN, 2),
                new EmptyBorder(8, 8, 8, 8)));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setForeground(DARK_GREEN);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        valueLabel.setForeground(DARK_GREEN);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    /** The food entry form: a row of labels above a row of text fields. */
    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridLayout(2, 6, 10, 4));
        form.setBackground(WHITE);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(MID_GREEN), " Add a Food ");
        border.setTitleColor(DARK_GREEN);
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
        form.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(4, 8, 8, 8)));

        nameField     = new JTextField();
        quantityField = new JTextField();
        caloriesField = new JTextField();
        proteinField  = new JTextField();
        carbsField    = new JTextField();
        fatsField     = new JTextField();

        String[] labels = {"Food name", "Quantity (g)", "Calories per 100 g",
                           "Protein per 100 g", "Carbs per 100 g", "Fats per 100 g"};
        JTextField[] fields = {nameField, quantityField, caloriesField,
                               proteinField, carbsField, fatsField};

        // First row: the labels
        for (String text : labels) {
            JLabel label = new JLabel(text);
            label.setFont(new Font("SansSerif", Font.BOLD, 12));
            label.setForeground(DARK_GREEN);
            form.add(label);
        }
        // Second row: the text fields (pressing Enter also adds the food)
        for (JTextField field : fields) {
            field.setFont(new Font("SansSerif", Font.PLAIN, 14));
            field.addActionListener(e -> addFood());
            form.add(field);
        }
        return form;
    }

    /** The JTable inside a JScrollPane. */
    private JScrollPane createTablePanel() {
        String[] columns = {"Food Name", "Quantity (g)", "Calories (kcal)",
                            "Protein (g)", "Carbohydrates (g)", "Fats (g)"};

        // Make cells read-only so the user cannot edit them by double-clicking
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        foodTable = new JTable(tableModel);
        foodTable.setRowHeight(26);
        foodTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        foodTable.setSelectionBackground(new Color(165, 214, 167));
        foodTable.setSelectionForeground(Color.BLACK);
        foodTable.setGridColor(new Color(210, 225, 210));
        foodTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        foodTable.getTableHeader().setReorderingAllowed(false);

        // Center the text in every cell
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < foodTable.getColumnCount(); i++) {
            foodTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Green header with white text
        JTableHeader tableHeader = foodTable.getTableHeader();
        tableHeader.setPreferredSize(new Dimension(0, 32));
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        headerRenderer.setBackground(MID_GREEN);
        headerRenderer.setForeground(WHITE);
        headerRenderer.setFont(new Font("SansSerif", Font.BOLD, 14));
        headerRenderer.setOpaque(true);
        for (int i = 0; i < foodTable.getColumnCount(); i++) {
            foodTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(foodTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(MID_GREEN));
        scrollPane.getViewport().setBackground(WHITE);
        return scrollPane;
    }

    /** Add Food, Delete Selected and Clear All buttons. */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        panel.setBackground(WHITE);

        JButton addButton    = createButton("Add Food", MID_GREEN, 170, 40);
        JButton deleteButton = createButton("Delete Selected", new Color(230, 140, 30), 170, 40);
        JButton clearButton  = createButton("Clear All", RED, 170, 40);

        addButton.addActionListener(e -> addFood());
        deleteButton.addActionListener(e -> deleteSelected());
        clearButton.addActionListener(e -> clearAll());

        panel.add(addButton);
        panel.add(deleteButton);
        panel.add(clearButton);
        return panel;
    }

    /** Creates a consistently styled button of the given size. */
    private JButton createButton(String text, Color color, int width, int height) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, height >= 40 ? 15 : 13));
        button.setBackground(color);
        button.setForeground(WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(width, height));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // =====================================================================
    //  DATE NAVIGATION
    // =====================================================================

    /** Switches to another day and refreshes the table and totals. */
    private void changeDate(LocalDate newDate) {
        selectedDate = newDate;
        showSelectedDate();
    }

    /** Reads the yyyy-MM-dd text box and jumps to that date. */
    private void goToTypedDate() {
        try {
            LocalDate typed = LocalDate.parse(dateField.getText().trim());
            changeDate(typed);
        } catch (DateTimeParseException ex) {
            showError("Please enter a valid date in the format yyyy-MM-dd (for example 2026-10-21).");
            dateField.setText(selectedDate.toString()); // put the real date back
        }
    }

    /**
     * Rebuilds everything that depends on the selected date:
     * the heading, the visible list, the table rows and the four totals.
     */
    private void showSelectedDate() {
        dateHeadingLabel.setText("Nutrition Log \u2014 " + selectedDate.format(DISPLAY_FORMAT));
        dateField.setText(selectedDate.toString());

        // Pick only the foods that belong to this date
        visibleFoods.clear();
        for (Food food : allFoods) {
            if (food.getDate().equals(selectedDate)) {
                visibleFoods.add(food);
            }
        }

        // Refill the table
        tableModel.setRowCount(0);
        for (Food food : visibleFoods) {
            tableModel.addRow(createRow(food));
        }

        updateTotals();
    }

    // =====================================================================
    //  BUTTON ACTIONS
    // =====================================================================

    /** Validates the form, calculates actual nutrition, and stores the new food. */
    private void addFood() {
        String name = nameField.getText().trim();

        // 1. Reject empty fields
        if (name.isEmpty()
                || quantityField.getText().trim().isEmpty()
                || caloriesField.getText().trim().isEmpty()
                || proteinField.getText().trim().isEmpty()
                || carbsField.getText().trim().isEmpty()
                || fatsField.getText().trim().isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        try {
            // 2. Parse numbers (throws IllegalArgumentException with a clear message if bad)
            double quantity = parseNumber(quantityField.getText(), "Quantity", true);
            double caloriesPer100 = parseNumber(caloriesField.getText(), "Calories per 100 g", false);
            double proteinPer100  = parseNumber(proteinField.getText(), "Protein per 100 g", false);
            double carbsPer100    = parseNumber(carbsField.getText(), "Carbohydrates per 100 g", false);
            double fatsPer100     = parseNumber(fatsField.getText(), "Fats per 100 g", false);

            // 3. actual value = (value per 100 g * quantity) / 100
            double calories = (caloriesPer100 * quantity) / 100;
            double protein  = (proteinPer100 * quantity) / 100;
            double carbs    = (carbsPer100 * quantity) / 100;
            double fats     = (fatsPer100 * quantity) / 100;

            // 4. Create the Food object for the SELECTED date and store it
            Food food = new Food(name, quantity, calories, protein, carbs, fats, selectedDate);
            allFoods.add(food);
            visibleFoods.add(food);

            // 5. Add a matching row to the table
            tableModel.addRow(createRow(food));

            // 6. Refresh the totals and save to the file
            updateTotals();
            saveFoods();

            // 7. Clear the form and put the cursor back in the first field
            clearForm();
            nameField.requestFocusInWindow();

            JOptionPane.showMessageDialog(this, "\"" + name + "\" was added to "
                            + selectedDate.format(DISPLAY_FORMAT) + ".",
                    "Food Added", JOptionPane.INFORMATION_MESSAGE);

        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    /** Removes the selected row from the table and from both lists. */
    private void deleteSelected() {
        int row = foodTable.getSelectedRow();
        if (row == -1) {
            showError("Please select a food in the table first.");
            return;
        }
        // The table is never sorted, so row index == index in visibleFoods.
        Food food = visibleFoods.get(row);
        allFoods.remove(food);      // remove from the master list (same object)
        visibleFoods.remove(row);   // remove from today's list
        tableModel.removeRow(row);
        updateTotals();
        saveFoods();
    }

    /** Removes every entry of the SELECTED date only, after the user confirms. */
    private void clearAll() {
        if (visibleFoods.isEmpty()) {
            showError("There are no foods to clear for this date.");
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Remove ALL food entries for " + selectedDate.format(DISPLAY_FORMAT) + "?\n"
                        + "Entries from other dates will not be affected.",
                "Confirm Clear All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            allFoods.removeAll(visibleFoods); // only this day's foods are removed
            visibleFoods.clear();
            tableModel.setRowCount(0);
            updateTotals();
            saveFoods();
            JOptionPane.showMessageDialog(this, "The log for this date has been cleared.",
                    "Cleared", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // =====================================================================
    //  FILE STORAGE
    // =====================================================================

    /**
     * Loads foods from data/fitbite-data.csv into allFoods.
     * Returns a warning message if some lines could not be read, otherwise null.
     * A missing file is normal on the first run and is not an error.
     */
    private String loadFoods() {
        if (!Files.exists(DATA_FILE)) {
            return null;
        }
        int skippedLines = 0;
        try {
            List<String> lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty() || line.startsWith("date,")) {
                    continue; // skip blank lines and the header
                }
                try {
                    allFoods.add(Food.fromCsvLine(line));
                } catch (IllegalArgumentException ex) {
                    skippedLines++; // malformed line: ignore it and keep going
                }
            }
        } catch (IOException ex) {
            return "The data file could not be read, so FitBite started with an empty log.\n"
                    + "(" + ex.getMessage() + ")";
        }

        if (skippedLines > 0) {
            // Keep a copy of the original file so nothing is lost when we save again
            try {
                Files.copy(DATA_FILE, DATA_FOLDER.resolve("fitbite-data.csv.bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // The backup is only a bonus; continue anyway
            }
            return skippedLines + " unreadable line(s) in the data file were skipped.\n"
                    + "A backup of the original was saved as data/fitbite-data.csv.bak";
        }
        return null;
    }

    /**
     * Saves every food (all dates) to the CSV file.
     * It writes to a temporary file first and then replaces the data file,
     * so a crash while saving cannot leave a half-written data file.
     */
    private void saveFoods() {
        try {
            Files.createDirectories(DATA_FOLDER);
            Path tempFile = DATA_FOLDER.resolve("fitbite-data.csv.tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(CSV_HEADER);
                writer.newLine();
                for (Food food : allFoods) {
                    writer.write(food.toCsvLine());
                    writer.newLine();
                }
            }
            Files.move(tempFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            showError("Your change could not be saved to disk:\n" + ex.getMessage());
        }
    }

    // =====================================================================
    //  HELPER METHODS
    // =====================================================================

    /** Turns a Food into the array of text values for one table row. */
    private Object[] createRow(Food food) {
        return new Object[]{
                food.getName(),
                format(food.getQuantityGrams()),
                format(food.getCalories()),
                format(food.getProtein()),
                format(food.getCarbohydrates()),
                format(food.getFats())
        };
    }

    /**
     * Converts text to a number.
     * If mustBePositive is true the value must be greater than 0 (used for quantity),
     * otherwise it only has to be 0 or more (used for nutrients).
     */
    private double parseNumber(String text, String fieldName, boolean mustBePositive) {
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
        if (mustBePositive && value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative.");
        }
        return value;
    }

    /** Adds up the foods of the selected date and refreshes the four summary cards. */
    private void updateTotals() {
        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFats = 0;

        for (Food food : visibleFoods) {
            totalCalories += food.getCalories();
            totalProtein  += food.getProtein();
            totalCarbs    += food.getCarbohydrates();
            totalFats     += food.getFats();
        }

        totalCaloriesLabel.setText(format(totalCalories));
        totalProteinLabel.setText(format(totalProtein));
        totalCarbsLabel.setText(format(totalCarbs));
        totalFatsLabel.setText(format(totalFats));
    }

    /** Formats a number with one decimal place (always uses a dot). */
    private String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private void clearForm() {
        nameField.setText("");
        quantityField.setText("");
        caloriesField.setText("");
        proteinField.setText("");
        carbsField.setText("");
        fatsField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }
}