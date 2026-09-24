package com.zeraki.assessment.analytics;

import java.util.ArrayList;
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
    private final StudentResultAggregator studentResultAggregator =
            new StudentResultAggregator(this::gradeFor, this::pointsFor);
    private final RankAssigner rankAssigner = new RankAssigner();
    private final StreamSummaryBuilder streamSummaryBuilder = new StreamSummaryBuilder(this::gradeFor);

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
            results.add(studentResultAggregator.aggregate(student, entries, gradingSystem));
        }

        List<Model.StudentResult> ranked = rankAssigner.assignRanks(results);
        List<Model.StreamSummary> rankedStreams = streamSummaryBuilder.build(ranked, gradingSystem);

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
        return GradingSystems.forName(gradingSystem).grade(score);
    }

    public double pointsFor(int score, String gradingSystem) {
        return GradingSystems.forName(gradingSystem).points(score);
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
