package com.zeraki.assessment.analytics;

import java.time.LocalDate;
import java.util.List;

/** Everything the exam pipeline reads. Plain data, no behaviour. */
public final class Model {

    private Model() {
    }

    public record Student(long id, String admissionNumber, String name, int form, String currentStream) {
    }

    /** A student moved from one stream to another, effective from a date. */
    public record StreamMove(long studentId, String fromStream, String toStream, LocalDate effectiveFrom) {
    }

    public record Exam(long id, String name, int term, int year, LocalDate satOn, List<String> subjects) {
    }

    /** One subject score for one student in one exam. Score is out of 100. */
    public record ExamEntry(long examId, long studentId, String subject, Integer score) {
    }

    public record SubjectResult(String subject, Integer score, String grade, double points) {
    }

    public record StudentResult(
            long studentId,
            String admissionNumber,
            String name,
            String stream,
            List<SubjectResult> subjects,
            double meanScore,
            double meanPoints,
            String meanGrade,
            int positionInStream,
            int positionOverall) {
    }

    public record StreamSummary(
            String stream,
            int entries,
            double meanScore,
            double meanPoints,
            String meanGrade,
            int rank) {
    }

    public record ExamReport(
            long examId,
            String examName,
            String gradingSystem,
            List<StudentResult> students,
            List<StreamSummary> streams,
            double schoolMeanScore,
            String schoolMeanGrade) {
    }
}
