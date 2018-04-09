package com.github.ghost_of_mind.pixonic.task.scheduler;

import java.util.concurrent.Executor;


// TODO: use Guava: may be replaced with com.google.common.util.concurrent.MoreExecutors#directExecutor()
public class CurrentThreadExecutor implements Executor {

    private static final CurrentThreadExecutor INSTANCE = new CurrentThreadExecutor();

    public static Executor instance() {
        return INSTANCE;
    }


    private CurrentThreadExecutor() {
    }

    @Override
    public void execute(final Runnable command) {
        command.run();
    }

}
