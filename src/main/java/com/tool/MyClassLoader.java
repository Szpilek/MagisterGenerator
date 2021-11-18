package com.tool;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


public class MyClassLoader extends URLClassLoader {
    static final MyClassLoader classLoader = new MyClassLoader(new URL[0], MyClassLoader.class.getClassLoader());
    public static void addFile(String path) throws MalformedURLException {
        File file = new File(path);
        if(file.exists()) {
            URL url = file.toURI().toURL();
            classLoader.addURL(url);
        }
    }
    public MyClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addURL(URL url) {
        super.addURL(url);
    }
}