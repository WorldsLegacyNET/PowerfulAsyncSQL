package net.cytocloud.pasyncsql.lib.worker;

import lombok.Getter;
import lombok.Setter;
import net.cytocloud.pasyncsql.lib.worker.helper.Task;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class SQLWorker extends Thread {

    @Setter @Getter
    private static @NotNull Consumer<SQLException> exceptionLogger = e -> {
        Bukkit.getLogger().severe("A sql exception occurred asynchron. Please override the exception logger to catch errors more effective");
        e.printStackTrace(); //TODO: add better exception logging
    };

    private static final @NotNull SQLWorker worker = new SQLWorker();
    private static final @NotNull ConcurrentLinkedQueue<ConnectionBridge> connectedBridges = new ConcurrentLinkedQueue<>();
    private static boolean isActive = false;

    public SQLWorker() {
        setName("PowerfulSQL-Worker");
    }

    @Override
    public void run() {
        List<ConnectionBridge> readyToRemove = new ArrayList<>();

        while(isActive) {
            synchronized (connectedBridges) {

                connectedBridges.forEach(bridge -> {

                    /*
                        Make sure the sql is connected (When not -> Connect)
                     */

                    if(!bridge.isConnected()) {
                        if(bridge.getAttempts() >= bridge.getInformation().tries()) {
                            readyToRemove.add(bridge);
                            return; // Too many attempts remove the bridge
                        }

                        final @Nullable Connection connection = createConnection(bridge);

                        if(connection == null) // No connection got created (skip)
                            return;

                        bridge.setConnected(true);
                        bridge.setConnection(connection);

                        Bukkit.getLogger().info(String.format("ConnectionBridge >> SQLConnection created (%s:%s)", bridge.getInformation().hostname(), bridge.getInformation().port()));

                        return; // This bridge can execute tasks in the next rotation
                    }

                    /*
                        The bridge is connected:
                        - Close the connection when:
                            - It got closed remotely
                            - The last sent query was x seconds before (Timeout) ---> Used to only connect when tasks are required
                        - Try to execute all tasks
                     */

                    final Connection connection = bridge.getConnection();

                    // The connection is null? ---> Make the connection offline
                    if(connection == null) {
                        bridge.reset();
                        return;
                    }

                    try {
                        if(!connection.isValid(10)) {
                            bridge.reset();
                            return;
                        }
                    }catch(SQLException e) {
                        bridge.reset();
                        getExceptionLogger().accept(e);
                        return;
                    }

                    // Is a timeout occurred?
                    if(bridge.getLastQuery() != -1 && ((System.currentTimeMillis() - bridge.getLastQuery()) / 1000 >= bridge.getInformation().timeout())) {
                        bridge.reset();
                        return;
                    }

                    /*

                        -- Execute tasks on the sql

                     */

                   synchronized (bridge.getSqlTasks()) {

                       while(!bridge.getSqlTasks().isEmpty()) {
                           final @NotNull Task task = bridge.getSqlTasks().poll();

                           if(task.isExecuted()) // the task got already executed?
                               continue;

                           try {
                               task.execute(bridge.getConnection()); // Execute the task
                           } catch (SQLException e) {

                               task.setExceptionThrown(e);
                               getExceptionLogger().accept(e);

                           }finally {
                               task.setExecuted(true);
                               task.recognizeExecution();
                               bridge.setLastQuery(System.currentTimeMillis());
                           }


                       }

                   }

                });

            }

            // Delete all expired connections
            readyToRemove.forEach(connectedBridges::remove);
            readyToRemove.clear();
        }
    }

    private @Nullable Connection createConnection(@NotNull ConnectionBridge bridge) {
        final ConnectionInformation information = bridge.getInformation();

        try {
            //Connect to the sql
            return DriverManager.getConnection("jdbc:mysql://" + information.hostname() +":"+information.port()+"/" + information.database() + "?user=" + information.username() + "&password=" + information.password() + "&autoReconnect=true");
        }catch(SQLException e) {
            bridge.setAttempts(bridge.getAttempts() + 1);
            getExceptionLogger().accept(e);
        }

        return null;
    }

    @ApiStatus.Internal
    public static void registerConnectionBridge(@NotNull ConnectionBridge bridge) {
        synchronized (connectedBridges) {
            connectedBridges.add(bridge);
        }
    }

    public static void startThread() {
        Bukkit.getLogger().info("Start sql worker thread");

        if(Thread.getAllStackTraces().keySet().stream().anyMatch(td -> td.getName().equals("PowerfulSQL-Worker"))) {
            Bukkit.getLogger().warning("A thread named \"SQL Worker\" is already running");
            return;
        }

        isActive = true;
        worker.start();
    }

    public static void stopThread() {
        Bukkit.getLogger().info("Stop sql worker thread");

        isActive = false;
        worker.interrupt();
    }

}
