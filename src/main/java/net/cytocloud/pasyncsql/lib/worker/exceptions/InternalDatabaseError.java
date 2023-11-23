package net.cytocloud.pasyncsql.lib.worker.exceptions;

import org.jetbrains.annotations.NotNull;

public class InternalDatabaseError extends RuntimeException {

    public InternalDatabaseError(@NotNull String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return "An internal database error occurred: " + super.getMessage();
    }

}
