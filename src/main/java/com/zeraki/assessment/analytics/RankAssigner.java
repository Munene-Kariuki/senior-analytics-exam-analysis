package com.zeraki.assessment.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Assigns overall and in-stream positions from mean score. Overall ties share the same rank
 * (competition ranking, with the usual gap after a tie) in-stream position is computed
 * independently, by counting students in the same stream with a strictly higher mean score.
 * These are two different tie-breaking implementations kept exactly as they were found;
 * see NOTES.md.
 */
class RankAssigner {

    List<Model.StudentResult> assignRanks(List<Model.StudentResult> results) {
        List<Model.StudentResult> sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparingDouble(Model.StudentResult::meanScore).reversed());

        List<Model.StudentResult> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Model.StudentResult r = sorted.get(i);

            int overall = i + 1;
            if (i > 0 && sorted.get(i - 1).meanScore() == r.meanScore()) {
                overall = ranked.get(i - 1).positionOverall();
            }

            int inStream = 1;
            for (Model.StudentResult other : sorted) {
                if (other.stream().equals(r.stream()) && other.meanScore() > r.meanScore()) {
                    inStream = inStream + 1;
                }
            }

            ranked.add(new Model.StudentResult(
                    r.studentId(),
                    r.admissionNumber(),
                    r.name(),
                    r.stream(),
                    r.subjects(),
                    r.meanScore(),
                    r.meanPoints(),
                    r.meanGrade(),
                    inStream,
                    overall));
        }
        return ranked;
    }
}
