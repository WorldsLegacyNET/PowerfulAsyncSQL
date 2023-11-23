package net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions;

public class NoConnectionFieldAvailableException extends RuntimeException{

    @Override
    public String getMessage() {
        return "There is no connection field available to gather information from";
    }

}
