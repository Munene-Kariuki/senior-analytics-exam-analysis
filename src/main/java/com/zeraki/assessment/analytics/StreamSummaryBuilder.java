package com.zeraki.assessment.analytics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** Groups already-ranked students by stream into a mean-score-ranked list of StreamSummary. */
class StreamSummaryBuilder {

    private final BiFunction<Integer, String, String> gradeFor;

    StreamSummaryBuilder(BiFunction<Integer, String, String> gradeFor) {
        this.gradeFor = gradeFor;
    }

    List<Model.StreamSummary> build(List<Model.StudentResult> ranked, String gradingSystem) {
        Map<String, List<Model.StudentResult>> byStream = new HashMap<>();
        for (Model.StudentResult r : ranked) {
            byStream.computeIfAbsent(r.stream(), k -> new ArrayList<>()).add(r);
        }

        List<Model.StreamSummary> streams = new ArrayList<>();
        for (Map.Entry<String, List<Model.StudentResult>> e : byStream.entrySet()) {
            double sumScore = 0;
            double sumPoints = 0;
            for (Model.StudentResult r : e.getValue()) {
                sumScore = sumScore + r.meanScore();
                sumPoints = sumPoints + r.meanPoints();
            }
            double mean = e.getValue().isEmpty() ? 0 : sumScore / e.getValue().size();
            double points = e.getValue().isEmpty() ? 0 : sumPoints / e.getValue().size();
            mean = Math.round(mean * 100.0) / 100.0;
            points = Math.round(points * 100.0) / 100.0;
            streams.add(new Model.StreamSummary(
                    e.getKey(),
                    e.getValue().size(),
                    mean,
                    points,
                    gradeFor.apply((int) Math.round(mean), gradingSystem),
                    0));
        }

        streams.sort(Comparator.comparingDouble(Model.StreamSummary::meanScore).reversed());
        List<Model.StreamSummary> rankedStreams = new ArrayList<>();
        for (int i = 0; i < streams.size(); i++) {
            Model.StreamSummary s = streams.get(i);
            rankedStreams.add(new Model.StreamSummary(
                    s.stream(), s.entries(), s.meanScore(), s.meanPoints(), s.meanGrade(), i + 1));
        }
        return rankedStreams;
    }
}
