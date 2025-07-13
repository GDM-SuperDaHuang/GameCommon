package com.slg.module.connection;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.slg.module.config.ServerConfig;
import com.slg.module.message.Constants;
import com.slg.module.util.NacosClientUtil;

import java.util.*;


public class ServerConfigManager {
    private static volatile ServerConfigManager instance;
    NacosClientUtil client = NacosClientUtil.getAlreadyInstance();
    private HashMap<String, ServerConfig> serverConfigMap = new HashMap<>();//所有可能的健康的实例
    //有序从小到大排序
    private List<List<ServerConfig>> serverConfigList = new ArrayList<>();


    private ServerConfigManager(String serviceName, String groupName, String configName, String excludeInstanceId) {
        this.init(serviceName, groupName, configName, excludeInstanceId);
    }

    //初始化
    private void init(String serviceName, String groupName, String configName, String excludeInstanceId) {
        try {
            if (configName != null) {
                String config = client.getConfig(
                        configName,  // 配置ID node.properties
                        groupName,         // 配置组
                        5000                     // 超时时间(ms)
                );
            }
            refreshServerInstances(client.getAllInstances(serviceName, groupName), excludeInstanceId);
            // 监听服务实例状态
            client.listenServiceStatus(serviceName, groupName, new EventListener() {
                @Override
                public void onEvent(Event event) {
                    if (event instanceof NamingEvent) {
                        NamingEvent changeEvent = (NamingEvent) event;
                        System.out.println("服务实例变更: " + changeEvent.getServiceName());
                        List<Instance> instances = changeEvent.getInstances();
                        // 处理实例状态变化
                        refreshServerInstances(instances, excludeInstanceId);

                    }
                }
            });

            //配置变更
            client.addListener(
                    "node.properties",  // 配置ID
                    groupName,         // 配置组
                    new NacosClientUtil.SimpleListener() {
                        @Override
                        public void onConfigChanged(String configInfo) {
                            System.out.println("配置已更新: " + configInfo);
                            // TODO: 处理配置变更（如重新加载配置）

                        }
                    });

        } catch (NacosException var3) {
            throw new RuntimeException("初始化Nacos客户端失败", var3);
        }
    }

    public void refreshServerInstances(List<Instance> allInstances, String excludeInstanceId) {
        for (Instance allInstance : allInstances) {
            // 排除当前实例
            if (allInstance.getInstanceId().equals(excludeInstanceId)) {
                System.out.println("跳过当前实例: " + allInstance.getInstanceId());
                continue;
            }
            if (!allInstance.isHealthy()) {
                continue;
            }
            Map<String, String> metadata = allInstance.getMetadata();
            ServerConfig serverConfig = new ServerConfig();
            String instanceId = allInstance.getInstanceId();
            int protoMaxId = Integer.parseInt(metadata.get(Constants.ProtoMaxId));
            int protoMixId = Integer.parseInt(metadata.get(Constants.ProtoMinId));
            int groupId = Integer.parseInt(metadata.get(Constants.GroupId));
            serverConfig.setServerId(Integer.parseInt(instanceId));
            serverConfig.setMaxProtoId(protoMaxId);
            serverConfig.setMinProtoId(protoMixId);
            serverConfig.setGroupId(groupId);
            serverConfig.setPort(allInstance.getPort());
            serverConfig.setHost(allInstance.getIp());
            serverConfigMap.put(instanceId, serverConfig);
        }
        //排序
        // 执行排序和分组操作
        serverConfigList = sortAndGroupByMinProtoId(serverConfigMap);
    }


    public static ServerConfigManager getInstance(String serviceName, String groupName, String configName, String
            excludeInstanceId) {
        if (instance == null) {
            synchronized (NacosClientUtil.class) {
                if (instance == null) {
                    instance = new ServerConfigManager(serviceName, groupName, configName, excludeInstanceId);
                }
            }
        }
        return instance;
    }

    public static ServerConfigManager getAlreadyInstance() {
        if (instance == null) return null;
        return instance;
    }

    public static List<List<ServerConfig>> sortAndGroupByMinProtoId(Map<String, ServerConfig> serverConfigMap) {
        // 首先，从 Map 中提取所有的 ServerConfig 对象
        List<ServerConfig> allConfigs = new ArrayList<>(serverConfigMap.values());

        // 然后，按照 minProtoId 对这些对象进行排序
        allConfigs.sort(Comparator.comparingInt(ServerConfig::getMinProtoId));

        // 接着，创建一个列表用于存储分组结果
        List<List<ServerConfig>> result = new ArrayList<>();
        if (allConfigs.isEmpty()) {
            return result;
        }

        // 对排序后的对象进行遍历，将相同 minProtoId 的对象分组
        List<ServerConfig> currentGroup = new ArrayList<>();
        currentGroup.add(allConfigs.get(0));
        int currentMinProtoId = allConfigs.get(0).getMinProtoId();

        for (int i = 1; i < allConfigs.size(); i++) {
            ServerConfig config = allConfigs.get(i);
            if (config.getMinProtoId() == currentMinProtoId) {
                // 如果当前对象的 minProtoId 与当前组的相同，就将其添加到当前组
                currentGroup.add(config);
            } else {
                // 若不同，就将当前组添加到结果列表，并创建一个新组
                result.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(config);
                currentMinProtoId = config.getMinProtoId();
            }
        }

        // 把最后一个组添加到结果列表
        result.add(currentGroup);

        return result;
    }

    // 找到一组相同类型的实例
    public List<ServerConfig> getChannelKey(int protocolId) {
        for (List<ServerConfig> serverConfigs : serverConfigList) {
            for (ServerConfig serverConfig : serverConfigs) {
                if (protocolId > serverConfig.getMaxProtoId()) {
                    break;
                } else {//找到目标
                    return serverConfigs;
                }
            }
        }
        return null;
    }
}
