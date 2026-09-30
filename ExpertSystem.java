import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class ExpertSystem {
    static final List<String> DEGREES = Arrays.asList("None", "Associates", "Bachelors", "Masters", "Doctorate");
    static final Map<String, Object> facts = new HashMap<>();

    static class Cond {
        String text; Predicate<Map<String, Object>> test; Function<Map<String, Object>, String> got;
        boolean ok;
        Cond(String text, Predicate<Map<String, Object>> t, Function<Map<String, Object>, String> g) { this.text = text; test = t; got = g; }
    }
    static class Position {
        String name; List<Cond> need = new ArrayList<>(), want = new ArrayList<>(), qual = new ArrayList<>();
        Position(String n) { name = n; }
    }
    static class Q {
        String id, type, text, hint; String[] vals; JTextField field; JLabel err;
        Q(String id, String type, String text, String hint, String... vals) { this.id = id; this.type = type; this.text = text; this.hint = hint; this.vals = vals; }
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

    static final List<Q> QS = Arrays.asList(
            new Q("degree", "enum", "Highest degree earned", "Enter exactly one of: None, Associates, Bachelors, Masters, Doctorate", "None", "Associates", "Bachelors", "Masters", "Doctorate"),
            new Q("field", "enum", "Field of that degree (skipped if degree is None)", "Enter exactly: Computer Science or Other", "Computer Science", "Other"),
            new Q("pyCourse", "yn", "Completed Python course work?", "Enter yes or no"),
            new Q("seCourse", "yn", "Completed Software Engineering course work?", "Enter yes or no"),
            new Q("agileCourse", "yn", "Completed an Agile course?", "Enter yes or no"),
            new Q("pyYears", "num", "Years of Python development experience", "Number of years (0 if none)"),
            new Q("dataYears", "num", "Years of data development experience", "Number of years (0 if none)"),
            new Q("agileYears", "num", "Years of experience on Agile projects", "Number of years (0 if none)"),
            new Q("git", "yn", "Have you used Git?", "Enter yes or no"),
            new Q("pmYears", "num", "Years managing software projects", "Number of years (0 if none)"),
            new Q("pmi", "yn", "Hold the PMI Lean Project Management Certification?", "Enter yes or no"),
            new Q("esYears", "num", "Years developing Expert Systems", "Number of years (0 if none)"),
            new Q("archYears", "num", "Years of data architecture and data development", "Number of years (0 if none)")
    );

    static String check(Q q, String s) {
        if (s.isEmpty()) return "Required. " + q.hint + ".";
        switch (q.type) {
            case "yn":
                if (s.equalsIgnoreCase("yes")) { facts.put(q.id, true); return null; }
                if (s.equalsIgnoreCase("no")) { facts.put(q.id, false); return null; }
                return "Enter exactly 'yes' or 'no'.";
            case "num":
                if (!s.matches("\\d+(\\.\\d+)?") || Double.parseDouble(s) > 60) return "Enter a non-negative number up to 60 (e.g. 3 or 2.5).";
                facts.put(q.id, Double.parseDouble(s));
                return null;
            default:
                for (String v : q.vals) if (v.equalsIgnoreCase(s)) { facts.put(q.id, v); return null; }
                return "'" + s + "' is not valid. Enter exactly one of: " + String.join(", ", q.vals) + ".";
        }
    }

    static void deriveFacts() {
        boolean cs = "Computer Science".equals(facts.get("field"));
        int lvl = DEGREES.indexOf((String) facts.get("degree"));
        facts.put("bachelorCS", lvl >= 2 && cs);
        facts.put("mastersCS", lvl >= 3 && cs);
    }

    static boolean evaluate(Position p) {
        boolean qualified = true;
        for (Cond c : p.need) { c.ok = c.test.test(facts); qualified &= c.ok; }
        for (Cond c : p.qual) { c.ok = c.test.test(facts); qualified &= c.ok; }
        for (Cond c : p.want) c.ok = c.test.test(facts);
        return qualified;
    }

    static String runInference() {
        deriveFacts();
        List<Position> passed = new ArrayList<>(), failed = new ArrayList<>();
        for (Position p : buildKB()) (evaluate(p) ? passed : failed).add(p);
        StringBuilder sb = new StringBuilder("POSITIONS YOU ARE QUALIFIED FOR:\n");
        if (passed.isEmpty()) sb.append("  (none)\n");
        for (Position p : passed) {
            sb.append("\n  * ").append(p.name).append("\n");
            for (Cond c : p.need) sb.append("      [met] Needed skill: ").append(c.text).append("\n");
            for (Cond c : p.qual) sb.append("      [met] Qualification: ").append(c.text).append("\n");
            for (Cond c : p.want) sb.append("      ").append(c.ok ? "[bonus] Desired skill you have: " : "[not required] Desired skill you lack: ").append(c.text).append("\n");
        }
        sb.append("\nPOSITIONS YOU ARE NOT QUALIFIED FOR:\n");
        if (failed.isEmpty()) sb.append("  (none - you qualify for every position!)\n");
        for (Position p : failed) {
            sb.append("\n  * ").append(p.name).append("  -- disqualified because:\n");
            for (Cond c : p.need) if (!c.ok) sb.append("      - Needed skill not met: ").append(c.text).append(" (you have: ").append(c.got.apply(facts)).append(")\n");
            for (Cond c : p.qual) if (!c.ok) sb.append("      - Qualification not met: ").append(c.text).append(" (you have: ").append(c.got.apply(facts)).append(")\n");
        }
        return sb.toString();
    }

    static void createUI() {
        JFrame frame = new JFrame("Job Qualification Expert System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (Q q : QS) {
            JLabel title = new JLabel(q.text);
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            JLabel hint = new JLabel(q.hint);
            hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
            hint.setForeground(Color.GRAY);
            q.field = new JTextField();
            q.field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            q.err = new JLabel(" ");
            q.err.setForeground(new Color(180, 35, 24));
            for (JComponent c : new JComponent[]{title, hint, q.field, q.err}) {
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
                form.add(c);
            }
            form.add(Box.createVerticalStrut(6));
        }
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        JTextArea result = new JTextArea("Fill in every question on the left, then click Evaluate.");
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        result.setMargin(new Insets(10, 10, 10, 10));

        JButton evalBtn = new JButton("Evaluate");
        JButton clearBtn = new JButton("Clear");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(evalBtn);
        buttons.add(clearBtn);

        evalBtn.addActionListener(e -> {
            facts.clear();
            int bad = 0;
            JTextField firstBad = null;
            for (Q q : QS) {
                q.err.setText(" ");
                if (q.id.equals("field") && "None".equals(facts.get("degree"))) { facts.put("field", "Other"); continue; }
                String msg = check(q, q.field.getText().trim());
                if (msg != null) { q.err.setText(msg); bad++; if (firstBad == null) firstBad = q.field; }
            }
            if (bad > 0) {
                result.setText("Please fix " + bad + " invalid entr" + (bad == 1 ? "y" : "ies") + " (shown in red) and click Evaluate again.");
                firstBad.requestFocus();
                return;
            }
            result.setText(runInference());
            result.setCaretPosition(0);
        });
        clearBtn.addActionListener(e -> {
            for (Q q : QS) { q.field.setText(""); q.err.setText(" "); }
            result.setText("Fill in every question on the left, then click Evaluate.");
        });

        JPanel left = new JPanel(new BorderLayout());
        left.add(formScroll, BorderLayout.CENTER);
        left.add(buttons, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, new JScrollPane(result));
        split.setResizeWeight(0.45);
        frame.add(split);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExpertSystem::createUI);
    }
}