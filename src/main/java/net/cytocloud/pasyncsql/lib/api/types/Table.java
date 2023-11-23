package net.cytocloud.pasyncsql.lib.api.types;


import lombok.Getter;
import net.cytocloud.pasyncsql.lib.api.types.resolved.ResolvedTable;
import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Table {

    @Getter
    private final @NotNull String id;

    public Table(@NotNull String id) {
        this.id = id;
    }

    public @NotNull ResolvedTable resolvedTable() {
        return ((ResolvedTable) this);
    }

    public @Nullable Property getPropertyByName(@NotNull String name) {
        return resolvedTable().asInterpretedTable().getPropertyByName(name);
    }

}
