package net.cytocloud.pasyncsql.lib.api.types.resolved.entry;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.types.Table;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public class TableRow {

    private final @NotNull Table table;
    private final @NotNull Map<Property, String> content;

    public TableRow(@NotNull Table table, @NotNull Map<Property, String> content) {
        this.table = table;
        this.content = content;
    }

    public static @NotNull TableRowBuilder from(@NotNull Table table) {
        return new TableRowBuilder(table);
    }

    /**
     * @return A sql formatted string of the values (value1, value2, value3, ...)
     */
    public @NotNull String getFormattedTableRowValues() {
        StringBuilder bd = new StringBuilder();

        for(Property property : getTable().resolvedTable().asInterpretedTable().getProperties()) {
            if(!this.content.containsKey(property)) {
                bd.append("null").append(", ");
                continue;
            }

            bd.append(this.content.get(property)).append(", ");
        }

        bd.delete(bd.length()-2, bd.length());

        return bd.toString();
    }

}
