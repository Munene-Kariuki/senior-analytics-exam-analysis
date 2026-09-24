package com.zeraki.assessment.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The only tests that exist today. They assert that it runs.
 */
class ExamAnalysisServiceTest {

    private ExamAnalysisService service;

    @BeforeEach
    void setUp() {
        ExamAnalysisService.clearCache();
        service = new ExamAnalysisService(Fixtures.school());
    }

    @Test
    void producesAReport() {
        Model.ExamReport report = service.analyse(9001L, "KCSE");

        assertThat(report.students()).isNotEmpty();
        assertThat(report.streams()).isNotEmpty();
    }

    @Test
    void producesReportText() {
        assertThat(service.reportText(9001L, "KCSE")).contains("EXAM ANALYSIS");
    }

    @Test
    void producesCsv() {
        assertThat(service.exportCsv(9001L, "KCSE")).startsWith("Position,Admission,Name,Stream");
    }
}
