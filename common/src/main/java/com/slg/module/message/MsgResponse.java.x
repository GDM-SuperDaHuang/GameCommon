package com.slg.module.message;

import com.google.protobuf.GeneratedMessage;
import org.agrona.concurrent.ManyToManyConcurrentArrayQueue;
import java.util.concurrent.atomic.AtomicInteger;
public class MsgResponse {
    private int errorCode;
    private GeneratedMessage.Builder<?> body;
    private byte flag;//先压缩后加密原则
    //0000 0011:压缩+加密
    //0000 0001:加密
    //0000 0010:压缩
    public boolean isEncrypted() {
        return (flag & Constants.ENCRYPTION_MASK) != 0;
    }

    // Agrona 无锁队列（替代 ObjectPool）
    private static final ManyToManyConcurrentArrayQueue<MsgResponse> OBJECT_QUEUE =
            new ManyToManyConcurrentArrayQueue<>(8192); // 初始容量

    // 状态标记 (0=空闲, 1=使用中)
    private final AtomicInteger state = new AtomicInteger(0);

    // 获取实例（支持虚拟线程）
    public static MsgResponse newInstance(GeneratedMessage.Builder<?> body) {
        MsgResponse msg = OBJECT_QUEUE.poll(); // 无锁获取
        if (msg == null) {
            msg = new MsgResponse();
        }
        if (!msg.state.compareAndSet(0, 1)) {
            throw new IllegalStateException("Acquired message in invalid state");
        }
        // 初始化元数据
        msg.body = body;
        return msg;
    }
    public static MsgResponse newInstance(int errorCode) {
        MsgResponse msg = OBJECT_QUEUE.poll(); // 无锁获取
        msg.errorCode = errorCode;
        msg.body = null;
        return msg;
    }
    public static MsgResponse newInstance(GeneratedMessage.Builder<?> body, boolean encrypted) {
        MsgResponse msg = OBJECT_QUEUE.poll(); // 无锁获取
        msg.errorCode = ErrorCodeConstants.SUCCESS;
        msg.body = body;
        byte msgFlag = msg.flag;
        if (encrypted) {
            msgFlag |= Constants.ENCRYPTION_MASK; // 设置加密位
        }
        msg.flag = msgFlag;
        return msg;
    }


    // 回收对象（线程安全）
    public void recycle() {
        body = null;
        errorCode = 0;
        flag = 0;
        // 标记为空闲并放回队列
        if (state.compareAndSet(1, 0)) {
            OBJECT_QUEUE.offer(this); // 无锁放回
        } else {
            throw new IllegalStateException("Message already recycled");
        }
    }

    // 安全回收方法（防泄漏）
    public static void safeRecycle(MsgResponse msg) {
        if (msg != null && msg.state.get() == 1) {
            msg.recycle();
        }
    }

    public int getErrorCode() {
        return errorCode;
    }

    public GeneratedMessage.Builder<?> getBody() {
        return body;
    }

    public byte getFlag() {
        return flag;
    }
}
