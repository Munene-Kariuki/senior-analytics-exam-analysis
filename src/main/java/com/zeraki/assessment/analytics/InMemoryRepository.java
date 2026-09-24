package com.zeraki.assessment.analytics;

import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the database. No JDBC, no container, no network.
 * Treat these methods as if each one were a query you would rather not repeat.
 */
public class InMemoryRepository {

    private final List<Model.Student> students;
    private final List<Model.StreamMove> streamMoves;
    private final List<Model.Exam> exams;
    private final List<Model.ExamEntry> entries;

    private int queryCount = 0;

    public InMemoryRepository(
            List<Model.Student> students,
            List<Model.StreamMove> streamMoves,
            List<Model.Exam> exams,
            List<Model.ExamEntry> entries) {
        this.students = students;
        this.streamMoves = streamMoves;
        this.exams = exams;
        this.entries = entries;
    }

    public int queryCount() {
        return queryCount;
    }

    public void resetQueryCount() {
        queryCount = 0;
    }

    public Model.Exam findExam(long examId) {
        queryCount++;
        for (Model.Exam e : exams) {
            if (e.id() == examId) {
                return e;
            }
        }
        return null;
    }

    public List<Model.Student> findAllStudents() {
        queryCount++;
        return new ArrayList<>(students);
    }

    public Model.Student findStudent(long studentId) {
        queryCount++;
        for (Model.Student s : students) {
            if (s.id() == studentId) {
                return s;
            }
        }
        return null;
    }

    public List<Model.ExamEntry> findEntriesForExam(long examId) {
        queryCount++;
        List<Model.ExamEntry> out = new ArrayList<>();
        for (Model.ExamEntry e : entries) {
            if (e.examId() == examId) {
                out.add(e);
            }
        }
        return out;
    }

    public List<Model.ExamEntry> findEntriesForStudent(long examId, long studentId) {
        queryCount++;
        List<Model.ExamEntry> out = new ArrayList<>();
        for (Model.ExamEntry e : entries) {
            if (e.examId() == examId && e.studentId() == studentId) {
                out.add(e);
            }
        }
        return out;
    }

    public List<Model.StreamMove> findStreamMoves(long studentId) {
        queryCount++;
        List<Model.StreamMove> out = new ArrayList<>();
        for (Model.StreamMove m : streamMoves) {
            if (m.studentId() == studentId) {
                out.add(m);
            }
        }
        return out;
    }
}
