package net.cytocloud.pasyncsql.lib.worker.helper;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter @Setter
public abstract class Task {

    @Getter(AccessLevel.NONE)
    private final @NotNull List<Consumer<Task>> executorListeners = new ArrayList<>();

    private boolean executed = false;
    private @Nullable SQLException exceptionThrown = null;

    @Setter(AccessLevel.NONE)
    private final @NotNull ResponseFuture response = new ResponseFuture(); // only on tasks which results into responses

    public abstract void execute(@NotNull Connection connection) throws SQLException;

    public void addExecutorListener(@NotNull Consumer<Task> executorListener) {
        if(isExecuted()) {
            executorListener.accept(this);
            return;
        }

        this.executorListeners.add(executorListener);
    }

    @ApiStatus.Internal
    public void recognizeExecution() {
        this.executorListeners.forEach(taskConsumer -> taskConsumer.accept(this));
    }

    public static @NotNull Task create(@NotNull SimplifiedTask task) {
        return new Task() {

            @Override
            public void execute(@NotNull Connection connection) throws SQLException {
                task.execute(connection);
            }

        };
    }

    @FunctionalInterface
    public interface SimplifiedTask {

        void execute(@NotNull Connection connection) throws SQLException;

    }

}
