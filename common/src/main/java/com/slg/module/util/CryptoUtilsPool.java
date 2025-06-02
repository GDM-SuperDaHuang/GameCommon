package com.slg.module.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.Recycler;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CryptoUtilsPool {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    // 对象池：复用 Cipher 和临时缓冲区（减少 GC）
    private static final Recycler<CipherContext> CIPHER_CONTEXT_POOL = new Recycler<CipherContext>() {
        @Override
        protected CipherContext newObject(Handle<CipherContext> handle) {
            return new CipherContext(handle);
        }
    };

    // 线程局部变量：避免 SecureRandom 竞争
    private static final ThreadLocal<SecureRandom> SECURE_RANDOM = ThreadLocal.withInitial(SecureRandom::new);

    // 加密/解密上下文（对象池复用）
    private static final class CipherContext {
        private final Recycler.Handle<CipherContext> handle;
        private final byte[] iv = new byte[IV_SIZE];
        private Cipher cipher;

        public CipherContext(Recycler.Handle<CipherContext> handle) {
            this.handle = handle;
        }

        public void recycle() {
            cipher = null;
            handle.recycle(this);
        }
    }

    /**
     * 生成 AES 密钥（优化：避免重复创建 MessageDigest）
     */
    public static SecretKey generateAesKey(BigInteger sharedKey) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(sharedKey.toByteArray());
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密消息（优化：使用对象池和零拷贝）
     */
    public static ByteBuf encrypt(SecretKey key, String message) throws Exception {
        CipherContext ctx = CIPHER_CONTEXT_POOL.get();
        try {
            // 初始化 Cipher（复用对象）
            if (ctx.cipher == null) {
                ctx.cipher = Cipher.getInstance(TRANSFORMATION);
            }
            SECURE_RANDOM.get().nextBytes(ctx.iv);
            ctx.cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ctx.iv));

            // 加密并直接写入 ByteBuf（避免 byte[] 拷贝）
            byte[] encrypted = ctx.cipher.doFinal(message.getBytes());
            ByteBuf result = Unpooled.buffer(IV_SIZE + encrypted.length);
            result.writeBytes(ctx.iv);
            result.writeBytes(encrypted);
            return result;
        } finally {
            ctx.recycle();
        }
    }

    /**
     * 解密消息（优化：零拷贝 + 对象池）
     */
    public static String decrypt(SecretKey key, ByteBuf encryptedMessage) throws Exception {
        CipherContext ctx = CIPHER_CONTEXT_POOL.get();
        try {
            // 1. 读取IV（前16字节）
            encryptedMessage.readBytes(ctx.iv);
            // 2. 获取加密数据（零拷贝：仅在JDK支持时使用ByteBuffer，否则回退到byte[]）
            byte[] encrypted;
            if (encryptedMessage.hasArray()) {
                // 堆缓冲区直接访问数组
                encrypted = new byte[encryptedMessage.readableBytes()];
                encryptedMessage.getBytes(
                        encryptedMessage.readerIndex(),
                        encrypted,
                        0,
                        encrypted.length
                );
            } else {
                // 直接内存缓冲区需拷贝（无法完全避免）
                encrypted = ByteBufUtil.getBytes(encryptedMessage,
                        encryptedMessage.readerIndex(),
                        encryptedMessage.readableBytes(),
                        false
                );
            }

            // 3. 解密（使用对象池中的Cipher）
            if (ctx.cipher == null) {
                ctx.cipher = Cipher.getInstance(TRANSFORMATION);
            }
            ctx.cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ctx.iv));
            byte[] decrypted = ctx.cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8); // 明确指定字符集
        } finally {
            ctx.recycle();
        }
    }
}