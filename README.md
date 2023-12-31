# PowerfulAsyncSQL
An simple api to maintain asynchronously sql connections to a server with the support of automatisated caches


# A simple example on how to use the api
```java
package net.cytocloud.pasyncsql.example;

import net.cytocloud.pasyncsql.lib.api.adapter.Database;
import net.cytocloud.pasyncsql.lib.api.adapter.DatabaseAdapter;
import net.cytocloud.pasyncsql.lib.api.init.PowerfulAsyncSQLAPI;
import net.cytocloud.pasyncsql.lib.api.types.Cache;
import net.cytocloud.pasyncsql.lib.api.types.Connection;
import net.cytocloud.pasyncsql.lib.api.types.Table;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import net.cytocloud.pasyncsql.lib.api.types.standard.TableRowResolver;
import net.cytocloud.pasyncsql.lib.api.types.standard.TableSchema;
import net.cytocloud.pasyncsql.lib.api.types.standard.cache.CachedDataSet;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

// Implement DatabaseAdapter to show this class will work with the API
public class PlayerBankDatabaseAdapter implements DatabaseAdapter {

    @Database.Connection(
        hostname = "127.0.0.1", // The hostname / address of the sql server
        port = 3306, // The port
        database = "player_bank", // Which database will be used
        username = "root", // What are the credentials (username)
        password = "root", // The password
        tries = 3, // How many times should the api try to connect to the sql
        timeout = 300 // After how many seconds the api will disconnect the sql (will automatically create a new connection on interaction with the connection object)
    )
    private Connection connection; // This object can be accessed over #getConnection
    
    @Database.Table(id = "player_balance_table") // the id is the name of the table in the database
    private Table player_balance_table = new TableSchema(
        new Property("UUID", Property.Type.VARCHAR, 36).primary(), // the uuid is the primary key of the table to access the data
        new Property("Amount", Property.Type.BIGINT) // the amount of money the player has (long)
    );
    
    @Database.Cache(
        tableID = "player_balance_table", // the name of the table in the database
        maxEntries = 1000, // how many entries does this cache save
        autoUpload = true // on overflow should the cache automatically upload the removed cache data?
    )
    private Cache player_balance_cache = new Cache(new TableRowResolver() {

        /**
         * Decode/Resolve the value of a table property
         * @param s The property name
         * @param resultSet The result set from the sql
         * @return The decoded object
         * @throws SQLException When something occurs while getting the result set information
         */
        @Override
        public @NotNull Object resolve(@NotNull String s, @NotNull ResultSet resultSet) throws SQLException {
            if(s.equals("UUID"))
                return resultSet.getString(s); // <--- Convert the UUID to a String
            
            return resultSet.getLong(s); // <--- Convert the amount into a Long
        }

        /**
         * Encode the value of a table property
         * @param s The property name
         * @param o The resolved object from {@link #resolve(String, ResultSet)}
         * @return The sql formatted value
         */
        @Override
        public @NotNull String encode(@NotNull String s, @NotNull Object o) {
            if(s.equals("UUID"))
                return "'" + o + "'"; // <--- Add ' ... ' to format the value as a string
            
            return String.valueOf(o); // <--- Convert to a normal string
        }
        
    });
    
    public long getAmount(@NotNull Player player) {
        final CachedDataSet cachedDataSet = player_balance_cache.get(player.getUniqueId().toString());

        if(cachedDataSet == null)
            throw new NullPointerException("The player \"" + player.getName() + "\" has no bank account");
        
        return (long) Objects.requireNonNull(cachedDataSet.get("Amount"));
    }
    
    public void setAmount(@NotNull Player player, long amount) {
        final CachedDataSet cachedDataSet = player_balance_cache.get(player.getUniqueId().toString());

        if(cachedDataSet == null)
            throw new NullPointerException("The player \"" + player.getName() + "\" has no bank account");
        
        cachedDataSet.set("Amount", amount);
        
        player_balance_cache.update(cachedDataSet); // upload the new data directly to the sql (not necessary)
    }
    
    /**
     * Get the current sql connection instance. This will only create one instance on first call.
     * @see PowerfulAsyncSQLAPI#getConnectionFromDatabaseAdapterClass(Class) 
     * @return The instance of the current connection
     */
    public static @NotNull Connection getConnection() {
        return PowerfulAsyncSQLAPI.initConnection(new PlayerBankDatabaseAdapter());
    }

    /**
     * Get the created adapter in {@link #getConnection()} to access a singleton instance
     * @return The created adapter instance 
     */
    public static @NotNull PlayerBankDatabaseAdapter getInstance() {
        return (PlayerBankDatabaseAdapter) getConnection().getAdapter();
    }


}
```

# How to access the table

```java
/*
    The resolved table is the table which was created from your table schema.
    It has many useful methods to interact with it
 */
final ResolvedTable resolvedTable = player_balance_table.resolvedTable();

resolvedTable.getConnection(); // <--- Stores the current connection

// An example on how to insert data into the table
resolvedTable.insert(
    TableRow.from(resolvedTable)
        .add(Objects.requireNonNull(resolvedTable.getPropertyByName("UUID")), "'" + "the player uuid" + "'")
        .add(Objects.requireNonNull(resolvedTable.getPropertyByName("Amount")), "187")
    .build()
);


/*
    The interpreted table only stores information about your table (the properties and the name and where the table schema was from)
 */
final DatabaseAdapterInterpreter.InterpretedTable interpretedTable = resolvedTable.asInterpretedTable();

interpretedTable.getProperties(); // <--- Get all properties
```

# ConnectionBridge
```java
/*
    The connection bridge is used to transfer data between the connection object and the
    sql connection which is saved in the asynchronous connection handler
 */
final ConnectionBridge bridge = connection.connectionBridge();

// Execute a sql query operation on the connection (query will return a result set)
final Task queryTask = bridge.executeSQLQuery("");

// Execute a sql task operation on the connection (update will not return a result set)
final Task updateTask = bridge.executeSQLUpdate("");

// The task will be executed asynchronously. The entered parameter will be returned.

final Task task = bridge.executeConnectionTask(Task.create(sqlConnection -> {
    // use of the default sqlConnection
}));

// Add a listener on when the task got executed
task.addExecutorListener(task1 -> {
    // ... the task got executed
});

// Access the response of the task

final ResponseFuture future = task.getResponse();

future.syncUntil(1, TimeUnit.SECONDS); // <-- this will block the current thread until 1 second (returns the result when received)
future.sync(); // <-- this will block the current thread until the response was received

// Create an asynchronous consumer for the result set.
future.async(resultSet -> {

});
```
