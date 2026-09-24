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
    private final TextReportRenderer textReportRenderer = new TextReportRenderer();
    private final CsvExporter csvExporter = new CsvExporter();
    private final StudentSlipRenderer studentSlipRenderer = new StudentSlipRenderer();

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
        return textReportRenderer.render(report);
    }

    public String exportCsv(long examId, String gradingSystem) {
        Model.ExamReport report = analyse(examId, gradingSystem);
        return csvExporter.export(report);
    }

    public String studentSlip(long examId, long studentId, String gradingSystem) {
        Model.ExamReport report = analyse(examId, gradingSystem);
        return studentSlipRenderer.render(report, studentId);
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
