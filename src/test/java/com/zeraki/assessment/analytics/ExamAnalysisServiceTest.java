package com.zeraki.assessment.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Characterization tests. These pin the CURRENT behaviour of ExamAnalysisService,
 * including quirks that look like defects (see NOTES.md). They exist to make the
 * refactor safe, not to assert what the code "should" do.
 *
 * Values embedded here (scores, ranks, exact text) were captured from the real,
 * unmodified service run against Fixtures.school(), which is deterministic
 * (fixed random seed). If any of these numbers change after a refactor, the
 * refactor changed behaviour.
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

    @Nested
    class GradeForKcse {

        @ParameterizedTest
        @CsvSource({
            "0,E", "29,E", "30,D-", "34,D-", "35,D", "39,D", "40,D+", "44,D+",
            "45,C-", "49,C-", "50,C", "54,C", "55,C+", "59,C+", "60,B-", "64,B-",
            "65,B", "69,B", "70,B+", "74,B+", "75,A-", "79,A-", "80,A", "100,A"
        })
        void gradesAtBoundaries(int score, String expectedGrade) {
            assertThat(service.gradeFor(score, "KCSE")).isEqualTo(expectedGrade);
        }

        @ParameterizedTest
        @CsvSource({
            "0,1", "29,1", "30,2", "34,2", "35,3", "39,3", "40,4", "44,4",
            "45,5", "49,5", "50,6", "54,6", "55,7", "59,7", "60,8", "64,8",
            "65,9", "69,9", "70,10", "74,10", "75,11", "79,11", "80,12", "100,12"
        })
        void pointsAtBoundaries(int score, double expectedPoints) {
            assertThat(service.pointsFor(score, "KCSE")).isEqualTo(expectedPoints);
        }
    }

    @Nested
    class GradeForKcpe {

        @ParameterizedTest
        @CsvSource({
            "0,Below 30", "29,Below 30", "30,30 - 39", "39,30 - 39",
            "40,40 - 49", "49,40 - 49", "50,50 - 59", "59,50 - 59",
            "60,60 - 69", "69,60 - 69", "70,70 - 79", "79,70 - 79",
            "80,Above 80", "100,Above 80"
        })
        void gradesAtBoundaries(int score, String expectedGrade) {
            assertThat(service.gradeFor(score, "KCPE")).isEqualTo(expectedGrade);
        }

        @Test
        void pointsEqualTheRawScore() {
            assertThat(service.pointsFor(0, "KCPE")).isEqualTo(0.0);
            assertThat(service.pointsFor(47, "KCPE")).isEqualTo(47.0);
            assertThat(service.pointsFor(100, "KCPE")).isEqualTo(100.0);
        }
    }

    @Nested
    class GradeForCbc {

        @ParameterizedTest
        @CsvSource({"0,BE,1", "25,BE,1", "26,AE,2", "50,AE,2", "51,ME,3", "75,ME,3", "76,EE,4", "100,EE,4"})
        void gradesAndPointsAtBoundaries(int score, String expectedGrade, double expectedPoints) {
            assertThat(service.gradeFor(score, "CBC")).isEqualTo(expectedGrade);
            assertThat(service.pointsFor(score, "CBC")).isEqualTo(expectedPoints);
        }
    }

    @Nested
    class GradeForIgcse {

        @ParameterizedTest
        @CsvSource({
            "0,U,1", "19,U,1", "20,G,2", "29,G,2", "30,F,3", "39,F,3",
            "40,E,4", "49,E,4", "50,D,5", "59,D,5", "60,C,6", "69,C,6",
            "70,B,7", "79,B,7", "80,A,8", "89,A,8", "90,A*,9", "100,A*,9"
        })
        void gradesAndPointsAtBoundaries(int score, String expectedGrade, double expectedPoints) {
            assertThat(service.gradeFor(score, "IGCSE")).isEqualTo(expectedGrade);
            assertThat(service.pointsFor(score, "IGCSE")).isEqualTo(expectedPoints);
        }
    }

    @Nested
    class GradingErrorHandling {

        @Test
        void gradeForRejectsNullGradingSystemWithIllegalArgumentException() {
            assertThatThrownBy(() -> service.gradeFor(50, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Grading system is required");
        }

        @Test
        void gradeForRejectsUnknownGradingSystem() {
            assertThatThrownBy(() -> service.gradeFor(50, "WAEC"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown grading system WAEC");
        }

        @Test
        void pointsForRejectsUnknownGradingSystem() {
            assertThatThrownBy(() -> service.pointsFor(50, "WAEC"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown grading system WAEC");
        }

        @Test
        void pointsForThrowsNullPointerExceptionOnNullGradingSystem() {
            // Unlike gradeFor, pointsFor does not guard against null: it dereferences
            // gradingSystem directly. Pinned as-is; see NOTES.md.
            assertThatNullPointerException().isThrownBy(() -> service.pointsFor(50, null));
        }
    }

    @Nested
    class Analyse {

        @Test
        void rejectsUnknownExamId() {
            assertThatThrownBy(() -> service.analyse(12345L, "KCSE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No exam with id 12345");
        }

        @Test
        void reportShapeForOpenerUnderKcse() {
            Model.ExamReport report = service.analyse(9001L, "KCSE");

            assertThat(report.examId()).isEqualTo(9001L);
            assertThat(report.examName()).isEqualTo("Term 2 Opener");
            assertThat(report.gradingSystem()).isEqualTo("KCSE");
            assertThat(report.students()).hasSize(160);
            assertThat(report.streams()).hasSize(4);
            assertThat(report.schoolMeanScore()).isEqualTo(59.59);
            assertThat(report.schoolMeanGrade()).isEqualTo("B-");
        }

        @Test
        void streamSummariesAreRankedByMeanScoreDescending() {
            Model.ExamReport report = service.analyse(9001L, "KCSE");

            assertThat(report.streams()).extracting(Model.StreamSummary::stream)
                    .containsExactly("North", "South", "West", "East");
            assertThat(report.streams()).extracting(Model.StreamSummary::rank)
                    .containsExactly(1, 2, 3, 4);
            assertThat(report.streams()).extracting(Model.StreamSummary::entries)
                    .containsExactly(40, 41, 39, 40);
            assertThat(report.streams()).extracting(Model.StreamSummary::meanScore)
                    .containsExactly(60.91, 60.39, 58.86, 58.16);
            assertThat(report.streams()).extracting(Model.StreamSummary::meanGrade)
                    .containsExactly("B-", "B-", "C+", "C+");
        }

        @Test
        void studentsAreRankedByMeanScoreDescendingWithCompetitionTiesOverall() {
            Model.ExamReport report = service.analyse(9001L, "KCSE");
            var students = report.students();

            assertThat(students.get(0).name()).isEqualTo("Vincent Kiprop");
            assertThat(students.get(0).meanScore()).isEqualTo(73.88);
            assertThat(students.get(0).positionOverall()).isEqualTo(1);

            // Two students tie at 73.0: both get overall rank 2, next student gets rank 4 (gap kept).
            assertThat(students.get(1).name()).isEqualTo("Kamau Wekesa");
            assertThat(students.get(1).meanScore()).isEqualTo(73.0);
            assertThat(students.get(1).positionOverall()).isEqualTo(2);
            assertThat(students.get(2).name()).isEqualTo("Rotich Mutua");
            assertThat(students.get(2).meanScore()).isEqualTo(73.0);
            assertThat(students.get(2).positionOverall()).isEqualTo(2);
            assertThat(students.get(3).name()).isEqualTo("Njeri Otieno");
            assertThat(students.get(3).positionOverall()).isEqualTo(4);

            var last = students.get(students.size() - 1);
            assertThat(last.name()).isEqualTo("Juma Kiprop");
            assertThat(last.meanScore()).isEqualTo(43.5);
            assertThat(last.positionOverall()).isEqualTo(160);
        }

        @Test
        void everyTieInMeanScoreSharesTheSameOverallRank() {
            var students = service.analyse(9001L, "KCSE").students();
            for (int i = 1; i < students.size(); i++) {
                if (students.get(i).meanScore() == students.get(i - 1).meanScore()) {
                    assertThat(students.get(i).positionOverall())
                            .isEqualTo(students.get(i - 1).positionOverall());
                }
            }
        }

        @Test
        void queryCountForAFullAnalysisIsOneFindExamPlusOneFindAllStudentsPlusOnePerStudent() {
            InMemoryRepository repository = Fixtures.school();
            ExamAnalysisService freshService = new ExamAnalysisService(repository);

            freshService.analyse(9001L, "KCSE");

            // 1 (findExam) + 1 (findAllStudents) + 160 (findEntriesForStudent, once per student).
            // Pinned as documentation of the current N+1 access pattern, not asserting it is desirable.
            assertThat(repository.queryCount()).isEqualTo(162);
        }
    }

    @Nested
    class CacheBehaviour {

        @Test
        void secondAnalyseCallForTheSameExamIdReturnsTheCachedReportRegardlessOfGradingSystem() {
            Model.ExamReport first = service.analyse(9001L, "KCSE");
            Model.ExamReport second = service.analyse(9001L, "CBC");

            // Known quirk: the cache key is examId only, so the gradingSystem argument
            // on the second call is silently ignored once an entry exists. Tested as-is
            // flagged as a likely defect in NOTES.md, not fixed here.
            assertThat(second).isSameAs(first);
            assertThat(second.gradingSystem()).isEqualTo("KCSE");
        }

        @Test
        void clearCacheForcesRecomputationWithTheNewGradingSystem() {
            service.analyse(9001L, "KCSE");

            ExamAnalysisService.clearCache();
            Model.ExamReport recomputed = service.analyse(9001L, "CBC");

            assertThat(recomputed.gradingSystem()).isEqualTo("CBC");
        }

        @Test
        void cacheIsSharedAcrossDifferentServiceInstancesAndRepositories() {
            service.analyse(9001L, "KCSE");

            ExamAnalysisService otherServiceOverDifferentRepository =
                    new ExamAnalysisService(Fixtures.school());
            Model.ExamReport fromOtherInstance = otherServiceOverDifferentRepository.analyse(9001L, "CBC");

            // The cache is static, so a second instance backed by a brand-new repository
            // still gets the first instance's cached report. Test as-is, see NOTES.md.
            assertThat(fromOtherInstance.gradingSystem()).isEqualTo("KCSE");
        }
    }

    @Nested
    class ReportText {

        @Test
        void exactTextForOpenerUnderKcse() {
            String expected = """
                    EXAM ANALYSIS
                    =============
                    Term 2 Opener
                    Grading: KCSE
                    Candidates: 160
                    School mean: 59.59 (B-)

                    STREAMS
                    -------
                    1. North        40 candidates     mean 60.91 (B-)
                    2. South        41 candidates     mean 60.39 (B-)
                    3. West         39 candidates     mean 58.86 (C+)
                    4. East         40 candidates     mean 58.16 (C+)

                    TOP 10
                    ------
                    1. Vincent Kiprop             South     73.88 (B+)
                    2. Kamau Wekesa               East      73.0 (B+)
                    2. Rotich Mutua               South     73.0 (B+)
                    4. Njeri Otieno               South     71.75 (B+)
                    5. Njeri Otieno               South     71.63 (B+)
                    6. Imani Wafula               North     70.43 (B+)
                    7. Mwangi Njoroge             North     69.63 (B+)
                    7. Chebet Njoroge             East      69.63 (B+)
                    9. Pauline Mutua              West      69.5 (B+)
                    10. Upendo Wafula             North     69.0 (B)

                    BOTTOM 5
                    --------
                    156. Dalmas Adhiambo          West      45.5 (C-)
                    157. Gideon Odhiambo          East      45.0 (C-)
                    158. Gideon Wafula            East      44.5 (C-)
                    159. Njeri Njoroge            South     44.25 (D+)
                    160. Juma Kiprop              South     43.5 (D+)
                    """;

            assertThat(service.reportText(9001L, "KCSE")).isEqualTo(expected);
        }
    }

    @Nested
    class ExportCsv {

        @Test
        void headerAndTopRowsForOpenerUnderKcse() {
            String csv = service.exportCsv(9001L, "KCSE");
            String[] lines = csv.split("\n");

            assertThat(lines[0]).isEqualTo(
                    "Position,Admission,Name,Stream,Mathematics,English,Kiswahili,Biology,Chemistry,"
                            + "Physics,History,Geography,Mean Score,Mean Grade,Stream Position");
            assertThat(lines[1]).isEqualTo("1,ADM2445,Vincent Kiprop,South,63,68,69,87,72,75,71,86,73.88,B+,1");
            // Kamau Wekesa is missing a History score: the cell is left blank, not 0 or -.
            assertThat(lines[2]).isEqualTo("2,ADM2434,Kamau Wekesa,East,35,70,82,87,87,65,,85,73.0,B+,1");
        }

        @Test
        void hasOneHeaderRowPlusOneRowPerStudentWithResults() {
            String[] lines = service.exportCsv(9001L, "KCSE").split("\n");
            assertThat(lines).hasSize(161);
        }

        @Test
        void lastRowsMatchTheBottomOfTheRanking() {
            String[] lines = service.exportCsv(9001L, "KCSE").split("\n");

            assertThat(lines[lines.length - 1])
                    .isEqualTo("160,ADM2553,Juma Kiprop,South,26,41,35,43,32,74,66,31,43.5,D+,41");
            assertThat(lines[lines.length - 2])
                    .isEqualTo("159,ADM2485,Njeri Njoroge,South,26,49,46,39,33,50,36,75,44.25,D+,40");
        }
    }

    @Nested
    class StudentSlip {

        @Test
        void exactSlipForAStreamTransferredStudent() {
            String expected = """
                    Achieng Otieno (ADM2400)
                    Term 2 Opener - South

                    Mathematics     25    E
                    English         42    D+
                    Kiswahili       43    D+
                    Biology         34    D-
                    Chemistry       71    B+
                    Physics         62    B-
                    History         57    C+
                    Geography       85    A

                    Mean 52.38 (C)
                    Position 136 overall, 35 in South
                    Remark: Average. More consistent revision needed.
                    """;

            assertThat(service.studentSlip(9001L, 1000L, "KCSE")).isEqualTo(expected);
        }

        @Test
        void messageForAStudentWithNoResults() {
            assertThat(service.studentSlip(9001L, 99999L, "KCSE"))
                    .isEqualTo("No results for student 99999");
        }

        @Test
        void excellentRemarkAtOrAboveSeventy() {
            // Vincent Kiprop, mean 73.88.
            assertThat(service.studentSlip(9001L, 1045L, "KCSE"))
                    .endsWith("Remark: Excellent work, keep it up.\n");
        }

        @Test
        void goodEffortRemarkBetweenFiftyFiveAndSeventy() {
            // Mwangi Njoroge, mean 69.63.
            assertThat(service.studentSlip(9001L, 1084L, "KCSE"))
                    .endsWith("Remark: Good effort, aim higher next term.\n");
        }

        @Test
        void averageRemarkBetweenFortyAndFiftyFive() {
            // Gideon Wafula, mean 54.88.
            assertThat(service.studentSlip(9001L, 1150L, "KCSE"))
                    .endsWith("Remark: Average. More consistent revision needed.\n");
        }

        // No student in the opener/KCSE dataset has a mean below 40 (the lowest is 43.5),
        // so the Below expectation remark band has no real record to pin here. The
        // production threshold (< 40) is unchanged and covered by the other three bands
        // bracketing it (40 and 55 boundaries above).
    }

    @Nested
    class StreamAtExam {

        @Test
        void returnsThePreviousStreamWhenTheMoveHappenedAfterTheExam() {
            // Student 1000 moved North to South on 2026-06-10. The opener was sat 2026-05-12,
            // before the move, so the exam-time stream is the old one: North.
            assertThat(service.streamAtExam(1000L, 9001L)).isEqualTo("North");
        }

        @Test
        void returnsTheCurrentStreamWhenTheMoveHappenedBeforeTheExam() {
            // The mid term was sat 2026-06-24, after the 2026-06-10 move, so the student's
            // current stream (South) is also the exam-time stream.
            assertThat(service.streamAtExam(1000L, 9002L)).isEqualTo("South");
        }

        @Test
        void returnsTheCurrentStreamForAStudentWhoNeverMoved() {
            assertThat(service.streamAtExam(1001L, 9001L)).isEqualTo("South");
            assertThat(service.streamAtExam(1001L, 9002L)).isEqualTo("South");
        }

        @Test
        void returnsEmptyStringForAnUnknownStudentOrExam() {
            assertThat(service.streamAtExam(99999L, 9001L)).isEqualTo("");
            assertThat(service.streamAtExam(1000L, 99999L)).isEqualTo("");
        }
    }
}
