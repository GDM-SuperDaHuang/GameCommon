package com.slg.module.register;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.slg.module.util.ConfigReader;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

/**
 * Nacos服务注册与发现组件
 */
public class NacosServiceRegistry {
    private final NamingService namingService;
    private final String serviceName;
    private String ip;
    private int port;
    private boolean registered = false;

    public NacosServiceRegistry(ConfigReader config) throws NacosException {
        String serverAddr = config.getProperty("nacos.server.addr");
        this.serviceName = config.getProperty("nacos.service.name");
        
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        this.namingService = NacosFactory.createNamingService(properties);
        
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("无法获取本地IP地址", e);
        }
    }

    /**
     * 注册服务实例到Nacos
     */
    public void registerService(int port) {
        try {
            this.port = port;
            
            Instance instance = new Instance();
            instance.setIp(ip);
            instance.setPort(port);
            instance.setServiceName(serviceName);
            instance.setWeight(1.0);
            instance.setHealthy(true);
            instance.setEnabled(true);
            
            namingService.registerInstance(serviceName, instance);
            registered = true;
            System.out.println("===== 服务已注册到Nacos: " + serviceName + " (" + ip + ":" + port + ") =====");
        } catch (Exception e) {
            System.err.println("!!!!! Nacos服务注册失败 !!!!!");
            e.printStackTrace();
        }
    }

    /**
     * 从Nacos注销服务实例
     */
    public void deregisterService() {
        if (!registered) return;
        
        try {
            namingService.deregisterInstance(serviceName, ip, port);
            System.out.println("===== 服务已从Nacos注销: " + serviceName + " (" + ip + ":" + port + ") =====");
        } catch (Exception e) {
            System.err.println("!!!!! Nacos服务注销失败 !!!!!");
            e.printStackTrace();
        }
    }

    /**
     * 获取指定服务的所有实例
     */
    public List<Instance> getServiceInstances(String serviceName) throws NacosException {
        return namingService.getAllInstances(serviceName);
    }
    
    /**
     * 获取当前服务名称
     */
    public String getServiceName() {
        return serviceName;
    }
}
