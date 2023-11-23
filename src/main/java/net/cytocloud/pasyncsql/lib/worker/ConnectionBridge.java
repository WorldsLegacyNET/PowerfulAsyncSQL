package net.cytocloud.pasyncsql.lib.worker;

import lombok.Getter;
import lombok.Setter;
import net.cytocloud.pasyncsql.lib.worker.exceptions.DatabaseNotConnectedException;
import net.cytocloud.pasyncsql.lib.worker.helper.Task;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
public class ConnectionBridge {

    private final @NotNull ConcurrentLinkedQueue<Task> sqlTasks = new ConcurrentLinkedQueue<>();
    private final @NotNull ConnectionInformation information;

    @Setter
    private int attempts = 0;

    @Setter
    private long lastQuery = -1; // When got the last query executed

    @Setter
    private volatile boolean isConnected = false; //Is the sql connected (update automatically when connected)

    @Setter
    private volatile Connection connection;

    public ConnectionBridge(@NotNull ConnectionInformation information) {
        this.information = information;
    }

    /**
     * Perform a task on the sql connection
     * @param task The entered task
     * @return The entered task
     * @throws DatabaseNotConnectedException When the database is not connected
     */
    public @NotNull Task executeConnectionTask(@NotNull Task task) {
        if(!isConnected())
            throw new DatabaseNotConnectedException(this);

        synchronized (sqlTasks) {
            this.sqlTasks.add(task);
        }

        return task;
    }

    /**
     * Execute a update request
     * @param query The sql query
     * @return The created task with your query
     * @throws DatabaseNotConnectedException When the database is not connected
     */
    public @NotNull Task executeSQLUpdate(@NotNull String query) {
        if(!isConnected())
            throw new DatabaseNotConnectedException(this);

        return executeConnectionTask(Task.create(connection -> {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.closeOnCompletion();
        }));
    }

    /**
     * Execute a query
     * @param query The sql query
     * @return The created task with your query
     * @throws DatabaseNotConnectedException When the database is not connected
     */
    public @NotNull Task executeSQLQuery(@NotNull String query) {
        if(!isConnected())
            throw new DatabaseNotConnectedException(this);

        return executeConnectionTask(new Task() {
            @Override
            public void execute(@NotNull Connection connection) throws SQLException {
                Statement statement = connection.createStatement();
                ResultSet set = statement.executeQuery(query);
                statement.closeOnCompletion();

                this.getResponse().accept(set);
            }

        });
    }

    /**
     * Reset the connection (make connected = false, set the attempts to 0, set the last query which got executed to -1)
     */
    public void reset() {
        Bukkit.getLogger().info(String.format("ConnectionBridge >> SQLConnection closed (%s:%s)", getInformation().hostname(), getInformation().port()));

        setConnected(false);
        setAttempts(0);
        setLastQuery(-1);
    }

}
