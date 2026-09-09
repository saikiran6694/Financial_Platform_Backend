package com.arthium.finance.cron;

/**
 * A per-user scheduled task. Implementations register themselves as beans and
 * are looked up by name, which keeps the services that schedule jobs decoupled
 * from the jobs themselves (they would otherwise form a dependency cycle).
 */
public interface UserJob {

    /** Matches the job names used in the Python version: report, transaction, budget. */
    String name();

    void run(String userId);
}
