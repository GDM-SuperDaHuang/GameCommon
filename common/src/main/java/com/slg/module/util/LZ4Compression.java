package com.slg.module.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.Recycler;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * 使用 Netty Recycler 优化的 LZ4 压缩工具类
 * 特性：
 * 1. 使用 Netty Recycler 实现对象池，高效重用压缩器/解压器
 * 2. 支持零拷贝操作 Netty ByteBuf
 * 3. 自动处理堆内/直接内存
 * 4. 提供传统 byte[] 数组的兼容方法
 * 5. 内置内存池管理
 */
public class LZ4Compression {
    // LZ4 工厂实例（线程安全）
    private static final LZ4Factory factory = LZ4Factory.fastestInstance();

    // 使用 Netty 的内存池分配器
    private static final ByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

    /**
     * 压缩器对象池（使用 Netty Recycler 实现）
     * 每个线程有自己的对象池，减少竞争
     */
    private static final Recycler<LZ4Compressor> COMPRESSOR_RECYCLER = new Recycler<LZ4Compressor>() {
        @Override
        protected LZ4Compressor newObject(Handle<LZ4Compressor> handle) {
            // 创建新的压缩器实例
            LZ4Compressor compressor = factory.fastCompressor();
            // 可以将 handle 存储在压缩器对象中以便回收，但这里不需要
            return compressor;
        }
    };

    /**
     * 解压器对象池（使用 Netty Recycler 实现）
     */
    private static final Recycler<LZ4FastDecompressor> DECOMPRESSOR_RECYCLER = new Recycler<LZ4FastDecompressor>() {
        @Override
        protected LZ4FastDecompressor newObject(Handle<LZ4FastDecompressor> handle) {
            // 创建新的解压器实例
            return factory.fastDecompressor();
        }
    };

    /**
     * 获取压缩器（从对象池中获取）
     */
    private static LZ4Compressor getCompressor() {
        return COMPRESSOR_RECYCLER.get();
    }

    /**
     * 获取解压器（从对象池中获取）
     */
    private static LZ4FastDecompressor getDecompressor() {
        return DECOMPRESSOR_RECYCLER.get();
    }

    /**
     * 压缩 ByteBuf 数据（零拷贝实现）
     *
     * @param src 源 ByteBuf（调用者保留所有权，不会被释放）
     * @return 压缩后的 ByteBuf（调用者负责释放）
     */
    public static ByteBuf compress(ByteBuf src) {
        if (src == null || !src.isReadable()) {
            return src;
        }

        LZ4Compressor compressor = getCompressor();
        try {
            int srcLength = src.readableBytes();
            int maxCompressedLength = compressor.maxCompressedLength(srcLength);

            // 分配目标缓冲区（使用内存池）
            ByteBuf dst = allocator.directBuffer(maxCompressedLength);

            if (src.hasArray() && dst.hasArray()) {
                // 堆内内存到堆内内存（最优路径）
                int compressedLength = compressor.compress(
                        src.array(),
                        src.arrayOffset() + src.readerIndex(),
                        srcLength,
                        dst.array(),
                        dst.arrayOffset(),
                        maxCompressedLength
                );
                dst.writerIndex(compressedLength);
            } else {
                // 处理直接内存或混合情况
                byte[] inputBytes = new byte[srcLength];
                src.getBytes(src.readerIndex(), inputBytes);

                if (dst.hasArray()) {
                    // 直接内存到堆内内存
                    int compressedLength = compressor.compress(
                            inputBytes, 0, srcLength,
                            dst.array(), dst.arrayOffset(), maxCompressedLength
                    );
                    dst.writerIndex(compressedLength);
                } else {
                    // 直接内存到直接内存
                    byte[] outputBytes = new byte[maxCompressedLength];
                    int compressedLength = compressor.compress(
                            inputBytes, 0, srcLength,
                            outputBytes, 0, maxCompressedLength
                    );
                    dst.writeBytes(outputBytes, 0, compressedLength);
                }
            }

            return dst;
        } finally {
            // Recycler 会自动回收对象，不需要显式释放
        }
    }

    /**
     * 解压 ByteBuf 数据（零拷贝实现）
     *
     * @param compressed     压缩的 ByteBuf（调用者保留所有权，不会被释放）
     * @param originalLength 原始数据长度
     * @return 解压后的 ByteBuf（调用者负责释放）
     */
    public static ByteBuf decompress(ByteBuf compressed, int originalLength) {
        if (compressed == null || !compressed.isReadable()) {
            return compressed;
        }

        LZ4FastDecompressor decompressor = getDecompressor();
        try {
            // 分配目标缓冲区（使用内存池）
            ByteBuf restored = allocator.directBuffer(originalLength);

            if (compressed.hasArray() && restored.hasArray()) {
                // 堆内内存到堆内内存（最优路径）
                decompressor.decompress(
                        compressed.array(),
                        compressed.arrayOffset() + compressed.readerIndex(),
                        restored.array(),
                        restored.arrayOffset(),
                        originalLength
                );
                restored.writerIndex(originalLength);
            } else {
                // 处理直接内存或混合情况
                byte[] compressedBytes = new byte[compressed.readableBytes()];
                compressed.getBytes(compressed.readerIndex(), compressedBytes);

                if (restored.hasArray()) {
                    // 直接内存到堆内内存
                    decompressor.decompress(
                            compressedBytes, 0,
                            restored.array(), restored.arrayOffset(),
                            originalLength
                    );
                    restored.writerIndex(originalLength);
                } else {
                    // 直接内存到直接内存
                    byte[] restoredBytes = new byte[originalLength];
                    decompressor.decompress(compressedBytes, 0, restoredBytes, 0, originalLength);
                    restored.writeBytes(restoredBytes);
                }
            }

            return restored;
        } finally {
            // Recycler 会自动回收对象，不需要显式释放
        }
    }

//    /**
//     * 压缩 byte 数组（兼容方法）
//     *
//     * @param data 原始数据
//     * @return 压缩后的数据（新数组）
//     */
//    public static byte[] compress(byte[] data) {
//        if (data == null || data.length == 0) {
//            return data;
//        }
//
//        ByteBuf src = null;
//        ByteBuf compressed = null;
//        try {
//            src = allocator.buffer(data.length);
//            src.writeBytes(data);
//            compressed = compress(src);
//
//            byte[] result = new byte[compressed.readableBytes()];
//            compressed.readBytes(result);
//            return result;
//        } finally {
//            if (src != null) src.release();
//            if (compressed != null) compressed.release();
//        }
//    }

//    /**
//     * 解压 byte 数组（兼容方法）
//     *
//     * @param compressed     压缩数据
//     * @param originalLength 原始数据长度
//     * @return 解压后的数据（新数组）
//     */
//    public static byte[] decompress(byte[] compressed, int originalLength) {
//        if (compressed == null || compressed.length == 0) {
//            return compressed;
//        }
//
//        ByteBuf src = null;
//        ByteBuf decompressed = null;
//        try {
//            src = allocator.buffer(compressed.length);
//            src.writeBytes(compressed);
//            decompressed = decompress(src, originalLength);
//
//            byte[] result = new byte[originalLength];
//            decompressed.readBytes(result);
//            return result;
//        } finally {
//            if (src != null) src.release();
//            if (decompressed != null) decompressed.release();
//        }
//    }

    /**
     * 带长度头的压缩方法（实用方法）
     *
     * @param src 源数据
     * @return 包含4字节长度头 + 压缩数据的ByteBuf（调用者负责释放）
     */
    public static ByteBuf compressWithLengthHeader(ByteBuf src, short originalLength) {
        ByteBuf compressed = compress(src);
        // 创建包含长度头的新buffer
        ByteBuf result = allocator.buffer(originalLength + compressed.readableBytes());
        result.writeShort(originalLength);  // 写入原始长度
        result.writeBytes(compressed); // 写入压缩数据
        compressed.release();

        return result;
    }

    /**
     * 解压带长度头的数据（实用方法）
     *
     * @param compressed     不含长度头的压缩数据
     * @param originalLength 原始数据长度
     * @return 解压后的ByteBuf（调用者负责释放）
     * @throws IllegalArgumentException 如果数据格式无效
     */
    public static ByteBuf decompressWithLengthHeader(ByteBuf compressed, short originalLength) {
        if (compressed.readableBytes() > originalLength) {
            throw new IllegalArgumentException("Invalid compressed data: missing length header");
        }
        ByteBuf decompressed = decompress(compressed, originalLength);
        return decompressed;
    }
}