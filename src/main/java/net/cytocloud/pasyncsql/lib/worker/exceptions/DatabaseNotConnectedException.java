package net.cytocloud.pasyncsql.lib.worker.exceptions;

import lombok.Getter;
import net.cytocloud.pasyncsql.lib.worker.ConnectionBridge;
import org.jetbrains.annotations.NotNull;

@Getter
public class DatabaseNotConnectedException extends RuntimeException {

    private final @NotNull ConnectionBridge bridge;

    public DatabaseNotConnectedException(@NotNull ConnectionBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public String getMessage() {
        return "The connection \"" + bridge.getInformation() + "\" is not connected";
    }

}
