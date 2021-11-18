package com.tool;

import java.lang.reflect.Field;
import java.util.List;

public class ClassInfo {
    List<Field> autowiredFields;
    Class<?> classDescription;

    public ClassInfo(List<Field> autowiredFields, Class<?> classDescription){
        this.autowiredFields=autowiredFields;
        this.classDescription=classDescription;
    }
}
