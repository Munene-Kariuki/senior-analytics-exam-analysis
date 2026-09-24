package com.zeraki.assessment.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeded school data. Deterministic: the same seed produces the same scores every run,
 * so tests written against it are stable.
 */
public final class Fixtures {

    public static final List<String> SUBJECTS =
            List.of("Mathematics", "English", "Kiswahili", "Biology", "Chemistry", "Physics", "History", "Geography");

    private static final String[] STREAMS = {"North", "South", "East", "West"};

    private static final String[] FIRST_NAMES = {
        "Achieng", "Baraka", "Chebet", "Dalmas", "Esther", "Faith", "Gideon", "Hawa",
        "Imani", "Juma", "Kamau", "Lydia", "Mwangi", "Njeri", "Omondi", "Pauline",
        "Quinter", "Rotich", "Sifa", "Tabitha", "Upendo", "Vincent", "Wanjiku", "Zawadi"
    };

    private static final String[] LAST_NAMES = {
        "Otieno", "Mutua", "Wafula", "Kiprop", "Njoroge", "Adhiambo", "Barasa", "Cherono",
        "Kilonzo", "Maina", "Odhiambo", "Wekesa"
    };

    private Fixtures() {
    }

    /** One school, four streams, two exams in the same term. */
    public static InMemoryRepository school() {
        Random random = new Random(20260922L);

        List<Model.Student> students = new ArrayList<>();
        for (int i = 0; i < 160; i++) {
            long id = 1000L + i;
            String name = FIRST_NAMES[i % FIRST_NAMES.length] + " " + LAST_NAMES[(i / 3) % LAST_NAMES.length];
            String admission = "ADM" + (2400 + i);
            String stream = STREAMS[i % STREAMS.length];
            students.add(new Model.Student(id, admission, name, 3, stream));
        }

        // Fourteen students transferred stream part way through the term. Their entries
        // were sat while they were still in the old stream.
        List<Model.StreamMove> moves = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            Model.Student s = students.get(i * 11);
            String to = STREAMS[(indexOfStream(s.currentStream()) + 1) % STREAMS.length];
            moves.add(new Model.StreamMove(s.id(), s.currentStream(), to, LocalDate.of(2026, 6, 10)));
            students.set(i * 11, new Model.Student(s.id(), s.admissionNumber(), s.name(), s.form(), to));
        }

        Model.Exam opener = new Model.Exam(9001L, "Term 2 Opener", 2, 2026, LocalDate.of(2026, 5, 12), SUBJECTS);
        Model.Exam midTerm = new Model.Exam(9002L, "Term 2 Mid Term", 2, 2026, LocalDate.of(2026, 6, 24), SUBJECTS);

        List<Model.ExamEntry> entries = new ArrayList<>();
        for (Model.Exam exam : List.of(opener, midTerm)) {
            for (Model.Student s : students) {
                for (String subject : SUBJECTS) {
                    // A few entries are genuinely missing; that is real and intentional.
                    if (random.nextInt(50) == 0) {
                        entries.add(new Model.ExamEntry(exam.id(), s.id(), subject, null));
                        continue;
                    }
                    int base = 30 + random.nextInt(60);
                    if (subject.equals("Mathematics")) {
                        base = base - 6;
                    }
                    if (subject.equals("English")) {
                        base = base + 4;
                    }
                    int score = Math.max(0, Math.min(100, base));
                    entries.add(new Model.ExamEntry(exam.id(), s.id(), subject, score));
                }
            }
        }

        return new InMemoryRepository(students, moves, List.of(opener, midTerm), entries);
    }

    private static int indexOfStream(String stream) {
        for (int i = 0; i < STREAMS.length; i++) {
            if (STREAMS[i].equals(stream)) {
                return i;
            }
        }
        return 0;
    }
}
