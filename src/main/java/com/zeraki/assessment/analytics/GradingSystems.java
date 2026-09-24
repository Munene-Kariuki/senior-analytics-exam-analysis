package com.zeraki.assessment.analytics;

/**
 * The four grading policies the school configures exams with. Grade and points boundaries
 * are copied as-is from the previous if/else chains in ExamAnalysisService only the
 * duplication across gradeFor/pointsFor is removed.
 */
enum GradingSystems implements GradingSystem {

    KCSE {
        @Override
        public String grade(int score) {
            if (score >= 80) {
                return "A";
            } else if (score >= 75) {
                return "A-";
            } else if (score >= 70) {
                return "B+";
            } else if (score >= 65) {
                return "B";
            } else if (score >= 60) {
                return "B-";
            } else if (score >= 55) {
                return "C+";
            } else if (score >= 50) {
                return "C";
            } else if (score >= 45) {
                return "C-";
            } else if (score >= 40) {
                return "D+";
            } else if (score >= 35) {
                return "D";
            } else if (score >= 30) {
                return "D-";
            } else {
                return "E";
            }
        }

        @Override
        public double points(int score) {
            if (score >= 80) {
                return 12;
            } else if (score >= 75) {
                return 11;
            } else if (score >= 70) {
                return 10;
            } else if (score >= 65) {
                return 9;
            } else if (score >= 60) {
                return 8;
            } else if (score >= 55) {
                return 7;
            } else if (score >= 50) {
                return 6;
            } else if (score >= 45) {
                return 5;
            } else if (score >= 40) {
                return 4;
            } else if (score >= 35) {
                return 3;
            } else if (score >= 30) {
                return 2;
            } else {
                return 1;
            }
        }
    },

    KCPE {
        @Override
        public String grade(int score) {
            if (score >= 80) {
                return "Above 80";
            } else if (score >= 70) {
                return "70 - 79";
            } else if (score >= 60) {
                return "60 - 69";
            } else if (score >= 50) {
                return "50 - 59";
            } else if (score >= 40) {
                return "40 - 49";
            } else if (score >= 30) {
                return "30 - 39";
            } else {
                return "Below 30";
            }
        }

        @Override
        public double points(int score) {
            return score;
        }
    },

    CBC {
        @Override
        public String grade(int score) {
            if (score >= 76) {
                return "EE";
            } else if (score >= 51) {
                return "ME";
            } else if (score >= 26) {
                return "AE";
            } else {
                return "BE";
            }
        }

        @Override
        public double points(int score) {
            if (score >= 76) {
                return 4;
            } else if (score >= 51) {
                return 3;
            } else if (score >= 26) {
                return 2;
            } else {
                return 1;
            }
        }
    },

    IGCSE {
        @Override
        public String grade(int score) {
            if (score >= 90) {
                return "A*";
            } else if (score >= 80) {
                return "A";
            } else if (score >= 70) {
                return "B";
            } else if (score >= 60) {
                return "C";
            } else if (score >= 50) {
                return "D";
            } else if (score >= 40) {
                return "E";
            } else if (score >= 30) {
                return "F";
            } else if (score >= 20) {
                return "G";
            } else {
                return "U";
            }
        }

        @Override
        public double points(int score) {
            if (score >= 90) {
                return 9;
            } else if (score >= 80) {
                return 8;
            } else if (score >= 70) {
                return 7;
            } else if (score >= 60) {
                return 6;
            } else if (score >= 50) {
                return 5;
            } else if (score >= 40) {
                return 4;
            } else if (score >= 30) {
                return 3;
            } else if (score >= 20) {
                return 2;
            } else {
                return 1;
            }
        }
    };

    /**
     * No null guard here on purpose: a null name reaches the first equals() call and throws
     * NullPointerException, matching pointsFor's previous behaviour. gradeFor keeps its own
     * null check before ever calling this. See NOTES.md.
     */
    static GradingSystem forName(String name) {
        if (name.equals("KCSE")) {
            return KCSE;
        } else if (name.equals("KCPE")) {
            return KCPE;
        } else if (name.equals("CBC")) {
            return CBC;
        } else if (name.equals("IGCSE")) {
            return IGCSE;
        } else {
            throw new IllegalArgumentException("Unknown grading system " + name);
        }
    }
}
