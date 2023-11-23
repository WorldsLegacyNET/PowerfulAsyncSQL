package net.cytocloud.pasyncsql.lib.api.types.standard;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;


@Getter
public class Property {

    private boolean primary = false;
    private final @NotNull String name;
    private final @NotNull Type type;
    private final int maxLength;

    public Property(@NotNull String name, @NotNull Type type) {
        this(name, type, -1);
    }

    public Property(@NotNull String name, @NotNull Type type, int maxLength) {
        this.name = name;
        this.type = type;
        this.maxLength = maxLength;
    }

    @Override
    public String toString() {
        return "{" + name + ", " + type + ", "+ maxLength + "}";
    }

    public @NotNull Property primary() {
        this.primary = true;
        return this;
    }

    public enum Type {
        VARCHAR,
        INT,
        SMALLINT,
        BIGINT,
        TEXT;

        public static Type parseType(@NotNull String type) {
            @NotNull String finalType = type.toUpperCase();

            if(Arrays.stream(Type.values()).anyMatch(t -> t.name().equalsIgnoreCase(finalType)))
                return Type.valueOf(finalType);

            if(finalType.contains("TEXT"))
                return TEXT;

            throw new IllegalArgumentException("No enum constant " + Type.class.getName() + "." + finalType);
        }

    }

}
