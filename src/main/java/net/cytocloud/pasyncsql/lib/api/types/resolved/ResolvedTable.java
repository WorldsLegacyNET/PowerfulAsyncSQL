package net.cytocloud.pasyncsql.lib.api.types.resolved;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.types.Table;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.DatabaseAdapterInterpreter;
import net.cytocloud.pasyncsql.lib.api.types.resolved.entry.TableRow;
import net.cytocloud.pasyncsql.lib.api.types.resolved.entry.TableRowEntry;
import net.cytocloud.pasyncsql.lib.worker.ConnectionBridge;
import net.cytocloud.pasyncsql.lib.worker.helper.Task;
import org.jetbrains.annotations.NotNull;

public class ResolvedTable extends Table {

    private final @NotNull DatabaseAdapterInterpreter.InterpretedTable interpretedTable;

    @Getter
    private final @NotNull ConnectionBridge connection;

    public ResolvedTable(@NotNull String id, @NotNull DatabaseAdapterInterpreter.InterpretedTable interpretedTable, @NotNull ConnectionBridge connection) {
        super(id);

        this.interpretedTable = interpretedTable;
        this.connection = connection;
    }

    public @NotNull Task insert(@NotNull TableRow tableRow) {
        final String sql = String.format("INSERT INTO `%s` (%s) VALUES (%s)", this.getId(), getFormattedProperties(), tableRow.getFormattedTableRowValues());
        return getConnection().executeSQLUpdate(sql);
    }

    public @NotNull Task insert(@NotNull TableRowEntry... entries) {
        return this.insert(TableRow.from(this).addAll(entries).build());
    }

    public @NotNull Task update(@NotNull TableRow tableRow, @NotNull String condition) {
        final String sql = String.format("UPDATE `%s` SET %s WHERE %s", this.getId(), getSetFormattedProperties(tableRow), condition);
        return getConnection().executeSQLUpdate(sql);

    }

    public @NotNull Task update(@NotNull String condition, @NotNull TableRowEntry... entries) {
        return this.update(TableRow.from(this).addAll(entries).build(), condition);
    }

    /**
     * SELECT * FROM `table`
     * @return The created task for the request
     */
    public @NotNull Task selectAll() {
        return this.connection.executeSQLQuery(String.format("SELECT * FROM `%s`", this.getId()));
    }

    /**
     * SELECT * FROM `table` WHERE condition
     * @param condition The condition
     * @return The created task for the request
     */
    public @NotNull Task selectAll(@NotNull String condition) {
        return this.connection.executeSQLQuery(String.format("SELECT * FROM `%s` WHERE " + condition, this.getId()));
    }

    /**
     * SELECT property1, property2, property... FROM `table`
     * @return The created task for the request
     */
    public @NotNull Task selectProperties(@NotNull String... properties) {
        StringBuilder formattedProperties = new StringBuilder();

        for (String p : properties) {
            formattedProperties.append(p).append(", ");
        }

        return this.connection.executeSQLQuery(String.format("SELECT %s FROM `%s`", formattedProperties.substring(0, formattedProperties.length()-2), this.getId()));
    }

    /**
     * SELECT property1, property2, property... FROM `table` WHERE condition
     * @return The created task for the request
     */
    public @NotNull Task selectProperties(@NotNull String condition, @NotNull String... properties) {
        StringBuilder formattedProperties = new StringBuilder();

        for (String p : properties) {
            formattedProperties.append(p).append(", ");
        }

        return this.connection.executeSQLQuery(String.format("SELECT %s FROM `%s` WHERE %s", formattedProperties.substring(0, formattedProperties.length()-2), this.getId(), condition));
    }

    public @NotNull Task truncate() {
        return this.connection.executeSQLUpdate(String.format("TRUNCATE `%s`", getId()));
    }

            /*
            UPDATE `table` SET `column1` = value1, ... WHERE condition;
         */

    public @NotNull String getFormattedProperties() {
        StringBuilder bd = new StringBuilder();

        for(Property property : asInterpretedTable().getProperties()) {
            bd.append("`").append(property.getName()).append("`, ");
        }

        bd.delete(bd.length()-2, bd.length());

        return bd.toString();
    }

    public @NotNull String getSetFormattedProperties(@NotNull TableRow tableRow) {
        StringBuilder bd = new StringBuilder();

        for(Property property : asInterpretedTable().getProperties()) {
            if(!tableRow.getContent().containsKey(property))
                continue; //when the property is not existing

            bd.append("`").append(property.getName()).append("` = ").append(tableRow.getContent().get(property)).append(", ");
        }

        bd.delete(bd.length()-2, bd.length());

        return bd.toString();
    }

    public @NotNull DatabaseAdapterInterpreter.InterpretedTable asInterpretedTable() {
        return this.interpretedTable;
    }



}
