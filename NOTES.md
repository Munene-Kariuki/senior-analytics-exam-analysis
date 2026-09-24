# Notes

## What I did

1. Wrote characterization tests over every public method of `ExamAnalysisService`
   (`analyse`, `reportText`, `exportCsv`, `studentSlip`, `gradeFor`, `pointsFor`,
   `streamAtExam`) against the original, unmodified code, before moving anything.
   These include exact golden-string assertions for `reportText`/`exportCsv`/
   `studentSlip`, boundary tables for all four grading systems, tie-ranking
   invariants, and a `queryCount()` baseline — so any behaviour change during the
   refactor would fail very loudly(Lol!).
2. Refactored `ExamAnalysisService` into single-responsibility units, in three
   steps, running the full suite after each:
   - `GradingSystem` / `GradingSystems` — the four grading policies (KCSE, KCPE,
     CBC, IGCSE), replacing the duplicated if/else ladders in `gradeFor`/`pointsFor`.
   - `StudentResultAggregator`, `RankAssigner`, `StreamSummaryBuilder` — the three
     stages `analyse()` used to do inline: per-student aggregation, overall/in-stream
     ranking, and per-stream summarization.
   - `TextReportRenderer`, `CsvExporter`, `StudentSlipRenderer` — the three
     presentation methods, moved out verbatim.
3. `ExamAnalysisService` is now an orchestrator: it owns the cache, wires the
   above units together in `analyse()`, and holds thin delegating methods for
   the three text outputs, plus `gradeFor`/`pointsFor`/`streamAtExam`.

No public method signature changed. No output changed.

## What I left alone

- **`analyse()`'s N+1 repository access.** It calls `findEntriesForStudent` once
  per student (160 calls) instead of one `findEntriesForExam` plus in-memory
  grouping. `InMemoryRepository.queryCount()` exists to make this visible. I
  pinned the current count (162) in a test rather than fixing it.
- **The `HashMap`-based stream grouping** in `StreamSummaryBuilder`. Iteration
  order is nondeterministic before the final mean-score sort, which happens to
  make no observable difference today (the sort fixes final order), but it's a
  trap for anyone who later reads partial state or changes the sort. Left as-is
  since fixing it would be an unrequested behavioural safety net, not a
  responsibility split.
- **The static `CACHE`** in `ExamAnalysisService`. It's shared across every
  instance and repository, never invalidated except by the explicit
  `clearCache()`, and not thread-safe.
- **Two independent tie-breaking implementations** in `RankAssigner`: overall
  rank uses standard competition ranking (copy the previous student's rank on a
  tie), while in-stream rank is computed separately by counting strictly-greater
  scores in the same stream. They produce consistent results today but are
  genuinely two different algorithms for the same idea. 

## What I found but deliberately did not fix

These look like defects to me. Per the brief, current behaviour is the
specification, so I pinned them with tests instead of changing them.

- **The cache key ignores `gradingSystem`.** `CACHE` is keyed only by `examId`.
  Calling `analyse(9001, "KCSE")` and then `analyse(9001, "CBC")` silently
  returns the first (KCSE) report — the second call's grading system is
  ignored. This will surprise any caller who analyses the same exam under two
  grading systems without calling `clearCache()` in between. Pinned in
  `CacheBehaviour`.
- **`streamAtExam` is dead code from the report's point of view.** Its javadoc
  says it's "kept for the printed report", but nothing in `analyse`,
  `reportText`, `exportCsv`, or `studentSlip` calls it. Every report shows a
  student's *current* stream (`student.currentStream()`), not the stream they
  sat the exam in — so the 14 students who transferred streams mid-term have
  their transfer effectively invisible in every output except a method nobody
  calls.
- **`exportCsv`'s subject columns come from `Fixtures.SUBJECTS`**, a test-fixture
  constant, not from `report`/`exam.subjects()`. It happens to work because the
  two seeded exams share the same subject list, but the CSV exporter is coupled
  to test data rather than the domain model it's rendering.
- **`meanGrade` is computed by re-grading the rounded mean score**
  (`gradeFor((int) Math.round(meanScore), gradingSystem)`), not by averaging the
  per-subject grades or points. This is a defensible policy choice, but it means
  a student's "mean grade" can land on a boundary the individual subject grades
  never touched. Worth confirming with whoever owns the grading specifications.
- **`pointsFor(score, null)` throws `NullPointerException`, while
  `gradeFor(score, null)` throws `IllegalArgumentException("Grading system is
  required")`.** Same missing input, two different exception types depending on
  which method you call. Pinned (exception type only, not message) in
  `GradingErrorHandling`.
