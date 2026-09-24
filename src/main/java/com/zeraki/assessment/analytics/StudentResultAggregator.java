package com.zeraki.assessment.analytics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Turns one student's raw exam entries into a StudentResult: per-subject grades/points and
 * the student's mean score/points/grade. Ranking is not this class's job; positions are left
 * at 0, as they were before extraction, for RankAssigner to fill in.
 */
class StudentResultAggregator {

    private final BiFunction<Integer, String, String> gradeFor;
    private final BiFunction<Integer, String, Double> pointsFor;

    StudentResultAggregator(
            BiFunction<Integer, String, String> gradeFor, BiFunction<Integer, String, Double> pointsFor) {
        this.gradeFor = gradeFor;
        this.pointsFor = pointsFor;
    }

    Model.StudentResult aggregate(Model.Student student, List<Model.ExamEntry> entries, String gradingSystem) {
        List<Model.SubjectResult> subjects = new ArrayList<>();
        double totalScore = 0;
        double totalPoints = 0;
        int counted = 0;

        for (Model.ExamEntry entry : entries) {
            if (entry.score() == null) {
                subjects.add(new Model.SubjectResult(entry.subject(), null, "-", 0));
                continue;
            }
            String grade = gradeFor.apply(entry.score(), gradingSystem);
            double points = pointsFor.apply(entry.score(), gradingSystem);
            subjects.add(new Model.SubjectResult(entry.subject(), entry.score(), grade, points));
            totalScore = totalScore + entry.score();
            totalPoints = totalPoints + points;
            counted = counted + 1;
        }

        double meanScore = counted == 0 ? 0 : totalScore / counted;
        double meanPoints = counted == 0 ? 0 : totalPoints / counted;
        meanScore = Math.round(meanScore * 100.0) / 100.0;
        meanPoints = Math.round(meanPoints * 100.0) / 100.0;

        String meanGrade = gradeFor.apply((int) Math.round(meanScore), gradingSystem);

        return new Model.StudentResult(
                student.id(),
                student.admissionNumber(),
                student.name(),
                student.currentStream(),
                subjects,
                meanScore,
                meanPoints,
                meanGrade,
                0,
                0);
    }
}
