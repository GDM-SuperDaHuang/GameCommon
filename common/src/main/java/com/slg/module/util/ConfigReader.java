package com.slg.module.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private final Properties properties;

    // 构造函数：通过文件名加载配置文件
    public ConfigReader(String fileName)  {
        properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("无法加载配置文件: " + fileName);
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 根据字段名获取属性值（字符串类型）
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    // 根据字段名获取属性值（整数类型）
    public int getIntProperty(String key) throws NumberFormatException {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new NumberFormatException("属性键 '" + key + "' 不存在");
        }
        return Integer.parseInt(value);
    }

}