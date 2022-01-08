package com.tool;

public class ParameterInfo {
    String name;
    String type;

    public String getName() {
        return name;
    }

    public ParameterInfo(String type, String name) {
        this.name = name;
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
