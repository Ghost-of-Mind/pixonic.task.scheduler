package com.pixonic.employment.task.scheduler;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;


public interface SchedulerTaskSolution {

    void schedule(Callable<?> task, LocalDateTime at);

}
