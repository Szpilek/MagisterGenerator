package com.tool;

import java.util.List;

public class Request {
    String service;
    String method;
    List<SerializedObject> arguments;
}

class SerializedObject {
    String type;
    String value;
}

class Response{
    SerializedObject returnArgument;
    
}