package com.slg.module.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
public class MsgUtil {
    private MsgUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    /**
     * 客户端消息
     */
    public static ByteBuf buildClientMsg(int cid, int errorCode, int protocolId, byte zip, byte encrypted, short length, ByteBuf body) {
        //写回
        ByteBuf out = Unpooled.buffer(16 + length);
        //消息头
        out.writeInt(cid);      // 4字节
        out.writeInt(errorCode);   // 4字节
        out.writeInt(protocolId);  // 4字节
        out.writeByte(zip);         // zip压缩标志，1字节
        out.writeByte(encrypted);  // 加密标志，1字节
        //消息体
        out.writeShort(length);   // 消息体长度，2字节
        // 写入消息体
        if (body!=null){
            out.writeBytes(body);
        }
        return out;
    }

    /**
     * 服务器信息
     */
    public static ByteBuf buildServerMsg(long userId, int cid, int errorCode, int protocolId, int zip, int encrypted, short length, ByteBuf body) {
        //写回
        ByteBuf out = Unpooled.buffer(24 + length);
        //消息头
        out.writeLong(userId);      // 8字节
        out.writeInt(cid);      // 4字节
        out.writeInt(errorCode);      // 4字节
        out.writeInt(protocolId);      // 4字节
        out.writeByte(zip);                       // zip压缩标志，1字节
        out.writeByte(encrypted);                       // 加密标志，1字节
        //消息体
        out.writeShort(length);                 // 消息体长度，2字节
        // 写入消息体
        if (body!=null){
            out.writeBytes(body);
        }
        return out;
    }

}
