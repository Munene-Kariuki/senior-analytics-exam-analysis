# Exam Analysis (take-home)

```
./mvnw test
```

The following are not needed for this project; database, Docker, Testcontainers, VPN, credentials, or environment variables. Data is
in memory, seeded by `Fixtures` with a fixed random seed, so runs are deterministic.

JDK 17 or newer.

## What this is

The `ExamAnalysisService` produces exam analysis for a school(per student, per stream, and
overall) in one of the four grading systems. As is, it works and the three tests assert that it runs.

## Your task

1. **Pin the current behaviour.** Create tests over the public methods, written **before**
   you move any code. Do this first even if you run out of time later.
2. **Refactor** into units with single responsibilities.
3. **Keep behaviour identical**, including the behaviour you dislike/disagree with.
4. `NOTES.md`: what you extracted, what you left alone, and what you found but deliberately
   did **not** fix.

The `InMemoryRepository.queryCount()` counts repository calls, if a number is useful to you.

What's not asked for: new features, public API changes, performance work for its own sake.

## Rules

- Commit in small steps: The commit sequence is part of what we read.
- Current behaviour is the project specification: If you believe something is a defec raise it in `NOTES.md` as a separate decision.
