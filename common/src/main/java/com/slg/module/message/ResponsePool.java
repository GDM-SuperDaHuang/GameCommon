//package com.slg.module.message;
//
//import com.google.protobuf.GeneratedMessage;
//import org.vibur.objectpool.ConcurrentPool;
//import org.vibur.objectpool.PoolObjectFactory;
//import org.vibur.objectpool.PoolService;
//import org.vibur.objectpool.util.ConcurrentCollection;
//import org.vibur.objectpool.util.ConcurrentLinkedQueueCollection;
//import org.vibur.objectpool.util.MultithreadConcurrentQueueCollection;
//
//public class ResponsePool {
//
//    final static private byte DefaultValue = 0;
//    private static final ResponsePool INSTANCE = new ResponsePool();
//    private final PoolService<MsgResponse> pool;
//
//    private ResponsePool() {
//        // 创建队列集合 - 参数为每个线程的队列容量
////        ConcurrentCollection<MsgResponse> queueCollection =
////                new MultithreadConcurrentQueueCollection<>(128);
//        ConcurrentCollection<MsgResponse> queueCollection =
//                new ConcurrentLinkedQueueCollection<>();
//
//        // 创建对象工厂
//        PoolObjectFactory<MsgResponse> factory = new MsgResponseFactory();
//
//        // 使用正确的构造函数参数
//        this.pool = new ConcurrentPool<>(
//                queueCollection,   // 队列集合
//                factory,           // 对象工厂
//                5,      // 初始大小 (最小空闲对象数)
//                3000,    // 最大大小 (最大对象数)
//                true,   // 公平模式
//                null    // 监听器
//        );
//    }
//
//    // 获取单例实例
//    public static ResponsePool getInstance() {
//        return INSTANCE;
//    }
//
//    // 从池中借出对象
//    public MsgResponse newInstance(GeneratedMessage.Builder<?> body) {
//        MsgResponse msg = pool.take();
//        msg.errorCode = ErrorCodeConstants.SUCCESS;
//        msg.body = body;
//        return msg;
//    }
//
//    public MsgResponse newInstance(int errorCode) {
//        MsgResponse msg = pool.take();
//        msg.errorCode = errorCode;
//        msg.body = null;
//        return msg;
//    }
//
//    public MsgResponse newInstance(GeneratedMessage.Builder<?> body, boolean encrypted) {
//        MsgResponse msg = pool.take();
//        msg.errorCode = ErrorCodeConstants.SUCCESS;
//        msg.body = body;
//        byte msgFlag = msg.flag;
//        if (encrypted) {
//            msgFlag |= Constants.ENCRYPTION_MASK; // 设置加密位
//        }
//        msg.flag = msgFlag;
//        return msg;
//    }
//
//    // 归还对象到池
//    public void returnObject(MsgResponse message) {
//        if (message != null) {
//            pool.restore(message);
//        }
//    }
//
//    // 关闭对象池
//    public void shutdown() {
//        if (pool instanceof ConcurrentPool) {
//            ((ConcurrentPool<MsgResponse>) pool).terminate();
//        }
//    }
//
//    // 对象工厂实现
//    private static class MsgResponseFactory
//            implements PoolObjectFactory<MsgResponse> {
//
//        @Override
//        public MsgResponse create() {
//            // 创建新对象
//            return new MsgResponse();
//        }
//
//
//        // 判断对象是否可用，可用直接返回，否则调用create
//        @Override
//        public boolean readyToTake(MsgResponse obj) {
//            // 对象取出前的准备
//            // 确保 ByteBuf 已被释放
//            // 重置基本类型字段
//            obj.body = null;
//            obj.errorCode = 0;
//            obj.flag = DefaultValue;
//            return true; // 对象准备好被使用
//        }
//
//
//        // 释放，不可直接调用
//        @Override
//        public boolean readyToRestore(MsgResponse obj) {
//            obj.body = null;
//            obj.errorCode = 0;
//            obj.flag = 0;
//            return true; // 对象准备好被放回池中
//        }
//
//        @Override
//        public void destroy(MsgResponse obj) {
//            // 销毁对象时释放资源
//            obj.body = null;
//            obj.errorCode = 0;
//            obj.flag = 0;
//        }
//
//    }
//}