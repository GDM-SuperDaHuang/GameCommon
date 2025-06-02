package com.slg.module.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class MsgUtil {
    private MsgUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }


    /**
     * 客户端消息
     * 如果返回的out，写入网络时候会自动释放，没写入网络要手动释放，composite写入网络也不释放，需要手动释放，
     * composite 释放时会自动释放 out
     *
     * @param ctx
     * @param cid
     * @param errorCode
     * @param protocolId
     * @param zip
     * @param encrypted
     * @param length
     * @param body
     * @return
     */
    public static ByteBuf buildClientMsg(ChannelHandlerContext ctx, int cid, int errorCode, int protocolId, byte zip, byte encrypted, short length, ByteBuf body) {
        //写回
        ByteBuf out = ctx.alloc().buffer(16 + length);
        //消息头
        out.writeInt(cid);      // 4字节
        out.writeInt(errorCode);   // 4字节
        out.writeInt(protocolId);  // 4字节
        out.writeByte(zip);         // zip压缩标志，1字节
        out.writeByte(encrypted);  // 加密标志，1字节
        //消息体
        out.writeShort(length);   // 消息体长度，2字节
        // 写入消息体
        if (body != null) {
            // 使用CompositeByteBuf组合头部和体部，避免复制
            CompositeByteBuf composite = Unpooled.compositeBuffer();
            composite.addComponent(true, out);
            composite.addComponent(true, body);
            return composite;
        }
        return out;
    }

    /**
     * 服务器信息
     */
    public static ByteBuf buildServerMsg(ChannelHandlerContext ctx, long userId, int cid, int errorCode, int protocolId, int zip, int encrypted, short length, ByteBuf body) {
        //写回
        ByteBuf out = ctx.alloc().buffer(24 + length);
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
        if (body != null) {
            // 使用CompositeByteBuf组合头部和体部，避免复制
            CompositeByteBuf composite = Unpooled.compositeBuffer();
            composite.addComponent(true, out);
            composite.addComponent(true, body);
            return composite;
        }
        return out;
    }

}
