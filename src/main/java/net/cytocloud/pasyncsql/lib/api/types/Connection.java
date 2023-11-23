package net.cytocloud.pasyncsql.lib.api.types;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.adapter.DatabaseAdapter;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.DatabaseAdapterInterpreter;
import net.cytocloud.pasyncsql.lib.worker.ConnectionBridge;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class Connection {

    @Getter
    private final DatabaseAdapter adapter;

    public Connection(@NotNull DatabaseAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Tries to get a connection bridge element
     * @return The connection bridge
     */
    public @NotNull ConnectionBridge connectionBridge() {
        return ((DatabaseAdapterInterpreter.InterpretedConnection)this).getBridge();
    }

    /**
     * Tries to get all registered tables
     * @return The connection bridge
     */
    public @NotNull List<DatabaseAdapterInterpreter.InterpretedTable> tableList() {
        return ((DatabaseAdapterInterpreter.InterpretedConnection)this).getInterpretedTables();
    }

}
