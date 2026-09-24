package com.zeraki.assessment.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Produces exam analysis for a school: per student, per stream, and for the school overall,
 * in whichever grading system the school has configured.
 *
 * This class has grown with the product. Everything it does today is the specification.
 */
public class ExamAnalysisService {

    // Shared across every instance and every request. Never invalidated.
    private static final Map<Long, Model.ExamReport> CACHE = new HashMap<>();

    private final InMemoryRepository repository;

    public ExamAnalysisService(InMemoryRepository repository) {
        this.repository = repository;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    //  public API

    public Model.ExamReport analyse(long examId, String gradingSystem) {
        if (CACHE.containsKey(examId)) {
            return CACHE.get(examId);
        }

        Model.Exam exam = repository.findExam(examId);
        if (exam == null) {
            throw new IllegalArgumentException("No exam with id " + examId);
        }

        List<Model.Student> allStudents = repository.findAllStudents();
        List<Model.StudentResult> results = new ArrayList<>();

        for (Model.Student student : allStudents) {
            List<Model.ExamEntry> entries = repository.findEntriesForStudent(examId, student.id());
            if (entries.isEmpty()) {
                continue;
            }

            List<Model.SubjectResult> subjects = new ArrayList<>();
            double totalScore = 0;
            double totalPoints = 0;
            int counted = 0;

            for (Model.ExamEntry entry : entries) {
                if (entry.score() == null) {
                    subjects.add(new Model.SubjectResult(entry.subject(), null, "-", 0));
                    continue;
                }
                String grade = gradeFor(entry.score(), gradingSystem);
                double points = pointsFor(entry.score(), gradingSystem);
                subjects.add(new Model.SubjectResult(entry.subject(), entry.score(), grade, points));
                totalScore = totalScore + entry.score();
                totalPoints = totalPoints + points;
                counted = counted + 1;
            }

            double meanScore = counted == 0 ? 0 : totalScore / counted;
            double meanPoints = counted == 0 ? 0 : totalPoints / counted;
            meanScore = Math.round(meanScore * 100.0) / 100.0;
            meanPoints = Math.round(meanPoints * 100.0) / 100.0;

            String meanGrade = gradeFor((int) Math.round(meanScore), gradingSystem);

            results.add(new Model.StudentResult(
                    student.id(),
                    student.admissionNumber(),
                    student.name(),
                    student.currentStream(),
                    subjects,
                    meanScore,
                    meanPoints,
                    meanGrade,
                    0,
                    0));
        }

        results.sort(Comparator.comparingDouble(Model.StudentResult::meanScore).reversed());

        List<Model.StudentResult> ranked = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Model.StudentResult r = results.get(i);

            int overall = i + 1;
            if (i > 0 && results.get(i - 1).meanScore() == r.meanScore()) {
                overall = ranked.get(i - 1).positionOverall();
            }

            int inStream = 1;
            for (Model.StudentResult other : results) {
                if (other.stream().equals(r.stream()) && other.meanScore() > r.meanScore()) {
                    inStream = inStream + 1;
                }
            }

            ranked.add(new Model.StudentResult(
                    r.studentId(),
                    r.admissionNumber(),
                    r.name(),
                    r.stream(),
                    r.subjects(),
                    r.meanScore(),
                    r.meanPoints(),
                    r.meanGrade(),
                    inStream,
                    overall));
        }

        Map<String, List<Model.StudentResult>> byStream = new HashMap<>();
        for (Model.StudentResult r : ranked) {
            byStream.computeIfAbsent(r.stream(), k -> new ArrayList<>()).add(r);
        }

        List<Model.StreamSummary> streams = new ArrayList<>();
        for (Map.Entry<String, List<Model.StudentResult>> e : byStream.entrySet()) {
            double sumScore = 0;
            double sumPoints = 0;
            for (Model.StudentResult r : e.getValue()) {
                sumScore = sumScore + r.meanScore();
                sumPoints = sumPoints + r.meanPoints();
            }
            double mean = e.getValue().isEmpty() ? 0 : sumScore / e.getValue().size();
            double points = e.getValue().isEmpty() ? 0 : sumPoints / e.getValue().size();
            mean = Math.round(mean * 100.0) / 100.0;
            points = Math.round(points * 100.0) / 100.0;
            streams.add(new Model.StreamSummary(
                    e.getKey(),
                    e.getValue().size(),
                    mean,
                    points,
                    gradeFor((int) Math.round(mean), gradingSystem),
                    0));
        }

        streams.sort(Comparator.comparingDouble(Model.StreamSummary::meanScore).reversed());
        List<Model.StreamSummary> rankedStreams = new ArrayList<>();
        for (int i = 0; i < streams.size(); i++) {
            Model.StreamSummary s = streams.get(i);
            rankedStreams.add(new Model.StreamSummary(
                    s.stream(), s.entries(), s.meanScore(), s.meanPoints(), s.meanGrade(), i + 1));
        }

        double schoolTotal = 0;
        for (Model.StudentResult r : ranked) {
            schoolTotal = schoolTotal + r.meanScore();
        }
        double schoolMean = ranked.isEmpty() ? 0 : schoolTotal / ranked.size();
        schoolMean = Math.round(schoolMean * 100.0) / 100.0;

        Model.ExamReport report = new Model.ExamReport(
                exam.id(),
                exam.name(),
                gradingSystem,
                ranked,
                rankedStreams,
                schoolMean,
                gradeFor((int) Math.round(schoolMean), gradingSystem));

        CACHE.put(examId, report);
        return report;
    }

    public String reportText(long examId, String gradingSystem) {
        Model.ExamReport report = analyse(examId, gradingSystem);

        String out = "";
        out = out + "EXAM ANALYSIS\n";
        out = out + "=============\n";
        out = out + report.examName() + "\n";
        out = out + "Grading: " + report.gradingSystem() + "\n";
        out = out + "Candidates: " + report.students().size() + "\n";
        out = out + "School mean: " + report.schoolMeanScore() + " (" + report.schoolMeanGrade() + ")\n";
        out = out + "\n";

        out = out + "STREAMS\n";
        out = out + "-------\n";
        for (Model.StreamSummary s : report.streams()) {
            String line = s.rank() + ". " + s.stream();
            while (line.length() < 16) {
                line = line + " ";
            }
            line = line + s.entries() + " candidates";
            while (line.length() < 34) {
                line = line + " ";
            }
            line = line + "mean " + s.meanScore() + " (" + s.meanGrade() + ")";
            out = out + line + "\n";
        }
        out = out + "\n";

        out = out + "TOP 10\n";
        out = out + "------\n";
        int shown = 0;
        for (Model.StudentResult r : report.students()) {
            if (shown >= 10) {
                break;
            }
            String line = r.positionOverall() + ". " + r.name();
            while (line.length() < 30) {
                line = line + " ";
            }
            line = line + r.stream();
            while (line.length() < 40) {
                line = line + " ";
            }
            line = line + r.meanScore() + " (" + r.meanGrade() + ")";
            out = out + line + "\n";
            shown = shown + 1;
        }
        out = out + "\n";

        out = out + "BOTTOM 5\n";
        out = out + "--------\n";
        List<Model.StudentResult> students = report.students();
        for (int i = Math.max(0, students.size() - 5); i < students.size(); i++) {
            Model.StudentResult r = students.get(i);
            String line = r.positionOverall() + ". " + r.name();
            while (line.length() < 30) {
                line = line + " ";
            }
            line = line + r.stream();
            while (line.length() < 40) {
                line = line + " ";
            }
            line = line + r.meanScore() + " (" + r.meanGrade() + ")";
            out = out + line + "\n";
        }

        return out;
    }

    public String exportCsv(long examId, String gradingSystem) {
        Model.ExamReport report = analyse(examId, gradingSystem);

        String header = "Position,Admission,Name,Stream";
        for (String subject : Fixtures.SUBJECTS) {
            header = header + "," + subject;
        }
        header = header + ",Mean Score,Mean Grade,Stream Position";

        String csv = header + "\n";
        for (Model.StudentResult r : report.students()) {
            String row = r.positionOverall() + "," + r.admissionNumber() + "," + r.name() + "," + r.stream();
            for (String subject : Fixtures.SUBJECTS) {
                String cell = "";
                for (Model.SubjectResult s : r.subjects()) {
                    if (s.subject().equals(subject)) {
                        cell = s.score() == null ? "" : String.valueOf(s.score());
                    }
                }
                row = row + "," + cell;
            }
            row = row + "," + r.meanScore() + "," + r.meanGrade() + "," + r.positionInStream();
            csv = csv + row + "\n";
        }
        return csv;
    }

    public String studentSlip(long examId, long studentId, String gradingSystem) {
        Model.ExamReport report = analyse(examId, gradingSystem);

        Model.StudentResult found = null;
        for (Model.StudentResult r : report.students()) {
            if (r.studentId() == studentId) {
                found = r;
            }
        }
        if (found == null) {
            return "No results for student " + studentId;
        }

        String out = "";
        out = out + found.name() + " (" + found.admissionNumber() + ")\n";
        out = out + report.examName() + " - " + found.stream() + "\n";
        out = out + "\n";
        for (Model.SubjectResult s : found.subjects()) {
            String line = s.subject();
            while (line.length() < 16) {
                line = line + " ";
            }
            line = line + (s.score() == null ? "ABS" : String.valueOf(s.score()));
            while (line.length() < 22) {
                line = line + " ";
            }
            line = line + s.grade();
            out = out + line + "\n";
        }
        out = out + "\n";
        out = out + "Mean " + found.meanScore() + " (" + found.meanGrade() + ")\n";
        out = out + "Position " + found.positionOverall() + " overall, "
                + found.positionInStream() + " in " + found.stream() + "\n";

        if (found.meanScore() >= 70) {
            out = out + "Remark: Excellent work, keep it up.\n";
        } else if (found.meanScore() >= 55) {
            out = out + "Remark: Good effort, aim higher next term.\n";
        } else if (found.meanScore() >= 40) {
            out = out + "Remark: Average. More consistent revision needed.\n";
        } else {
            out = out + "Remark: Below expectation. See the class teacher.\n";
        }

        return out;
    }

    //  grading

    public String gradeFor(int score, String gradingSystem) {
        if (gradingSystem == null) {
            throw new IllegalArgumentException("Grading system is required");
        }

        if (gradingSystem.equals("KCSE")) {
            if (score >= 80) {
                return "A";
            } else if (score >= 75) {
                return "A-";
            } else if (score >= 70) {
                return "B+";
            } else if (score >= 65) {
                return "B";
            } else if (score >= 60) {
                return "B-";
            } else if (score >= 55) {
                return "C+";
            } else if (score >= 50) {
                return "C";
            } else if (score >= 45) {
                return "C-";
            } else if (score >= 40) {
                return "D+";
            } else if (score >= 35) {
                return "D";
            } else if (score >= 30) {
                return "D-";
            } else {
                return "E";
            }
        } else if (gradingSystem.equals("KCPE")) {
            if (score >= 80) {
                return "Above 80";
            } else if (score >= 70) {
                return "70 - 79";
            } else if (score >= 60) {
                return "60 - 69";
            } else if (score >= 50) {
                return "50 - 59";
            } else if (score >= 40) {
                return "40 - 49";
            } else if (score >= 30) {
                return "30 - 39";
            } else {
                return "Below 30";
            }
        } else if (gradingSystem.equals("CBC")) {
            if (score >= 76) {
                return "EE";
            } else if (score >= 51) {
                return "ME";
            } else if (score >= 26) {
                return "AE";
            } else {
                return "BE";
            }
        } else if (gradingSystem.equals("IGCSE")) {
            if (score >= 90) {
                return "A*";
            } else if (score >= 80) {
                return "A";
            } else if (score >= 70) {
                return "B";
            } else if (score >= 60) {
                return "C";
            } else if (score >= 50) {
                return "D";
            } else if (score >= 40) {
                return "E";
            } else if (score >= 30) {
                return "F";
            } else if (score >= 20) {
                return "G";
            } else {
                return "U";
            }
        } else {
            throw new IllegalArgumentException("Unknown grading system " + gradingSystem);
        }
    }

    public double pointsFor(int score, String gradingSystem) {
        if (gradingSystem.equals("KCSE")) {
            if (score >= 80) {
                return 12;
            } else if (score >= 75) {
                return 11;
            } else if (score >= 70) {
                return 10;
            } else if (score >= 65) {
                return 9;
            } else if (score >= 60) {
                return 8;
            } else if (score >= 55) {
                return 7;
            } else if (score >= 50) {
                return 6;
            } else if (score >= 45) {
                return 5;
            } else if (score >= 40) {
                return 4;
            } else if (score >= 35) {
                return 3;
            } else if (score >= 30) {
                return 2;
            } else {
                return 1;
            }
        } else if (gradingSystem.equals("KCPE")) {
            return score;
        } else if (gradingSystem.equals("CBC")) {
            if (score >= 76) {
                return 4;
            } else if (score >= 51) {
                return 3;
            } else if (score >= 26) {
                return 2;
            } else {
                return 1;
            }
        } else if (gradingSystem.equals("IGCSE")) {
            if (score >= 90) {
                return 9;
            } else if (score >= 80) {
                return 8;
            } else if (score >= 70) {
                return 7;
            } else if (score >= 60) {
                return 6;
            } else if (score >= 50) {
                return 5;
            } else if (score >= 40) {
                return 4;
            } else if (score >= 30) {
                return 3;
            } else if (score >= 20) {
                return 2;
            } else {
                return 1;
            }
        } else {
            throw new IllegalArgumentException("Unknown grading system " + gradingSystem);
        }
    }

    //  streams

    /**
     * The stream a student sat an exam in, which is not always the stream they are in now.
     * Written when stream transfers were added. Kept for the printed report.
     */
    public String streamAtExam(long studentId, long examId) {
        Model.Student student = repository.findStudent(studentId);
        Model.Exam exam = repository.findExam(examId);
        if (student == null || exam == null) {
            return "";
        }
        List<Model.StreamMove> moves = repository.findStreamMoves(studentId);
        String stream = student.currentStream();
        for (Model.StreamMove move : moves) {
            if (move.effectiveFrom().isAfter(exam.satOn())) {
                stream = move.fromStream();
            }
        }
        return stream;
    }
}
