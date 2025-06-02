package com.slg.module.register;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class HandlePbBeanManager {
    private final Map<Integer, Method> parseFromMethodMap;

    //handle处理类
    private final Map<Integer, Class<?>> handleClassMap;

    //handle目标方法
    private final Map<Integer, Method> handleMethodMap;

    private HandlePbBeanManager() {
        parseFromMethodMap = new HashMap<>();
        handleClassMap = new HashMap<>();
        handleMethodMap = new HashMap<>();
    }

    // 静态内部类持有单例
    private static class Holder {
        static final HandlePbBeanManager INSTANCE = new HandlePbBeanManager();
    }

    // 全局访问点
    public static HandlePbBeanManager getInstance() {
        return Holder.INSTANCE;
    }

    public void setParseFromMethodMap(Integer key, Method method) {
        parseFromMethodMap.put(key, method);
    }

    public void setHandleClassMap(Integer key, Class<?> clazz) {
        handleClassMap.putIfAbsent(key, clazz);
    }

    public void setHandleMethodMap(Integer key, Method method) {
        handleMethodMap.put(key, method);
    }

    public Method getParseFromMethod(Integer key) {
        return parseFromMethodMap.getOrDefault(key, null);
    }

    public Class<?> getClassHandle(Integer key) {
        return handleClassMap.getOrDefault(key, null);
    }

    public Method getHandleMethod(Integer key) {
        return handleMethodMap.getOrDefault(key, null);
    }
}