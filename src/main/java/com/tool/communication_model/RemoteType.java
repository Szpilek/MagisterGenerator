package com.tool.communication_model;

import java.util.List;

public class RemoteType {
    private String type;
    private List<RemoteType> parameters;

    public RemoteType(
            String type,
            List<RemoteType> parameters
    ){
        this.type = type;
        this.parameters = parameters;
    }

    public RemoteType() {
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setParameters(List<RemoteType> parameters) {
        this.parameters = parameters;
    }

    public String getType() {
        return type;
    }

    public List<RemoteType> getParameters() {
        return parameters;
    }
}
