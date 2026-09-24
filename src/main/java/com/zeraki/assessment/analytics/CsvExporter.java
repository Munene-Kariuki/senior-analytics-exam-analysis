package com.zeraki.assessment.analytics;

/**
 * Renders an exam report as CSV. The subject columns come from Fixtures.SUBJECTS rather than
 * the exam's own subject list kept as-was. Captured in NOTES.md
 */
class CsvExporter {

    String export(Model.ExamReport report) {
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
}
