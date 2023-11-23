package net.cytocloud.pasyncsql.lib.api.types.standard;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.types.Table;
import net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions.InvalidTableSchemeException;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
public class TableSchema extends Table {

    private final @NotNull Property[] properties;

    public TableSchema(Property... properties) {
        super("none");
        this.properties = properties.clone();

        // Check if it's only one primary key property available
        if(Arrays.stream(this.properties).filter(Property::isPrimary).count() != 1)
            throw new InvalidTableSchemeException();
    }

    @Override
    public @NotNull String getId() {
        throw new IllegalStateException("A database scheme has no table id");
    }

}
