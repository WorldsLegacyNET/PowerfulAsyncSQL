package net.cytocloud.pasyncsql.lib.api.types.standard;

import net.cytocloud.pasyncsql.lib.worker.exceptions.InternalDatabaseError;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class TableRowResolver {

    /**
     * This method is used for getting the right datatype for the entered property. Use the resultSet to gain the object
     * @param property The property which needs to be resolved
     * @param resultSet The result set where the information can be gathered from
     * @return An object which represents the property value
     * @throws SQLException Could occur in the result set
     */
    public abstract @NotNull Object resolve(@NotNull String property, @NotNull ResultSet resultSet) throws SQLException;

    /**
     * This method is used to get the right sql format for the specific property (used for inserting)
     * @param property The property
     * @param object The resolved object {@link #resolve(String, ResultSet)}
     * @return The formatted value
     */
    public abstract @NotNull String encode(@NotNull String property, @NotNull Object object);

    public @NotNull Map<Property, Object> createObjects(@NotNull ResultSet resultSet, @NotNull Property... properties) {
        try {
            resultSet.next();

            Map<Property, Object> map = new HashMap<>();

            for(Property property : properties) {
                map.put(property, resolve(property.getName(), resultSet));
            }

            return map;
        }catch(SQLException e) {
            throw new InternalDatabaseError("Couldn't convert the data row into a solid object state. Check the cache table row resolver.") {
                @Override
                public synchronized Throwable getCause() {
                    return e;
                }
            };
        }
    }

}
