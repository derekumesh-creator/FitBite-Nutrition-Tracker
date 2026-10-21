// Food.java
// Stores ONE food entry. All nutrient values are the TOTALS for the quantity eaten
// (not the values per 100 g). Each entry also remembers the date it belongs to.
import java.time.LocalDate;

public class Food {

    private final String name;
    private final double quantityGrams;
    private final double calories;
    private final double protein;
    private final double carbohydrates;
    private final double fats;
    private final LocalDate date;

    public Food(String name, double quantityGrams, double calories,
                double protein, double carbohydrates, double fats, LocalDate date) {
        this.name = name;
        this.quantityGrams = quantityGrams;
        this.calories = calories;
        this.protein = protein;
        this.carbohydrates = carbohydrates;
        this.fats = fats;
        this.date = date;
    }

    public String getName()          { return name; }
    public double getQuantityGrams() { return quantityGrams; }
    public double getCalories()      { return calories; }
    public double getProtein()       { return protein; }
    public double getCarbohydrates() { return carbohydrates; }
    public double getFats()          { return fats; }
    public LocalDate getDate()       { return date; }

    /**
     * Converts this food to one line of the CSV file.
     * The name is written LAST so that commas inside a name cannot break the columns.
     * Format: date,quantity,calories,protein,carbs,fats,name
     */
    public String toCsvLine() {
        return date + "," + quantityGrams + "," + calories + "," + protein + ","
                + carbohydrates + "," + fats + "," + name;
    }

    /**
     * Builds a Food from one CSV line.
     * Throws IllegalArgumentException if the line is malformed, so the caller can skip it.
     */
    public static Food fromCsvLine(String line) {
        // limit 7 -> everything after the 6th comma stays together as the name
        String[] parts = line.split(",", 7);
        if (parts.length != 7) {
            throw new IllegalArgumentException("Wrong number of columns");
        }
        try {
            LocalDate date = LocalDate.parse(parts[0].trim());
            double quantity = Double.parseDouble(parts[1].trim());
            double calories = Double.parseDouble(parts[2].trim());
            double protein  = Double.parseDouble(parts[3].trim());
            double carbs    = Double.parseDouble(parts[4].trim());
            double fats     = Double.parseDouble(parts[5].trim());
            String name = parts[6].trim();

            if (name.isEmpty() || quantity <= 0 || calories < 0 || protein < 0
                    || carbs < 0 || fats < 0
                    || !Double.isFinite(quantity) || !Double.isFinite(calories)
                    || !Double.isFinite(protein) || !Double.isFinite(carbs)
                    || !Double.isFinite(fats)) {
                throw new IllegalArgumentException("Invalid values");
            }
            return new Food(name, quantity, calories, protein, carbs, fats, date);
        } catch (RuntimeException ex) {
            // Covers bad dates and bad numbers as well
            throw new IllegalArgumentException("Malformed line: " + line);
        }
    }
}