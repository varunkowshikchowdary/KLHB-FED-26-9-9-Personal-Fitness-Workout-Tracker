/*
 * Personal Fitness & Workout Tracker
 * Course : Problem Solving Using Programming (Java) - 26SC1101E
 *
 * A menu-driven console app to log workouts, search / sort / remove them,
 * see a weekly chart, calculate BMI and print a summary report.
 * Workouts are saved in "workouts.txt" so they are still there next time.
 *
 * How to run:
 *     javac FitnessTracker.java
 *     java FitnessTracker
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FitnessTracker {

    // ===================== Constants and shared data =====================

    static final String FILE_NAME = "workouts.txt";
    static final Path SAVE_FILE = Path.of(FILE_NAME);
    static final int MAX_WORKOUTS = 50;
    static final int WEEKLY_GOAL_MINUTES = 150;

    // planned minutes for Mon, Tue, Wed, Thu, Fri, Sat, Sun
    static final int[] PLANNED_MINUTES = {30, 45, 0, 30, 45, 60, 0};

    static final Scanner input = new Scanner(System.in);
    static ArrayList<Workout> workouts = new ArrayList<>();
    static UserProfile profile;


    // ============================= main ==================================

    public static void main(String[] args) {
        System.out.println("=============================================");
        System.out.println("   PERSONAL FITNESS & WORKOUT TRACKER");
        System.out.println("=============================================");

        try {
            String typedName = readText("Enter your name (or press Enter to skip): ").trim();
            if (typedName.isEmpty()) {
                profile = new UserProfile();
            } else {
                profile = new UserProfile(typedName);
            }

            loadFromFile();
            runMenu();
        } catch (RuntimeException e) {
            // happens when there is no more input to read (for example Ctrl+D)
            System.out.println();
            System.out.println("Input ended unexpectedly. Closing the tracker.");
        } finally {
            input.close();
        }
    }


    // ======================= Menu (if / switch / loops) ===================

    static void runMenu() {
        int choice;

        do {
            showMenu();
            choice = readInt("Choose an option (1-9): ", 1, 9);

            switch (choice) {
                case 1:
                    addWorkout();
                    break;
                case 2:
                    viewWorkouts();
                    break;
                case 3:
                    searchWorkout();
                    break;
                case 4:
                    sortWorkouts();
                    break;
                case 5:
                    removeWorkout();
                    break;
                case 6:
                    showWeeklyChart();
                    break;
                case 7:
                    bmiMenu();
                    break;
                case 8:
                    showSummary();
                    break;
                case 9:
                    System.out.println("Goodbye, " + profile.getName() + "! Stay active.");
                    break;
                default:
                    System.out.println("That option does not exist.");
            }
        } while (choice != 9);
    }

    static void showMenu() {
        System.out.println();
        System.out.println("===== PERSONAL FITNESS & WORKOUT TRACKER =====");
        System.out.println("1. Add Workout");
        System.out.println("2. View Workouts");
        System.out.println("3. Search Workouts");
        System.out.println("4. Sort Workouts");
        System.out.println("5. Remove Workout");
        System.out.println("6. Weekly Chart");
        System.out.println("7. Calculate BMI");
        System.out.println("8. Summary Report");
        System.out.println("9. Exit");
    }


    // ================== Reading input (Scanner + validation) ==============

    static String readText(String prompt) {
        System.out.print(prompt);
        return input.nextLine();
    }

    // keeps asking until the user types a whole number between min and max
    static int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readText(prompt).trim();
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("That is not a whole number, try again.");
            }
        }
    }

    static double readDouble(String prompt, double min, double max) {
        while (true) {
            String text = readText(prompt).trim();
            try {
                double value = Double.parseDouble(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("Please enter a value between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("That is not a number, try again.");
            }
        }
    }

    // checks a workout name and returns it in a neat format
    static String cleanName(String raw) throws InvalidInputException {
        String name = raw.trim();

        if (name.isEmpty()) {
            throw new InvalidInputException("Workout name cannot be empty.");
        }
        if (name.length() > 25) {
            throw new InvalidInputException("Workout name must be 25 characters or fewer.");
        }

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            boolean isLetter = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
            boolean isDigit = ch >= '0' && ch <= '9';
            boolean allowed = isLetter || isDigit || ch == ' ' || ch == '-';
            if (!allowed) {
                throw new InvalidInputException(
                        "Only letters, digits, spaces and '-' are allowed (found '" + ch + "').");
            }
        }
        return capitalise(name);
    }

    // "push-UPS" becomes "Push-ups"
    static String capitalise(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase()
                + text.substring(1).toLowerCase();
    }

    static void printReport(Reportable item) {
        System.out.println(item.summary());
    }


    // ===================== Add / view / search / sort / remove ===========

    static void addWorkout() {
        if (workouts.size() >= MAX_WORKOUTS) {
            System.out.println("The log is full (" + MAX_WORKOUTS + " workouts). Remove one first.");
            return;
        }

        try {
            String name = cleanName(readText("Workout name: "));
            int minutes = readInt("Duration in minutes (1-300): ", 1, 300);
            int day = readInt("Day of the week (1 = Mon ... 7 = Sun): ", 1, 7);

            System.out.println("Type: 1) Cardio   2) Strength   3) Other");
            int type = readInt("Pick a type: ", 1, 3);

            Workout w;
            if (type == 1) {
                double km = readDouble("Distance in km: ", 0.1, 500);
                w = new CardioWorkout(name, minutes, day, km);
            } else if (type == 2) {
                int sets = readInt("Number of sets: ", 1, 20);
                int reps = readInt("Reps per set: ", 1, 200);
                w = new StrengthWorkout(name, minutes, day, sets, reps);
            } else {
                w = new Workout(name, minutes, day);
            }

            workouts.add(w);
            appendToFile(w);
            System.out.println("Added: " + w.summary());
        } catch (InvalidInputException e) {
            System.out.println("Invalid input: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("The workout was added but could not be saved: " + e.getMessage());
        } finally {
            System.out.println("(Returning to the menu)");
        }
    }

    static void viewWorkouts() {
        if (workouts.isEmpty()) {
            System.out.println("No workouts recorded yet.");
            return;
        }

        System.out.println();
        System.out.println("----- Workout History -----");
        for (int i = 0; i < workouts.size(); i++) {
            System.out.print((i + 1) + ". ");
            printReport(workouts.get(i));
        }

        int total = 0;
        for (Workout w : workouts) {
            total += w.getMinutes();
        }
        System.out.println("Workouts logged    : " + workouts.size());
        System.out.println("Total workout time : " + total + " minutes");
    }

    static void searchWorkout() {
        if (workouts.isEmpty()) {
            System.out.println("Nothing to search - the log is empty.");
            return;
        }

        System.out.println("1) Search by name   2) Search by exact duration");
        int how = readInt("Pick: ", 1, 2);

        if (how == 1) {
            String key = readText("Name contains: ").trim().toLowerCase();
            if (key.isEmpty()) {
                System.out.println("Please type something to search for.");
                return;
            }

            int found = 0;
            for (Workout w : workouts) {
                if (w.getName().toLowerCase().contains(key)) {
                    System.out.println("  " + w.summary());
                    found++;
                }
            }
            if (found == 0) {
                System.out.println("No workout name contains \"" + key + "\".");
            }
        } else {
            int target = readInt("Duration in minutes: ", 1, 300);

            // binary search needs a sorted array, so copy the durations and sort them
            int[] sorted = new int[workouts.size()];
            for (int i = 0; i < sorted.length; i++) {
                sorted[i] = workouts.get(i).getMinutes();
            }
            Arrays.sort(sorted);

            if (Arrays.binarySearch(sorted, target) >= 0) {
                System.out.println("Workouts lasting exactly " + target + " minutes:");
                for (Workout w : workouts) {
                    if (w.getMinutes() == target) {
                        System.out.println("  " + w.summary());
                    }
                }
            } else {
                System.out.println("No workout lasts exactly " + target + " minutes.");
            }
        }
    }

    static void sortWorkouts() {
        if (workouts.isEmpty()) {
            System.out.println("Nothing to sort - the log is empty.");
            return;
        }

        System.out.println("1) Shortest first   2) Longest first   3) Name A-Z");
        int how = readInt("Pick: ", 1, 3);

        if (how == 1) {
            Collections.sort(workouts);                  // uses compareTo() from Comparable
        } else if (how == 2) {
            Collections.sort(workouts);
            Collections.reverse(workouts);
        } else {
            Comparator<Workout> byName =
                    (a, b) -> a.getName().compareToIgnoreCase(b.getName());
            Collections.sort(workouts, byName);
        }
        viewWorkouts();
    }

    static void removeWorkout() {
        if (workouts.isEmpty()) {
            System.out.println("Nothing to remove - the log is empty.");
            return;
        }

        String name = readText("Name of the workout to remove: ").trim();
        int removed = 0;

        // an Iterator lets us remove items safely while looping
        Iterator<Workout> it = workouts.iterator();
        while (it.hasNext()) {
            Workout w = it.next();
            if (w.getName().equalsIgnoreCase(name)) {    // equals-style check, not ==
                it.remove();
                removed++;
            }
        }

        if (removed == 0) {
            System.out.println("No workout named \"" + name + "\" was found.");
            return;
        }

        try {
            saveAllToFile();
            System.out.println("Removed " + removed + " workout(s) named \"" + name + "\".");
        } catch (IOException e) {
            System.out.println("Removed from the list, but the file could not be updated: " + e.getMessage());
        }
    }


    // ================ Weekly chart (methods, arrays, recursion) ===========

    static void showWeeklyChart() {
        int[][] chart = buildWeekChart();
        String[] rowNames = {"Planned", "Done", "Gap"};

        System.out.println();
        System.out.println("----- Weekly Chart (minutes) -----");

        System.out.printf("%-9s", "");
        for (String day : Workout.DAY_NAMES) {
            System.out.printf("%5s", day);
        }
        System.out.println();

        for (int row = 0; row < chart.length; row++) {
            System.out.printf("%-9s", rowNames[row]);
            for (int col = 0; col < chart[row].length; col++) {
                System.out.printf("%5d", chart[row][col]);
            }
            System.out.println();
        }
        System.out.println("(a negative gap means you did more than planned)");

        int[] done = chart[1];

        System.out.println();
        for (int d = 0; d < done.length; d++) {
            System.out.printf("%-4s|%s %d%n", Workout.DAY_NAMES[d], makeBar(done[d]), done[d]);
        }

        int total = sumMinutes(done, 0);
        double average = (double) total / done.length;
        int activeDays = countActiveDays(done);

        System.out.println();
        System.out.println("Total this week   : " + total + " minutes");
        System.out.printf("Daily average     : %.1f minutes%n", average);
        System.out.println("Active days       : " + activeDays + " of " + done.length);

        int best = findBestDay(done);
        if (best == -1) {
            System.out.println("Best day          : none yet");
        } else {
            System.out.println("Best day          : " + Workout.DAY_NAMES[best] + " (" + done[best] + " min)");
        }
    }

    // row 0 = planned, row 1 = done (from the log), row 2 = planned - done
    static int[][] buildWeekChart() {
        int[][] chart = new int[3][PLANNED_MINUTES.length];

        for (int d = 0; d < PLANNED_MINUTES.length; d++) {
            chart[0][d] = PLANNED_MINUTES[d];
        }
        for (Workout w : workouts) {
            chart[1][w.getDay() - 1] += w.getMinutes();
        }
        for (int d = 0; d < PLANNED_MINUTES.length; d++) {
            chart[2][d] = chart[0][d] - chart[1][d];
        }
        return chart;
    }

    // recursive sum: base case = we ran past the end of the array
    static int sumMinutes(int[] minutes, int index) {
        if (index == minutes.length) {
            return 0;
        }
        return minutes[index] + sumMinutes(minutes, index + 1);
    }

    static int countActiveDays(int[] minutes) {
        int count = 0;
        for (int m : minutes) {
            if (m == 0) {
                continue;               // rest day, skip it
            }
            count++;
        }
        return count;
    }

    // simple linear search for the biggest value, returns its index (-1 if all zero)
    static int findBestDay(int[] minutes) {
        int bestIndex = -1;
        int bestValue = 0;
        for (int i = 0; i < minutes.length; i++) {
            if (minutes[i] > bestValue) {
                bestValue = minutes[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    // one '#' for every 10 minutes (max 30 so the bar never runs off the screen)
    static String makeBar(int minutes) {
        int blocks = minutes / 10;
        if (blocks > 30) {
            blocks = 30;
        }
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < blocks; i++) {
            bar.append('#');
        }
        return bar.toString();
    }


    // ======================= BMI (overloading, if-else ladder) ============

    static void bmiMenu() {
        double weight = readDouble("Weight in kg (10-300): ", 10, 300);
        int heightCm = readInt("Height in cm (50-250): ", 50, 250);

        profile.setWeightKg(weight);
        profile.setHeightCm(heightCm);

        double bmi = calculateBmi(weight, heightCm);
        String category = bmiCategory(bmi);
        String advice = (category.equals("Normal weight"))
                ? "Great, keep it up!"
                : "Keep logging your workouts - small steps add up.";

        System.out.printf("BMI      : %.2f%n", bmi);
        System.out.println("Category : " + category);
        System.out.println(advice);
    }

    static double calculateBmi(double weightKg, double heightM) {
        return weightKg / (heightM * heightM);
    }

    // same name, different parameter type: height given in centimetres
    static double calculateBmi(double weightKg, int heightCm) {
        double heightM = heightCm / 100.0;
        return calculateBmi(weightKg, heightM);
    }

    static String bmiCategory(double bmi) {
        String category;
        if (bmi < 18.5) {
            category = "Underweight";
        } else if (bmi < 25.0) {
            category = "Normal weight";
        } else if (bmi < 30.0) {
            category = "Overweight";
        } else {
            category = "Obese";
        }
        return category;
    }


    // =============== Summary report (collections, lambdas, streams) =======

    static void showSummary() {
        Supplier<String> title = () -> "----- Summary for " + profile.getName() + " -----";
        System.out.println();
        System.out.println(title.get());
        printReport(profile);

        if (workouts.isEmpty()) {
            System.out.println("No workouts to summarise yet.");
            return;
        }

        // totals with reduce()
        int totalMinutes = workouts.stream()
                .map(Workout::getMinutes)
                .reduce(0, Integer::sum);
        double totalCalories = workouts.stream()
                .map(Workout::caloriesBurned)
                .reduce(0.0, Double::sum);

        System.out.printf("Total workout time : %d minutes (%d h %d min)%n",
                totalMinutes, totalMinutes / 60, totalMinutes % 60);
        System.out.println("Estimated calories : about " + (int) totalCalories + " kcal (rough estimate)");

        // goal progress, with a nested if
        int percent = (int) (totalMinutes * 100.0 / WEEKLY_GOAL_MINUTES);
        String goalNote;
        if (totalMinutes >= WEEKLY_GOAL_MINUTES) {
            if (totalMinutes >= 2 * WEEKLY_GOAL_MINUTES) {
                goalNote = "double the goal, superb!";
            } else {
                goalNote = "goal reached!";
            }
        } else if (totalMinutes >= WEEKLY_GOAL_MINUTES / 2) {
            goalNote = "more than halfway there.";
        } else {
            goalNote = "just getting started.";
        }
        System.out.println("Goal progress      : " + percent + "% of " + WEEKLY_GOAL_MINUTES
                + " min - " + goalNote);

        // functional interfaces used to pass behaviour around
        Predicate<Workout> isLong = w -> w.getMinutes() >= 45;
        Function<Workout, String> label =
                w -> w.getName() + " - " + w.getMinutes() + " min";
        Consumer<String> bullet =
                text -> System.out.println("   * " + text);
        Comparator<Workout> longestFirst =
                (a, b) -> Integer.compare(b.getMinutes(), a.getMinutes());

        long longCount = workouts.stream().filter(isLong).count();
        System.out.println("Long sessions (45+): " + longCount);

        System.out.println("Top 3 longest sessions:");
        workouts.stream()
                .sorted(longestFirst)
                .limit(3)
                .map(label)
                .forEach(bullet);

        String types = workouts.stream()
                .map(Workout::getType)
                .distinct()
                .sorted()
                .collect(Collectors.joining(" / "));
        System.out.println("Types logged       : " + types);

        System.out.println("Sessions per type:");
        Map<String, Long> sessionsPerType = workouts.stream()
                .collect(Collectors.groupingBy(Workout::getType,
                        Collectors.counting()));
        sessionsPerType.forEach((type, count) ->
                System.out.println("   " + type + ": " + count));

        // an ordered map (TreeMap keeps the keys in A-Z order)
        TreeMap<String, Integer> minutesPerType = new TreeMap<>();
        for (Workout w : workouts) {
            String type = w.getType();
            if (minutesPerType.containsKey(type)) {
                minutesPerType.put(type, minutesPerType.get(type) + w.getMinutes());
            } else {
                minutesPerType.put(type, w.getMinutes());
            }
        }
        System.out.println("Minutes per type:");
        for (String type : minutesPerType.keySet()) {
            System.out.println("   " + type + ": " + minutesPerType.get(type) + " min");
        }

        // a Set removes duplicates, a HashMap counts how often each name appears
        Set<String> exercises = new HashSet<>();
        HashMap<String, Integer> timesDone = new HashMap<>();
        for (Workout w : workouts) {
            String key = w.getName().toLowerCase();
            exercises.add(key);
            if (timesDone.containsKey(key)) {
                timesDone.put(key, timesDone.get(key) + 1);
            } else {
                timesDone.put(key, 1);
            }
        }

        String favourite = "";
        int favouriteCount = 0;
        for (String key : timesDone.keySet()) {
            int count = timesDone.get(key);
            boolean higher = count > favouriteCount;
            boolean tieButEarlier = count == favouriteCount
                    && key.compareTo(favourite) < 0;
            if (higher || tieButEarlier) {
                favourite = key;
                favouriteCount = count;
            }
        }
        System.out.println("Different exercises: " + exercises.size());
        System.out.println("Favourite exercise : " + capitalise(favourite) + " (" + favouriteCount + "x)");

        // instanceof + casting to reach the cardio-only field
        double totalKm = 0;
        for (Workout w : workouts) {
            if (w instanceof CardioWorkout) {
                CardioWorkout cardio = (CardioWorkout) w;
                totalKm += cardio.getDistanceKm();
            }
        }
        System.out.printf("Cardio distance    : %.1f km%n", totalKm);

        Workout longest = null;
        for (Workout w : workouts) {
            if (longest == null || w.compareTo(longest) > 0) {
                longest = w;
            }
        }
        System.out.println("Longest workout    : " + longest);

        System.out.println("Records in " + FILE_NAME + " : " + countSavedRecords());
        System.out.println("Workout objects created this run: " + Workout.getTotalCreated());
    }


    // ======================= File handling (java.nio.file) ================

    // read the saved workouts when the program starts
    static void loadFromFile() {
        if (!Files.exists(SAVE_FILE)) {
            System.out.println("No saved workouts yet - starting fresh.");
            return;
        }

        int loaded = 0;
        int skipped = 0;

        try (BufferedReader reader = Files.newBufferedReader(SAVE_FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (workouts.size() >= MAX_WORKOUTS) {
                    System.out.println("Workout limit reached - ignoring the rest of the file.");
                    break;
                }
                try {
                    workouts.add(parseRecord(line));
                    loaded++;
                } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                    // NumberFormatException is a kind of IllegalArgumentException,
                    // so bad numbers and bad values both land here
                    skipped++;
                }
            }
            System.out.println("Loaded " + loaded + " workout(s) from " + FILE_NAME
                    + (skipped > 0 ? " (skipped " + skipped + " damaged line(s))" : "") + ".");
        } catch (IOException e) {
            System.out.println("Could not read " + FILE_NAME + ": " + e.getMessage());
        }
    }

    // turns one line like  CARDIO,Running,30,2,5.0  back into an object
    static Workout parseRecord(String line) {
        String[] parts = line.split(",");
        String type = parts[0].trim();
        String name = parts[1].trim();
        int minutes = Integer.parseInt(parts[2].trim());
        int day = Integer.parseInt(parts[3].trim());

        Workout w;
        switch (type) {
            case "CARDIO":
                w = new CardioWorkout(name, minutes, day, Double.parseDouble(parts[4].trim()));
                break;
            case "STRENGTH":
                w = new StrengthWorkout(name, minutes, day,
                        Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()));
                break;
            case "GENERAL":
                w = new Workout(name, minutes, day);
                break;
            default:
                throw new IllegalArgumentException("Unknown workout type: " + type);
        }
        return w;
    }

    // add one record to the end of the file (creates the file if needed)
    static void appendToFile(Workout w) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(SAVE_FILE,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(w.toRecord());
            writer.newLine();
        }
    }

    // rewrite the whole file from the list (used after removing workouts)
    static void saveAllToFile() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(SAVE_FILE)) {
            for (Workout w : workouts) {
                writer.write(w.toRecord());
                writer.newLine();
            }
        }
    }

    // reads the file as a stream of lines and counts the non-empty ones
    static long countSavedRecords() {
        if (!Files.exists(SAVE_FILE)) {
            return 0;
        }
        try (Stream<String> lines = Files.lines(SAVE_FILE)) {
            return lines.filter(line -> !line.trim().isEmpty()).count();
        } catch (IOException e) {
            return 0;
        }
    }
}


// =======================================================================
//  Classes below: object-oriented part (interface, inheritance, exceptions)
// =======================================================================

// anything that can describe itself in one line
interface Reportable {
    String summary();
}


// our own checked exception for bad user input
class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}


class Workout implements Reportable, Comparable<Workout> {

    static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private static final double CALORIES_PER_MINUTE = 5.0;

    // shared by every Workout object
    private static int totalCreated = 0;

    private String name;
    private int minutes;
    private int day;                          // 1 = Mon ... 7 = Sun

    public Workout(String name, int minutes, int day) {
        setName(name);
        setMinutes(minutes);
        setDay(day);
        totalCreated++;
    }

    public String getName() {
        return name;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getDay() {
        return day;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Workout name cannot be empty");
        }
        this.name = name.trim();
    }

    public void setMinutes(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be positive");
        }
        this.minutes = minutes;
    }

    public void setDay(int day) {
        if (day < 1 || day > 7) {
            throw new IllegalArgumentException("Day must be between 1 and 7");
        }
        this.day = day;
    }

    public String getType() {
        return "General";
    }

    public double caloriesBurned() {
        return minutes * CALORIES_PER_MINUTE;
    }

    // one line of the save file, e.g.  GENERAL,Yoga,45,3
    public String toRecord() {
        return String.join(",", getType().toUpperCase(), name,
                String.valueOf(minutes), String.valueOf(day));
    }

    public static int getTotalCreated() {
        return totalCreated;
    }

    @Override
    public String summary() {
        return toString() + " | " + DAY_NAMES[day - 1] + " | " + getType()
                + " | ~" + (int) caloriesBurned() + " kcal";
    }

    // shorter workouts come first
    @Override
    public int compareTo(Workout other) {
        return Integer.compare(this.minutes, other.minutes);
    }

    @Override
    public String toString() {
        return name + " (" + minutes + " min)";
    }
}


class CardioWorkout extends Workout {

    private static final double CALORIES_PER_MINUTE = 8.0;

    private double distanceKm;

    public CardioWorkout(String name, int minutes, int day, double distanceKm) {
        super(name, minutes, day);
        setDistanceKm(distanceKm);
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        this.distanceKm = distanceKm;
    }

    @Override
    public String getType() {
        return "Cardio";
    }

    @Override
    public double caloriesBurned() {
        return getMinutes() * CALORIES_PER_MINUTE;
    }

    @Override
    public String toRecord() {
        return super.toRecord() + "," + distanceKm;
    }

    @Override
    public String toString() {
        return super.toString() + " " + distanceKm + " km";
    }
}


class StrengthWorkout extends Workout {

    private static final double CALORIES_PER_MINUTE = 6.0;
    private static final double CALORIES_PER_REP = 0.5;

    private int sets;
    private int reps;

    public StrengthWorkout(String name, int minutes, int day, int sets, int reps) {
        super(name, minutes, day);
        setSets(sets);
        setReps(reps);
    }

    public int getSets() {
        return sets;
    }

    public int getReps() {
        return reps;
    }

    public void setSets(int sets) {
        if (sets <= 0) {
            throw new IllegalArgumentException("Sets must be positive");
        }
        this.sets = sets;
    }

    public void setReps(int reps) {
        if (reps <= 0) {
            throw new IllegalArgumentException("Reps must be positive");
        }
        this.reps = reps;
    }

    @Override
    public String getType() {
        return "Strength";
    }

    @Override
    public double caloriesBurned() {
        return getMinutes() * CALORIES_PER_MINUTE + sets * reps * CALORIES_PER_REP;
    }

    @Override
    public String toRecord() {
        return super.toRecord() + "," + sets + "," + reps;
    }

    @Override
    public String toString() {
        return super.toString() + " " + sets + "x" + reps;
    }
}


class UserProfile implements Reportable {

    private String name;
    private double weightKg;
    private int heightCm;

    public UserProfile() {
        this("Athlete");
    }

    public UserProfile(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setWeightKg(double weightKg) {
        if (weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        this.weightKg = weightKg;
    }

    public void setHeightCm(int heightCm) {
        if (heightCm <= 0) {
            throw new IllegalArgumentException("Height must be positive");
        }
        this.heightCm = heightCm;
    }

    @Override
    public String summary() {
        String stats = (weightKg > 0) ? weightKg + " kg, " + heightCm + " cm" : "no body stats yet";
        return "Profile: " + name + " (" + stats + ")";
    }
}