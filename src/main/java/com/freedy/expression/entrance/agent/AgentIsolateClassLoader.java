package com.freedy.expression.entrance.agent;

import com.freedy.expression.utils.ReflectionUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Freedy
 * @date 2023/1/19 0:11
 */
public class AgentIsolateClassLoader extends URLClassLoader {

    private final ClassLoader appClassLoader = AgentIsolateClassLoader.class.getClassLoader();

    public AgentIsolateClassLoader() throws MalformedURLException {
        super(new URL[]{new File(ReflectionUtils.getLocalJarPath()).toURI().toURL()}, null);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            return appClassLoader.loadClass(name);
        }
    }
}
