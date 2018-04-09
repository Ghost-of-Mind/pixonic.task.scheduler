package com.github.ghost_of_mind.pixonic.task.scheduler;

import com.pixonic.employment.task.scheduler.SchedulerTaskSolution;
import com.pixonic.employment.task.scheduler.SchedulerTaskSolutionTest;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.defaultThreadFactory;


public class SchedulerServiceAsTaskSolutionTest extends SchedulerTaskSolutionTest {

    private SchedulerService scheduleService;

    @Before
    public void beforeTest() {
        scheduleService = new SchedulerService(defaultThreadFactory());
    }

    @Override
    protected SchedulerTaskSolution getSolution() {
        return scheduleService;
    }

    @After
    public void afterTest() throws InterruptedException {
        scheduleService.shutdownNow();
        if (!scheduleService.awaitTermination(1L, TimeUnit.SECONDS)) {
            logger.warn("{} shutdowns too long", scheduleService);
        }
    }

}
