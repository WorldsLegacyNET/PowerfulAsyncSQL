package net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions;

public class InvalidTableSchemeException extends RuntimeException {

    @Override
    public String getMessage() {
        return "Your table scheme is invalid. You need only one property which is a primary key";
    }

}
