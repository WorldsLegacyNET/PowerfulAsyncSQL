package net.cytocloud.pasyncsql.lib.worker.helper;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ResponseFuture {

    private ResultSet result = null;
    private final List<Consumer<ResultSet>> consumers = new ArrayList<>();

    /**
     * @param result The response from the sql query
     */
    @ApiStatus.Internal
    public void accept(@NotNull ResultSet result) {
        this.result = result;
        this.consumers.forEach(c -> c.accept(result));
    }

    public @NotNull ResultSet sync() {
        while(result == null) {
            try {
                Thread.sleep(100);
            }catch(InterruptedException ignored) {}
        }

        return result;
    }

    public @Nullable ResultSet syncUntil(long time, TimeUnit timeUnit) {
        long ms = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(time, timeUnit);

        while(result == null && System.currentTimeMillis() <= ms) {
            try {
                Thread.sleep(100);
            }catch(InterruptedException ignored) {}
        }

        return result;
    }

    public void async(@NotNull Consumer<ResultSet> consumer) {
        if(this.result != null) {
            consumer.accept(this.result);
            return;
        }

        consumers.add(consumer);
    }

}
