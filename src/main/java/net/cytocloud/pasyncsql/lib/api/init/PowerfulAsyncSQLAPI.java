package net.cytocloud.pasyncsql.lib.api.init;

import net.cytocloud.pasyncsql.lib.api.adapter.DatabaseAdapter;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.DatabaseAdapterInterpreter;
import net.cytocloud.pasyncsql.lib.api.types.Connection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PowerfulAsyncSQLAPI {

    private static final @NotNull Map<Class<? extends DatabaseAdapter>, Connection> registeredConnections = new HashMap<>();

    /**
     * Initialize connection from an adapter
     * @param adapter A new instance of an adapter object
     * @return The already existing connection of the adapter or a new instance
     */
    public static @NotNull Connection initConnection(@NotNull DatabaseAdapter adapter) {
        if(registeredConnections.containsKey(adapter.getClass()))
            return registeredConnections.get(adapter.getClass());

        final Connection connection = DatabaseAdapterInterpreter.createConnection(adapter);
        registeredConnections.put(adapter.getClass(), connection);

        return connection;
    }

    /**
     * Get a connection from a database adapter class (or null when no exist)
     * @param adapterClass The adapter class
     * @return the existing connection or null
     */
    public static @Nullable Connection getConnectionFromDatabaseAdapterClass(@NotNull Class<? extends DatabaseAdapter> adapterClass) {
        if(!registeredConnections.containsKey(adapterClass))
            return null;

        return registeredConnections.get(adapterClass);
    }


}
