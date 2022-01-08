package com.tool.communication_model;

import java.util.List;

public class RemoteCall {

       private String service;
       private String method;
       private List<RemoteArgument> arguments;

    public RemoteCall(String service, String method, List<RemoteArgument> arguments) {
        this.service = service;
        this.method = method;
        this.arguments = arguments;
    }

    public String getService() {
        return service;
    }

    public String getMethod() {
        return method;
    }

    public List<RemoteArgument> getArguments() {
        return arguments;
    }
}
