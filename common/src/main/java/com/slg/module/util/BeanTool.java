package com.slg.module.util;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
/**
 * 放到外层，确保spring启动优先创建BeanTool
 */
@Component
public class BeanTool implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
//    private static final ConcurrentHashMap<Class<?>, Object> beanCache = new ConcurrentHashMap<>();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 防止重复设置（虽然 Spring 一般只调用一次，但多线程环境下仍可能有问题）
        if (BeanTool.applicationContext == null) {
            synchronized (BeanTool.class) {
                if (BeanTool.applicationContext == null) {
                    BeanTool.applicationContext = applicationContext;
                }
            }
        }
    }

    /**
     * 获取 Spring 管理的 Bean（单例或原型）
     */
    public static <T> T getBean(Class<T> clazz) {
        checkApplicationContext();
        try {
            return applicationContext.getBean(clazz);
        } catch (NoUniqueBeanDefinitionException e) {
            throw new IllegalStateException("存在多个 " + clazz.getName() + " 类型的 Bean，请通过名称指定或标记 @Primary。", e);
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalStateException("未找到类型为 " + clazz.getName() + " 的 Bean。", e);
        }
    }

    /**
     * 按名称获取 Bean（适用于同类型多个 Bean 的情况）
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        checkApplicationContext();
        return applicationContext.getBean(name, clazz);
    }


    /**
     * 检查 ApplicationContext 是否已初始化
     */
    private static void checkApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext 未初始化，请确保 BeanTool 已被 Spring 管理！");
        }
    }
}
