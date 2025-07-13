package com.slg.module.util;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;

public class NacosClientUtil {
    private static volatile NacosClientUtil instance;
    private final String serverAddr;
    private final String namespace;
    private NamingService namingService;
    private ConfigService configService;

    public static NacosClientUtil getAlreadyInstance() {
        if (instance == null) return null;
        return instance;
    }

    /**
     * 构造函数，初始化Nacos客户端
     *
     * @param serverAddr Nacos服务器地址
     * @param namespace  命名空间ID
     */
    private NacosClientUtil(String serverAddr, String namespace) {
        this.serverAddr = serverAddr;
        this.namespace = namespace;
        init();
    }

    // 初始化方法
    private void init() {
        try {
            Properties namingProps = new Properties();
            namingProps.put(SERVER_ADDR, serverAddr);
            namingProps.put(NAMESPACE, namespace);
            namingService = NamingFactory.createNamingService(namingProps);
            Properties configProps = new Properties();
            configProps.put(SERVER_ADDR, serverAddr);
            configProps.put(NAMESPACE, namespace);
            configService = NacosFactory.createConfigService(configProps);
        } catch (NacosException e) {
            throw new RuntimeException("初始化Nacos客户端失败", e);
        }
    }

    // 双重检查锁定实现单例
    public static NacosClientUtil getInstance(String serverAddr, String namespace) {
        if (instance == null) {
            synchronized (NacosClientUtil.class) {
                if (instance == null) {
                    instance = new NacosClientUtil(serverAddr, namespace);
                }
            }
        }
        return instance;
    }

    // 获取已初始化的实例（需先调用带参数的getInstance方法）
    public static NacosClientUtil getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NacosClientUtil尚未初始化，请先调用getInstance(serverAddr, namespace)");
        }
        return instance;
    }

    // ==================== 服务注册与发现相关方法 ====================

    /**
     * 注册服务实例
     *
     * @param serviceName 服务名
     * @param groupName   组名
     * @param ip          IP地址
     * @param port        端口
     * @param weight      权重
     * @param metadata    元数据
     * @throws NacosException 注册异常
     */
    public void registerInstance(String instanceId, String serviceName, String groupName, String ip, int port, double weight, Map<String, String> metadata) throws NacosException {
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setServiceName(serviceName);
        instance.setIp(ip);
        instance.setPort(port);
        instance.setWeight(weight);
        instance.setMetadata(metadata);
        namingService.registerInstance(serviceName, groupName, instance);
    }

    /**
     * 注册服务实例，使用默认组
     */
    public void registerInstance(String instanceId, String serviceName, String ip, int port) throws NacosException {
        registerInstance(instanceId, serviceName, DEFAULT_GROUP, ip, port, 1.0, new HashMap<>());
    }

    /**
     * 注销服务实例
     */
    public void deregisterInstance(String serviceName, String groupName, String ip, int port) throws NacosException {
        namingService.deregisterInstance(serviceName, groupName, ip, port);
    }

    /**
     * 获取服务实例列表
     */
    public List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException {
        return namingService.getAllInstances(serviceName, groupName);
    }

    /**
     * 获取健康的服务实例列表
     */
    public List<Instance> getHealthyInstances(String serviceName, String groupName) throws NacosException {
        return namingService.selectInstances(serviceName, groupName, true);
    }

    // ==================== 配置管理相关方法 ====================

    /**
     * 获取配置
     *
     * @param dataId  配置ID
     * @param group   配置组
     * @param timeout 超时时间(ms)
     */
    public String getConfig(String dataId, String group, long timeout) throws NacosException {
        return configService.getConfig(dataId, group, timeout);
    }

    /**
     * 发布配置
     *
     * @param dataId  配置ID
     * @param group   配置组
     * @param content 配置内容
     */
    public boolean publishConfig(String dataId, String group, String content) throws NacosException {
        return configService.publishConfig(dataId, group, content);
    }

    /**
     * 删除配置
     */
    public boolean deleteConfig(String dataId, String group) throws NacosException {
        return configService.removeConfig(dataId, group);
    }

    /**
     * 添加配置监听器
     *
     * @param dataId   配置ID
     * @param group    配置组
     * @param listener 配置变更监听器
     */
    public void addListener(String dataId, String group, Listener listener) throws NacosException {
        configService.addListener(dataId, group, listener);
    }

    /**
     * 移除配置监听器
     */
    public void removeListener(String dataId, String group, Listener listener) throws NacosException {
        configService.removeListener(dataId, group, listener);
    }

    /**
     * 监听服务实例状态变更
     */
    public void listenServiceStatus(String serviceName, String groupName, EventListener listener) throws NacosException {
        namingService.subscribe(serviceName, groupName, listener);
    }

    /**
     * 监听 Nacos 服务器集群状态变更
     */
    public void listenServerStatus(EventListener listener) throws NacosException {
        namingService.subscribe(null, listener);
    }

    /**
     * 停止监听服务实例状态
     */
    public void stopListenServiceStatus(String serviceName, String groupName, EventListener listener) throws NacosException {
        namingService.unsubscribe(serviceName, groupName, listener);
    }

    /**
     * 停止监听 Nacos 服务器状态
     */
    public void stopListenServerStatus(EventListener listener) throws NacosException {
        namingService.unsubscribe(null, listener);
    }

    /**
     * 简化的配置监听器实现，使用lambda表达式
     */
    public static abstract class SimpleListener implements Listener {
        private final AtomicBoolean shutdown = new AtomicBoolean(false);

        @Override
        public Executor getExecutor() {
            return null; // 使用默认线程池
        }

        @Override
        public final void receiveConfigInfo(String configInfo) {
            if (!shutdown.get()) {
                onConfigChanged(configInfo);
            }
        }

        /**
         * 配置变更时的回调方法
         *
         * @param configInfo 变更后的配置内容
         */
        public abstract void onConfigChanged(String configInfo);

        /**
         * 关闭监听器
         */
        public void shutdown() {
            shutdown.set(true);
        }
    }

    // ==================== 连接状态相关方法 ====================

    /**
     * 检查客户端是否已连接到Nacos服务器
     */
    public boolean isConnected() {
        try {
            // 简单检查，实际生产环境可能需要更健壮的实现
            namingService.getServerStatus();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭客户端资源
     */
    public void close() {
        try {
            if (namingService != null) {
                namingService.shutDown();
            }
            if (configService != null) {
                // ConfigService没有显式的关闭方法
            }
        } catch (Exception e) {
            throw new RuntimeException("关闭Nacos客户端失败", e);
        }
    }
}