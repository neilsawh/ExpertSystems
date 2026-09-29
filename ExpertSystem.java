import java.util.*;
import java.util.function.*;

public class ExpertSystem {
    static final List<String> DEGREES = Arrays.asList("None", "Associates", "Bachelors", "Masters", "Doctorate");
    static final Scanner IN = new Scanner(System.in);
    static final Map<String, Object> facts = new HashMap<>();

    static class Cond {
        String kind, text; Predicate<Map<String, Object>> test; Function<Map<String, Object>, String> got;
        boolean ok;
        Cond(String text, Predicate<Map<String, Object>> t, Function<Map<String, Object>, String> g) { this.text = text; test = t; got = g; }
    }
    static class Position {
        String name; List<Cond> need = new ArrayList<>(), want = new ArrayList<>(), qual = new ArrayList<>();
        Position(String n) { name = n; }
    }
    static double num(Map<String, Object> f, String k) { return (Double) f.get(k); }
    static boolean flag(Map<String, Object> f, String k) { return (Boolean) f.get(k); }
    static String fmt(double d) { return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d); }

    static Cond yrs(String k, double n, String label) {
        return new Cond(fmt(n) + "+ years " + label, f -> num(f, k) >= n, f -> fmt(num(f, k)) + " year(s)");
    }
    static Cond yes(String k, String label) { return new Cond(label, f -> flag(f, k), f -> "no"); }
    static Cond bachCS() { return new Cond("Bachelor in Computer Science", f -> flag(f, "bachelorCS"), f -> f.get("degree") + " in " + f.get("field")); }

    static List<Position> buildKB() {
        Position a = new Position("Entry-Level Python Engineer");
        a.need.add(yes("pyCourse", "Python course work")); a.need.add(yes("seCourse", "Software Engineering course work"));
        a.want.add(yes("agileCourse", "Agile course")); a.qual.add(bachCS());

        Position b = new Position("Python Engineer");
        b.need.add(yrs("pyYears", 3, "Python development")); b.need.add(yrs("dataYears", 1, "data development"));
        b.need.add(new Cond("Experience in Agile projects", f -> num(f, "agileYears") > 0, f -> "0 years"));
        b.want.add(yes("git", "Used Git")); b.qual.add(bachCS());

        Position c = new Position("Project Manager");
        c.need.add(yrs("pmYears", 3, "managing software projects")); c.need.add(yrs("agileYears", 2, "in Agile projects"));
        c.qual.add(yes("pmi", "PMI Lean Project Management Certification"));

        Position d = new Position("Senior Knowledge Engineer");
        d.need.add(yrs("pyYears", 4, "using Python to develop")); d.need.add(yrs("esYears", 2, "developing Expert Systems"));
        d.need.add(yrs("archYears", 2, "data architecture and data development"));
        d.qual.add(new Cond("Master's in Computer Science", f -> flag(f, "mastersCS"), f -> f.get("degree") + " in " + f.get("field")));
        return Arrays.asList(a, b, c, d);
    }

    static String readLine() {
        if (!IN.hasNextLine()) { System.out.println("\nNo more input. Exiting."); System.exit(1); }
        return IN.nextLine().trim();
    }
    static String askEnum(String text, String... vals) {
        while (true) {
            System.out.print(text + " [" + String.join(" / ", vals) + "]: ");
            String s = readLine();
            for (String v : vals) if (v.equalsIgnoreCase(s)) return v;
            System.out.println("  INVALID: '" + s + "'. Enter exactly one of: " + String.join(", ", vals) + ".");
        }
    }
    static boolean askYN(String text) {
        while (true) {
            System.out.print(text + " [yes / no]: ");
            String s = readLine().toLowerCase();
            if (s.equals("yes")) return true;
            if (s.equals("no")) return false;
            System.out.println("  INVALID: please enter exactly 'yes' or 'no'.");
        }
    }
    static double askNum(String text) {
        while (true) {
            System.out.print(text + " (years, 0 if none): ");
            String s = readLine();
            if (s.matches("\\d+(\\.\\d+)?") && Double.parseDouble(s) <= 60) return Double.parseDouble(s);
            System.out.println("  INVALID: enter a non-negative number up to 60 (e.g. 3 or 2.5).");
        }
    }

    static void collect() {
        System.out.println("Answer each question exactly as shown in the brackets.\n");
        String deg = askEnum("Highest degree earned", DEGREES.toArray(new String[0]));
        facts.put("degree", deg);
        facts.put("field", deg.equals("None") ? "Other" : askEnum("Field of that degree", "Computer Science", "Other"));
        facts.put("pyCourse", askYN("Completed Python course work?"));
        facts.put("seCourse", askYN("Completed Software Engineering course work?"));
        facts.put("agileCourse", askYN("Completed an Agile course?"));
        facts.put("pyYears", askNum("Python development experience"));
        facts.put("dataYears", askNum("Data development experience"));
        facts.put("agileYears", askNum("Experience on Agile projects"));
        facts.put("git", askYN("Have you used Git?"));
        facts.put("pmYears", askNum("Managing software projects"));
        facts.put("pmi", askYN("Hold the PMI Lean Project Management Certification?"));
        facts.put("esYears", askNum("Developing Expert Systems"));
        facts.put("archYears", askNum("Data architecture and data development"));
    }

    static void deriveFacts() {
        boolean cs = "Computer Science".equals(facts.get("field"));
        int lvl = DEGREES.indexOf((String) facts.get("degree"));
        facts.put("bachelorCS", lvl >= 2 && cs);
        facts.put("mastersCS", lvl >= 3 && cs);
    }
    static boolean evaluate(Position p) {
        boolean qualified = true;
        for (Cond c : p.need) { c.kind = "Needed skill"; c.ok = c.test.test(facts); qualified &= c.ok; }
        for (Cond c : p.qual) { c.kind = "Qualification"; c.ok = c.test.test(facts); qualified &= c.ok; }
        for (Cond c : p.want) { c.kind = "Desired skill"; c.ok = c.test.test(facts); }
        return qualified;
    }

    public static void main(String[] args) {
        System.out.println("=== Job Qualification Expert System ===");
        collect();
        deriveFacts();
        List<Position> kb = buildKB();
        List<Position> yes = new ArrayList<>(), no = new ArrayList<>();
        for (Position p : kb) (evaluate(p) ? yes : no).add(p);

        System.out.println("\n=========== RESULTS ===========");
        System.out.println("\nPOSITIONS YOU ARE QUALIFIED FOR:");
        if (yes.isEmpty()) System.out.println("  (none)");
        for (Position p : yes) {
            System.out.println("\n  * " + p.name);
            for (Cond c : p.need) System.out.println("      [met] Needed skill: " + c.text);
            for (Cond c : p.qual) System.out.println("      [met] Qualification: " + c.text);
            for (Cond c : p.want) System.out.println("      " + (c.ok ? "[bonus] Desired skill you have: " : "[not required] Desired skill you lack: ") + c.text);
        }
        System.out.println("\nPOSITIONS YOU ARE NOT QUALIFIED FOR:");
        if (no.isEmpty()) System.out.println("  (none - you qualify for every position!)");
        for (Position p : no) {
            System.out.println("\n  * " + p.name + "  -- disqualified because:");
            for (Cond c : p.need) if (!c.ok) System.out.println("      - Needed skill not met: " + c.text + " (you have: " + c.got.apply(facts) + ")");
            for (Cond c : p.qual) if (!c.ok) System.out.println("      - Qualification not met: " + c.text + " (you have: " + c.got.apply(facts) + ")");
        }
    }
}