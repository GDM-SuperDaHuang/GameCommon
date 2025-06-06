package com.slg.module.message;

import com.google.protobuf.GeneratedMessage;
import io.netty.util.Recycler;

import java.util.Objects;

public class MsgResponse {
    private int errorCode;
    private GeneratedMessage.Builder<?> body;
    private byte flag;//先压缩后加密原则
    //0000 0011:压缩+加密
    //0000 0001:加密
    //0000 0010:压缩

    private static final Recycler<MsgResponse> RECYCLER = new Recycler<MsgResponse>() {
        @Override
        protected MsgResponse newObject(Handle<MsgResponse> handle) {
            return new MsgResponse(handle);
        }
    };

    private final Recycler.Handle<MsgResponse> handle; // final字段

    // 必须的私有构造器
    private MsgResponse(Recycler.Handle<MsgResponse> handle) {
        this.handle = Objects.requireNonNull(handle);
    }

    // 从对象池获取实例（传入 ByteBuf 直接引用）
    public static MsgResponse newInstance(GeneratedMessage.Builder<?> body) {
        MsgResponse msg = RECYCLER.get();
        msg.errorCode = ErrorCodeConstants.SUCCESS;
        msg.body = body;
        return msg;
    }

    public static MsgResponse newInstance(int errorCode) {
        MsgResponse msg = RECYCLER.get();
        msg.errorCode = errorCode;
        msg.body = null;
        return msg;
    }

    public static MsgResponse newInstance(GeneratedMessage.Builder<?> body, boolean encrypted) {
        MsgResponse msg = RECYCLER.get();
        msg.errorCode = ErrorCodeConstants.SUCCESS;
        msg.body = body;
        byte msgFlag = msg.flag;
        if (encrypted) {
            msgFlag |= Constants.ENCRYPTION_MASK; // 设置加密位
        }
        msg.flag = msgFlag;
        return msg;
    }

    // 检查是否加密
    public boolean isEncrypted() {
        return (flag & Constants.ENCRYPTION_MASK) != 0;
    }


    // 设置加密标志
    public byte setEncrypted(boolean encrypted) {
        if (encrypted) {
            flag |= Constants.ENCRYPTION_MASK; // 设置加密位
        } else {
            flag &= ~Constants.ENCRYPTION_MASK; // 清除加密位
        }
        return flag;
    }

    // 回收对象
    public void recycle() {
        flag = 0;
        errorCode = 0;
        body = null;
        handle.recycle(this);
    }


    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public byte getFlag() {
        return flag;
    }

    public GeneratedMessage.Builder<?> getBody() {
        return body;
    }

    public void setBody(GeneratedMessage.Builder<?> body) {
        this.body = body;
    }
}
