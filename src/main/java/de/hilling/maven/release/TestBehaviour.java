package de.hilling.maven.release;

public enum TestBehaviour {
    /**
     * Run tests only during the test (pre-release) execution.
     */
    testPhaseOnly(true, false),
    /**
     * Run tests in both executions.
     */
    runAlways(true, true),
    /**
     * Don't run tests at all.
     */
    skipTests(false, false);

    private final boolean runInTestPhase;
    private final boolean runInReleasePhase;

    TestBehaviour(boolean runInTestPhase, boolean runInReleasePhase) {

        this.runInTestPhase = runInTestPhase;
        this.runInReleasePhase = runInReleasePhase;
    }

    public boolean isRunInTestPhase() {
        return runInTestPhase;
    }

    public boolean isRunInReleasePhase() {
        return runInReleasePhase;
    }
}
