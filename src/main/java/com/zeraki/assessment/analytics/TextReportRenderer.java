package com.zeraki.assessment.analytics;

import java.util.List;

/** Renders the human-readable exam analysis report: school summary, streams, top 10, bottom 5. */
class TextReportRenderer {

    String render(Model.ExamReport report) {
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
}
