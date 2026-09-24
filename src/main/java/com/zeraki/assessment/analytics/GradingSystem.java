package com.zeraki.assessment.analytics;

/** One school's grading policy: how a raw score (out of 100) maps to a grade and to points. */
interface GradingSystem {

    String grade(int score);

    double points(int score);
}
