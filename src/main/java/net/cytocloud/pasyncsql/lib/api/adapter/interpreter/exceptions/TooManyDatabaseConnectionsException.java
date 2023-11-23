package net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions;

public class TooManyDatabaseConnectionsException extends RuntimeException {

    @Override
    public String getMessage() {
        return "You can only have one connection field in your DatabaseAdapter class";
    }

}
