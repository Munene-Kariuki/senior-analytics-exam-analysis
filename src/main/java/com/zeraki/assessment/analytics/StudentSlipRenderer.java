package com.zeraki.assessment.analytics;

/** Renders one student's result slip: per-subject scores/grades, mean, position, and a remark. */
class StudentSlipRenderer {

    String render(Model.ExamReport report, long studentId) {
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
}
