package net.cytocloud.pasyncsql.lib.api.types.standard.cache;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.types.Cache;
import net.cytocloud.pasyncsql.lib.api.types.resolved.entry.TableRow;
import net.cytocloud.pasyncsql.lib.api.types.resolved.entry.TableRowEntry;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import net.cytocloud.pasyncsql.lib.api.types.standard.TableRowResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Getter
public class CachedDataSet {

    private final @NotNull Property property;
    private final @NotNull String value;
    private final @NotNull Cache cache;
    private final @NotNull Map<Property, Object> objects;
    private final @NotNull Map<String, Object> internalStorage = new HashMap<>();

    private final long time;

    /**
     * @param cache The cache where the data set is from
     * @param objects The objects which were downloaded
     * @param time The time the download was made
     */
    public CachedDataSet(@NotNull Property property, @NotNull String value, @NotNull Cache cache, @NotNull Map<Property, Object> objects, long time) {
        this.cache = cache;
        this.objects = objects;
        this.time = time;
        this.property = property;
        this.value = value;
    }

    /**
     * Get the object for the entered property
     * @param property The property
     * @throws NullPointerException When the table doesn't have the property
     */
    public @Nullable Object get(@NotNull String property) {
        return this.getObjects().get(this.getCache().getTable().getPropertyByName(property));
    }

    /**
     * Get the object for the entered property
     * @param property The property
     */
    public @Nullable Object get(@NotNull Property property) {
        if(!this.getObjects().containsKey(property))
            return null;

        return this.getObjects().get(property);
    }

    /**
     * Set the value of a property
     * @param property The property
     * @param value The value
     * @throws NullPointerException When the table doesn't have the property
     */
    public void set(@NotNull String property, @NotNull Object value) {
        this.set(Objects.requireNonNull(this.getCache().getTable().getPropertyByName(property)), value);
    }

    /**
     * Set the value of a property
     * @param property The property
     * @param value The value
     */
    public void set(@NotNull Property property, @NotNull Object value) {
        this.getObjects().remove(property);
        this.getObjects().put(property, value);
    }

    /**
     * This is used to fill no existing property values of the table to a null object
     */
    public void fillEmptyPropertyValues() {
        for(Property property : this.getCache().getTable().resolvedTable().asInterpretedTable().getProperties()) {
            if(this.getObjects().containsKey(property)) //skip all existing properties
                continue;

            this.getObjects().put(property, null);
        }
    }

    /**
     * Convert this cached data set into a table row
     * @return A new table row with the content
     */
    public @NotNull TableRow toTableRow() {
        final TableRowResolver resolver = getCache().getResolver();
        final List<TableRowEntry> entryList = new ArrayList<>();

        fillEmptyPropertyValues(); // make sure every property has an object

        getObjects().forEach((property, object) -> {
            if(object == null) {
                entryList.add(new TableRowEntry(property, "null"));
                return;
            }

            entryList.add(new TableRowEntry(property, resolver.encode(property.getName(), object)));
        });

        return TableRow.from(this.getCache().getTable()).addAll(entryList).build();
    }


}
