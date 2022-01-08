package com.tool.communication_model;

import java.util.List;

public class RemoteType {
 // te typy muszą być dostępne w kliencie i wrapperze więc muszą być wygenerowane w znanym miejscu źródeł starego projektu
    private String type;
    private List<RemoteType> parameters;
    public RemoteType(
            String type,
            List<RemoteType> parameters
    ){
        this.type = type;
        this.parameters = parameters;
    }

    public String getType() {
        return type;
    }

    public List<RemoteType> getParameters() {
        return parameters;
    }
}
