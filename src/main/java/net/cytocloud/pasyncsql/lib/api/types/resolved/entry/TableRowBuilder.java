package net.cytocloud.pasyncsql.lib.api.types.resolved.entry;

import net.cytocloud.pasyncsql.lib.api.types.Table;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableRowBuilder {

    private final @NotNull Table table;
    private final @NotNull Map<Property, String> content = new HashMap<>();

    public TableRowBuilder(@NotNull Table table) {
        this.table = table;
    }

    /**
     * Add a specific value to a property
     * @param property The property which is selected (only the name is required)
     * @param sqlFormattedValue The formatted sql value of the property key. (A String would be 'value' and a number only the number as string)
     * @return An instance of this
     * @throws NullPointerException When the property was not found in the table
     * @throws IllegalStateException When the property is already added
     */
    public @NotNull TableRowBuilder add(@NotNull String property, @NotNull String sqlFormattedValue) {
        final @Nullable Property p = table.resolvedTable().asInterpretedTable().getPropertyByName(property);

        if(p == null) throw new NullPointerException("The entered property \"" + property + "\" was not found in the table");
        if(content.containsKey(p)) throw new IllegalStateException("The entered property \"" + property + "\" has already a value set");

        content.put(p, sqlFormattedValue);

        return this;
    }

    /**
     * Add a specific value to a property
     * @param property The property which is selected
     * @param sqlFormattedValue The formatted sql value of the property key. (A String would be 'value' and a number only the number as string)
     * @return An instance of this
     */
    public @NotNull TableRowBuilder add(@NotNull Property property, @NotNull String sqlFormattedValue) {
        return this.add(property.getName(), sqlFormattedValue);
    }

    public @NotNull TableRowBuilder add(@NotNull TableRowEntry entry) {
        return this.add(entry.property(), entry.sqlFormattedValue());
    }

    public @NotNull TableRowBuilder addAll(@NotNull TableRowEntry... entries) {
        Arrays.asList(entries).forEach(this::add);
        return this;
    }

    public @NotNull TableRowBuilder addAll(List<TableRowEntry> entries) {
        entries.forEach(this::add);
        return this;
    }

    public @NotNull TableRow build() {
        return new TableRow(table, content);
    }



}
