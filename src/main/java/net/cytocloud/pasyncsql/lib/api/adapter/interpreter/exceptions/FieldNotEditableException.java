package net.cytocloud.pasyncsql.lib.api.adapter.interpreter.exceptions;

import lombok.Getter;

import java.lang.reflect.Field;

public class FieldNotEditableException extends RuntimeException {

    @Getter
    private final Field field;

    public FieldNotEditableException(Field field, Throwable cause) {
        super(cause);
        this.field = field;
    }

    @Override
    public String getMessage() {
        return "The field \"" + field.getName() + "\" in class \"" + field.getDeclaringClass() + "\" is not editable";
    }

}
