package net.cytocloud.pasyncsql.lib.api.types.resolved.entry;

import net.cytocloud.pasyncsql.lib.api.types.standard.Property;
import org.jetbrains.annotations.NotNull;

public record TableRowEntry(@NotNull Property property, @NotNull String sqlFormattedValue) {


}
