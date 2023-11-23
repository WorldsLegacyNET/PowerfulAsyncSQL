package net.cytocloud.pasyncsql.lib.api.types;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.cytocloud.pasyncsql.lib.api.types.resolved.entry.TableRowEntry;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import net.cytocloud.pasyncsql.lib.api.types.standard.cache.AutoSaveCallable;
import net.cytocloud.pasyncsql.lib.api.types.standard.cache.CachedDataSet;
import net.cytocloud.pasyncsql.lib.api.types.standard.TableRowResolver;
import net.cytocloud.pasyncsql.lib.api.types.standard.cache.CachedDataSetNotFoundException;
import net.cytocloud.pasyncsql.lib.worker.ConnectionBridge;
import net.cytocloud.pasyncsql.lib.worker.exceptions.DatabaseNotConnectedException;
import net.cytocloud.pasyncsql.lib.worker.exceptions.InternalDatabaseError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
public class Cache {

    private final @NotNull TableRowResolver resolver;
    private final @NotNull CacheConfig cacheConfig = new CacheConfig();
    private final @NotNull List<AutoSaveCallable> autoSaveListeners = new ArrayList<>();

    @Setter
    private @NotNull Table table;

    @Setter
    private @NotNull String tableID;

    @Setter
    private @NotNull ConnectionBridge connectionBridge;

    @Getter(AccessLevel.NONE)
    private final @NotNull List<CachedDataSet> cachedDataSets = new ArrayList<>();

    public Cache(@NotNull TableRowResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Download the data set from the sql and create a cached data set for it. It won't add it into the cache directly
     * @param property The property which is used for selecting the data set
     * @param value The filter for getting the value (= needs to be unique)
     * @return The cached data set or null when no data set is available in the sql
     * @throws DatabaseNotConnectedException When the database is not connected
     */
    public @Nullable CachedDataSet download(@NotNull Property property, @NotNull String value) {
         if(!this.getConnectionBridge().isConnected())
             throw new DatabaseNotConnectedException(getConnectionBridge());

        final @Nullable ResultSet resultSet = getTable().resolvedTable().selectAll(String.format("`%s` = '%s'", property.getName(), value)).getResponse().syncUntil(2, TimeUnit.SECONDS);

        if(resultSet == null)
            throw new InternalDatabaseError("The result set from the download request is null. Is the database still connected?");

        //Convert the gathered information into objects
        final Map<Property, Object> objects = getResolver().createObjects(resultSet, getTable().resolvedTable().asInterpretedTable().getProperties());

        return new CachedDataSet(property, value, this, objects, System.currentTimeMillis());
    }

    /**
     * Search for the cached data set (If it is already saved)
     * @param property The property which was used
     * @param value The value which was used
     * @return The cached data set if found or null
     */
    public @Nullable CachedDataSet search(@NotNull Property property, @NotNull String value) {
        return this.cachedDataSets.stream().filter(d -> d.getProperty() == property && d.getValue().equals(value)).findFirst().orElse(null);
    }

    /**
     * Search for the cached data set (If it is already saved)
     * @param property The property which was used
     * @param value The value which was used
     * @return The cached data set if found or null
     * @throws NullPointerException When the table doesn't have the entered property
     */
    public @Nullable CachedDataSet search(@NotNull String property, @NotNull String value) {
        return this.search(Objects.requireNonNull(getTable().getPropertyByName(property)), value);
    }

    /**
     * Search for the cached data set (If it is already saved)
     * @param value The value which was used (Using the primary key property)
     * @return The cached data set if found or null
     * @throws NullPointerException When the table doesn't have a primary key property
     */
    public @Nullable CachedDataSet search(@NotNull String value) {
        return this.search(Objects.requireNonNull(getTable().resolvedTable().asInterpretedTable().getPrimaryKeyProperty()), value);
    }

    /**
     * Check if a data set is already added in a cache
     * @param cachedDataSet The data set to check
     * @return true when the data set is already registered
     */
    public boolean isDataSetAlreadyRegistered(@NotNull CachedDataSet cachedDataSet) {
        return this.cachedDataSets.stream().anyMatch(d -> d.getValue().equals(cachedDataSet.getValue()) && d.getProperty() == cachedDataSet.getProperty());
    }

    /**
     * Check if a data set is existent in the database
     * @return true when exist
     * @throws CachedDataSetNotFoundException When the data set was not found in the sql or in the cache
     * @throws DatabaseNotConnectedException When the database is not connected
     * @throws InternalDatabaseError When an internal database error occurs
     */
    public boolean isDataSetOnSQL(@NotNull CachedDataSet cachedDataSet) {
        if(!this.getConnectionBridge().isConnected())
            throw new DatabaseNotConnectedException(getConnectionBridge());

        final @Nullable ResultSet resultSet = getTable().resolvedTable().selectAll(String.format("`%s` = '%s'", cachedDataSet.getProperty().getName(), cachedDataSet.getValue())).getResponse().syncUntil(2, TimeUnit.SECONDS);

        if(resultSet == null)
            throw new InternalDatabaseError("The result set is null. Database probably not connected?");

        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new InternalDatabaseError("Unknown error") {
                @Override
                public synchronized Throwable getCause() {
                    return e;
                }
            };
        }
    }

    /**
     * Update (e.g. Upload) a cached data set into the sql (Overwrites an existing data set)
     * @param cachedDataSet The cached data set
     */
    public void update(@NotNull CachedDataSet cachedDataSet) {
        final Table table = cachedDataSet.getCache().getTable();

        // The table already have the table row?
        if(isDataSetOnSQL(cachedDataSet)) {
            // Update the existing table row
            table.resolvedTable().update(cachedDataSet.toTableRow(), "`" + cachedDataSet.getProperty().getName() + "` = '" + cachedDataSet.getValue() + "'");
            return;
        }

        // Insert a new table row
        table.resolvedTable().insert(cachedDataSet.toTableRow());
    }

    /**
     * Stores a new cached data set
     * @param cachedDataSet The new cached data set
     * @throws IllegalStateException When the cached data set is already existing
     */
    public void save(@NotNull CachedDataSet cachedDataSet) {
        if(isDataSetAlreadyRegistered(cachedDataSet))
            throw new IllegalStateException("The data set is already existing in the cache");

        final int maxEntries = this.getCacheConfig().get(CacheConfig.CacheProperty.MAX_ENTRIES);

        // When the cache is already full
        if(maxEntries != -1 && maxEntries == this.cachedDataSets.size()) {
            // Get the data set that would be removed due to overloading the cache
            final @NotNull CachedDataSet dataSetToRemove = this.cachedDataSets.get(0);

            // When auto upload is enabled the data set get updated
            if(this.getCacheConfig().get(CacheConfig.CacheProperty.AUTO_UPLOAD)) {
                update(dataSetToRemove);
            }

            // The data set get removed
            remove(dataSetToRemove);
        }

        this.cachedDataSets.add(cachedDataSet);
    }

    /**
     * Remove the cached data set
     * @param cachedDataSet An existing cached data set in the cache
     */
    public void remove(@NotNull CachedDataSet cachedDataSet) {
        this.cachedDataSets.remove(cachedDataSet);
    }

    /**
     * Download (when necessary) or get the cached value by using the primary key property and using the entered value as condition
     * @param property The property which is used for selecting the data set
     * @param value The filter for getting the value (= needs to be unique)
     * @return The cached data set or null when no data set was found
     * @throws CachedDataSetNotFoundException When the data set was not found in the sql or in the cache
     * @throws DatabaseNotConnectedException When the database is not connected
     */
    public @Nullable CachedDataSet get(@NotNull Property property, @NotNull String value) {
        final @Nullable CachedDataSet internalSavedDataSet = search(property, value);

        if(internalSavedDataSet != null)
            return internalSavedDataSet;

        final @Nullable CachedDataSet newDownloaded = download(property, value);

        if(newDownloaded == null)
            return null;

        // Save the downloaded data set
        save(newDownloaded);

        return newDownloaded;
    }

    /**
     * Download (when necessary) or get the cached value by using the primary key property and using the entered value as condition
     * @param property The property which is used for selecting the data set
     * @param value The filter for getting the value (= needs to be unique)
     * @return The cached data set or null when no data set was found
     * @throws CachedDataSetNotFoundException When the data set was not found in the sql or in the cache
     * @throws NullPointerException When the table doesn't have the entered property
     * @throws DatabaseNotConnectedException When the database is not connected
     */
    public @Nullable CachedDataSet get(@NotNull String property, @NotNull String value) {
        return this.get(Objects.requireNonNull(table.getPropertyByName(property)), value);
    }

    /**
     * Download (when necessary) or get the cached value by using the primary key property and using the entered value as condition
     * @param value The filter for getting the value (= needs to be unique)
     * @return The cached data set or null when no data set was found
     * @throws CachedDataSetNotFoundException When the data set was not found in the sql or in the cache
     * @throws NullPointerException When the table doesn't have a primary key property
     * @throws DatabaseNotConnectedException When the database is not connected
     */
    public @Nullable CachedDataSet get(@NotNull String value) {
        return this.get(Objects.requireNonNull(getTable().resolvedTable().asInterpretedTable().getPrimaryKeyProperty()), value);
    }

    /**
     * Get a copy of all cached data sets in this cache
     * @return A list of all cached data sets
     */
    public @NotNull List<CachedDataSet> getAllCachedDataSets() {
        return new ArrayList<>(this.cachedDataSets);
    }

}
