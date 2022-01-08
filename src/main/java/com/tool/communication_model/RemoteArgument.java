package com.tool.communication_model;

public class RemoteArgument {
    private String value;
    private RemoteType type;
    public RemoteArgument(
            String value,
            RemoteType type
    ){
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public RemoteType getType() {
        return type;
    }
}
