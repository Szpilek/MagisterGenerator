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

    public RemoteCall() {
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setArguments(List<RemoteArgument> arguments) {
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
