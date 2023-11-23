package net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.DatabaseAdapterInterpreter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class InvalidTableException extends RuntimeException {

    @Getter
    private final @NotNull DatabaseAdapterInterpreter.InterpretedTable table;

    public InvalidTableException(@NotNull DatabaseAdapterInterpreter.InterpretedTable table) {
        this.table = table;
    }

    @Override
    public String getMessage() {
        return "The table \"" + table.getId() + "\" with properties \"" + Arrays.toString(table.getProperties()) + "\" is not the same as the sql table";
    }

}
