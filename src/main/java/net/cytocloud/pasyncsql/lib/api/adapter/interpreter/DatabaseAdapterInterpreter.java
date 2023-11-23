package net.cytocloud.pasyncsql.lib.api.adapter.interpreter;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.adapter.Database;
import net.cytocloud.pasyncsql.lib.api.adapter.DatabaseAdapter;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions.FieldNotEditableException;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions.InvalidTableException;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions.NoConnectionFieldAvailableException;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions.TooManyDatabaseConnectionsException;
import net.cytocloud.pasyncsql.lib.api.types.Cache;
import net.cytocloud.pasyncsql.lib.api.types.CacheConfig;
import net.cytocloud.pasyncsql.lib.api.types.Connection;
import net.cytocloud.pasyncsql.lib.api.types.resolved.ResolvedTable;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import net.cytocloud.pasyncsql.lib.api.types.standard.TableSchema;
import net.cytocloud.pasyncsql.lib.worker.ConnectionBridge;
import net.cytocloud.pasyncsql.lib.worker.ConnectionInformation;
import net.cytocloud.pasyncsql.lib.worker.SQLWorker;
import net.cytocloud.pasyncsql.lib.worker.exceptions.DatabaseNotConnectedException;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DatabaseAdapterInterpreter {
    
    @ApiStatus.Internal
    public static @NotNull Connection createConnection(@NotNull DatabaseAdapter adapter) {
        ConnectionInformation information = null; // Which connection parameters are used
        Field connectionField = null; // Where the connection is located
        ConnectionBridge bridge = null;

        final List<InterpretedTable> interpretedTables = new ArrayList<>();
        final List<Cache> caches = new ArrayList<>();

        for (Field field : adapter.getClass().getDeclaredFields()) {
            List<Database.Connection> connectionAnnotations = Arrays.asList(field.getAnnotationsByType(Database.Connection.class));
            List<Database.ConfigConnection> configConnectionAnnotations = Arrays.asList(field.getAnnotationsByType(Database.ConfigConnection.class));
            List<Database.Table> tableAnnotations = Arrays.asList(field.getAnnotationsByType(Database.Table.class));
            List<Database.Cache> cacheAnnotations = Arrays.asList(field.getAnnotationsByType(Database.Cache.class));

            if(!connectionAnnotations.isEmpty()) {
                final Database.Connection connection = connectionAnnotations.get(0);

                if(information != null)
                    throw new TooManyDatabaseConnectionsException();

                information = new ConnectionInformation(connection.hostname(), connection.port(), connection.database(), connection.username(), connection.password(), connection.timeout(), connection.tries());
                bridge = new ConnectionBridge(information);

                connectionField = field;
            }

            if(!configConnectionAnnotations.isEmpty()) {
                final Database.ConfigConnection connection = configConnectionAnnotations.get(0);

                if(information != null)
                    throw new TooManyDatabaseConnectionsException();

                information = ConnectionInformation.fromConfig(connection.configFile());
                bridge = new ConnectionBridge(information);

                connectionField = field;
            }

            if(!tableAnnotations.isEmpty()) {
                final Database.Table table = tableAnnotations.get(0);
                final String tableID = table.id();

                try {
                    field.setAccessible(true);
                    interpretedTables.add(new InterpretedTable(tableID, ((TableSchema) field.get(adapter)).getProperties(), field));
                } catch (IllegalAccessException e) {
                    throw new FieldNotEditableException(field, e);
                }
            }

            if(!cacheAnnotations.isEmpty()) {
                if(bridge == null)
                    throw new NoConnectionFieldAvailableException();

                final Database.Cache cacheInformation = cacheAnnotations.get(0);
                final String tableID = cacheInformation.tableID();

                try {
                    field.setAccessible(true);

                    final Cache cache = (Cache) field.get(adapter);

                    cache.setTableID(tableID);
                    cache.setConnectionBridge(bridge);

                    caches.add(cache);

                    final CacheConfig cc = cache.getCacheConfig();

                    cc.set(CacheConfig.CacheProperty.AUTO_UPLOAD, cacheInformation.autoUpload());
                    cc.set(CacheConfig.CacheProperty.AUTO_EMPTY, cacheInformation.autoEmpty());
                    cc.set(CacheConfig.CacheProperty.AUTO_SAVE, cacheInformation.autoSave());
                    cc.set(CacheConfig.CacheProperty.MAX_ENTRIES, cacheInformation.maxEntries());
                    cc.set(CacheConfig.CacheProperty.SAVE_INTERVAL, cacheInformation.saveInterval());
                } catch (IllegalAccessException e) {
                    throw new FieldNotEditableException(field, e);
                }

            }
        }

        if(bridge == null)
            throw new NoConnectionFieldAvailableException();

        final @NotNull InterpretedConnection interpretedConnection = new InterpretedConnection(adapter, bridge, interpretedTables, caches);

        if(connectionField == null)
            throw new NoConnectionFieldAvailableException();

        try {
            connectionField.setAccessible(true);
            connectionField.set(adapter, interpretedConnection);
        }catch(Exception e) {
            throw new FieldNotEditableException(connectionField, e);
        }
        
        return interpretedConnection;
    }

    /**
     * Overrides the field in the adapter class of any entered interpreted tables. Look if all tables are working properly and trying to create them
     * @param interpretedTables The interpreted tables
     * @return All resolved tables
     */
    public static @NotNull List<ResolvedTable> registerAllTables(@NotNull List<InterpretedTable> interpretedTables, @NotNull ConnectionBridge connection, @NotNull DatabaseAdapter adapter) {
        final List<ResolvedTable> resolvedTables = new ArrayList<>();

        int tries = 0;

        while(!connection.isConnected()) {
            try {
                Thread.sleep(1000);
                tries++;

                if(tries == 10) throw new DatabaseNotConnectedException(connection);
            }catch(InterruptedException ignored) {}
        }

        for (InterpretedTable interpretedTable : interpretedTables) {
            final String id = interpretedTable.getId();

            // Get the information schema

            try {
                ResultSet set = connection.executeSQLQuery(String.format("SELECT * From INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '%s'", id)).getResponse().syncUntil(3, TimeUnit.SECONDS);;
                List<Property> properties = new ArrayList<>();

                while(set.next()) {
                    final String name = set.getString("COLUMN_NAME");
                    final String type = set.getString("DATA_TYPE");
                    final int maxLength = set.getInt("CHARACTER_MAXIMUM_LENGTH");

                    properties.add(new Property(name, Property.Type.parseType(type), maxLength));
                }

                // When no properties are found the table don't exist ---> Create
                if(properties.isEmpty()) {
                    Bukkit.getLogger().info("SQLConnection create table \"" + id + "\"");
                    connection.executeSQLUpdate("CREATE TABLE `" + id + "` (" + createCreationQuery(interpretedTable) + ")");
                }else {
                    // Table already exist --> Check if the saved table is the same as the provided one

                    if(properties.size() != interpretedTable.getProperties().length)
                        throw new InvalidTableException(interpretedTable);

                    for (int i = 0; i < properties.size(); i++) {
                        final Property p1 = properties.get(i);
                        final Property p2 = interpretedTable.getProperties()[i];

                        if(!p1.getName().equalsIgnoreCase(p2.getName()) || p1.getType() != p2.getType())
                            throw new InvalidTableException(interpretedTable);
                    }

                    /*
                        --- The table is the same ---
                        Add relation with a ResolvedTable
                     */

                    final ResolvedTable resolvedTable = new ResolvedTable(id, interpretedTable, connection);

                    Field relation = interpretedTable.getRelation();
                    relation.setAccessible(true);
                    relation.set(adapter, resolvedTable);

                    resolvedTables.add(resolvedTable);
                }

            }catch(Exception e) {
                throw new RuntimeException(e);
            }

        }

        return resolvedTables;
    }

    private static @NotNull String createCreationQuery(@NotNull InterpretedTable interpretedTable) {
        StringBuilder creationQuery = new StringBuilder();

        for(Property p : interpretedTable.getProperties()) {
            creationQuery.append(
                p.getMaxLength() == -1 ?
                    (p.getName() + " " + p.getType()) :
                    (p.getName() + " " + p.getType() + "(" + p.getMaxLength() + ")")
            );

            if(p.isPrimary()) {
                creationQuery.append(" NOT NULL PRIMARY KEY");
            }

            creationQuery.append(", ");
        }


        return creationQuery.substring(0, creationQuery.length() - 2);
    }

    @Getter
    public static class InterpretedConnection extends Connection {

        private final @NotNull ConnectionBridge bridge;
        private final @NotNull List<InterpretedTable> interpretedTables;
        private final @NotNull List<Cache> caches;
        
        InterpretedConnection(@NotNull DatabaseAdapter adapter, @NotNull ConnectionBridge bridge, @NotNull List<InterpretedTable> interpretedTables, @NotNull List<Cache> caches) {
            super(adapter);
            this.bridge = bridge;
            this.interpretedTables = interpretedTables;
            this.caches = caches;

            SQLWorker.registerConnectionBridge(bridge);

            List<ResolvedTable> resolvedTables = registerAllTables(getInterpretedTables(),  bridge, adapter);

            for(Cache cache : caches) {
                final String tableID = cache.getTableID();
                final ResolvedTable resolvedTable = resolvedTables.stream().filter(t -> t.getId().equals(tableID)).findFirst().orElse(null);

                if(resolvedTable == null)
                    throw new IllegalStateException("Try to create a cache for \"" + tableID + "\" but the table isn't declared anywhere");

                cache.setTable(resolvedTable);
            }

        }

    }

    @Getter
    public static class InterpretedTable {

        private final @NotNull String id;
        private final @NotNull Property[] properties;
        private final @NotNull Field relation;

        public InterpretedTable(@NotNull String id, @NotNull Property[] properties, @NotNull Field relation) {
            this.id = id;
            this.properties = properties;
            this.relation = relation;
        }

        public @Nullable Property getPropertyByName(@NotNull String propertyName) {
            return Arrays.stream(getProperties()).filter(property -> property.getName().equalsIgnoreCase(propertyName)).findFirst().orElse(null);
        }

        public @Nullable Property getPrimaryKeyProperty() {
            return Arrays.stream(getProperties()).filter(Property::isPrimary).findFirst().orElse(null);
        }

    }
    
}
