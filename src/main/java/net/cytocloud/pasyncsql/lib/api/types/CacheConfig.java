package net.cytocloud.pasyncsql.lib.api.types;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public class CacheConfig {

    private final @NotNull Map<CacheProperty, Object> storage = new HashMap<>();

    public CacheConfig() {
        fillDefaults();
    }

    public void fillDefaults() {
        storage.put(CacheProperty.MAX_ENTRIES, 1000);
        storage.put(CacheProperty.SAVE_INTERVAL, 30);
        storage.put(CacheProperty.AUTO_UPLOAD, true);
        storage.put(CacheProperty.AUTO_SAVE, false);
        storage.put(CacheProperty.AUTO_EMPTY, false);
    }

    public <T> void set(@NotNull CacheProperty property, @NotNull T value) {
        if(value.getClass().equals(property.getType())) throw new IllegalArgumentException("Illegal type for property \"" + property.name() + "\"");

        storage.remove(property);
        storage.put(property, value);
    }

    public <T> @NotNull T get(@NotNull CacheProperty property) {
        return (T) storage.get(property);
    }

    @Getter
    public enum CacheProperty {

        MAX_ENTRIES (int.class), SAVE_INTERVAL (int.class),
        AUTO_UPLOAD (boolean.class), AUTO_SAVE (boolean.class), AUTO_EMPTY (boolean.class);

        private final @NotNull Class<?> type;

        CacheProperty(@NotNull Class<?> type) {
            this.type = type;
        }


    }

}
