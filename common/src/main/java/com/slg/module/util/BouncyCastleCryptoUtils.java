package com.slg.module.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Recycler;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Objects;

/**
 * 基于 Bouncy Castle 的高性能加密工具类
 * 特性：
 * 1. 零拷贝加密/解密
 * 2. 对象池优化
 * 3. 完全支持 Netty ByteBuf
 * 4. 线程安全
 */
public final class BouncyCastleCryptoUtils {
    static {
        // 注册 Bouncy Castle 提供者
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String PROVIDER = "BC";
    private static final int IV_SIZE = 16;
    private static final ThreadLocal<SecureRandom> SECURE_RANDOM = ThreadLocal.withInitial(SecureRandom::new);

    // 对象池：加密上下文
    private static final Recycler<EncryptContext> ENCRYPT_CONTEXT_POOL = new Recycler<EncryptContext>() {
        @Override
        protected EncryptContext newObject(Handle<EncryptContext> handle) {
            return new EncryptContext(handle);
        }
    };

    // 对象池：解密上下文
    private static final Recycler<DecryptContext> DECRYPT_CONTEXT_POOL = new Recycler<DecryptContext>() {
        @Override
        protected DecryptContext newObject(Handle<DecryptContext> handle) {
            return new DecryptContext(handle);
        }
    };

    // 加密上下文（对象池复用）
    private static final class EncryptContext {
        private final Recycler.Handle<EncryptContext> handle;
        private final byte[] iv = new byte[IV_SIZE];
        private Cipher cipher;

        EncryptContext(Recycler.Handle<EncryptContext> handle) {
            this.handle = handle;
        }

        void recycle() {
            cipher = null;
            handle.recycle(this);
        }
    }

    // 解密上下文（对象池复用）
    private static final class DecryptContext {
        private final Recycler.Handle<DecryptContext> handle;
        private final byte[] iv = new byte[IV_SIZE];
        private Cipher cipher;

        DecryptContext(Recycler.Handle<DecryptContext> handle) {
            this.handle = handle;
        }

        void recycle() {
            cipher = null;
            handle.recycle(this);
        }
    }

    /**
     * AES 加密 (CBC 模式)
     * @param alloc ByteBuf 分配器
     * @param key 密钥
     * @param plaintext 明文 ByteBuf
     * @return 包含 IV + 密文的 ByteBuf
     */
    public static ByteBuf encryptAesCbc(ByteBufAllocator alloc, SecretKey key, ByteBuf plaintext) throws Exception {
        Objects.requireNonNull(alloc, "ByteBufAllocator cannot be null");
        Objects.requireNonNull(key, "SecretKey cannot be null");
        Objects.requireNonNull(plaintext, "Plaintext ByteBuf cannot be null");

        EncryptContext ctx = ENCRYPT_CONTEXT_POOL.get();
        try {
            // 初始化 Cipher
            if (ctx.cipher == null) {
                ctx.cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", PROVIDER);
            }

            // 生成随机 IV
            SECURE_RANDOM.get().nextBytes(ctx.iv);
            ctx.cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ctx.iv));

            // 计算输出缓冲区大小
            int outputSize = ctx.cipher.getOutputSize(plaintext.readableBytes());
            ByteBuf output = alloc.buffer(IV_SIZE + outputSize);

            try {
                // 写入 IV
                output.writeBytes(ctx.iv);

                // 零拷贝加密
                ByteBuffer inputBuffer = plaintext.nioBuffer();
                ByteBuffer outputBuffer = output.nioBuffer(IV_SIZE, outputSize);
                int encryptedSize = ctx.cipher.doFinal(inputBuffer, outputBuffer);
                output.writerIndex(IV_SIZE + encryptedSize);

                return output;
            } catch (Exception e) {
                output.release();
                throw e;
            }
        } finally {
            ctx.recycle();
        }
    }

    /**
     * AES 解密 (CBC 模式)
     * @param alloc ByteBuf 分配器
     * @param key 密钥
     * @param encrypted 包含 IV + 密文的 ByteBuf
     * @return 明文 ByteBuf
     */
    public static ByteBuf decryptAesCbc(ByteBufAllocator alloc, SecretKey key, ByteBuf encrypted) throws Exception {
        Objects.requireNonNull(alloc, "ByteBufAllocator cannot be null");
        Objects.requireNonNull(key, "SecretKey cannot be null");
        Objects.requireNonNull(encrypted, "Encrypted ByteBuf cannot be null");

        if (encrypted.readableBytes() < IV_SIZE) {
            throw new IllegalArgumentException("Encrypted data too short");
        }

        DecryptContext ctx = DECRYPT_CONTEXT_POOL.get();
        try {
            // 读取 IV
            encrypted.readBytes(ctx.iv);

            // 初始化 Cipher
            if (ctx.cipher == null) {
                ctx.cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", PROVIDER);
            }
            ctx.cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ctx.iv));

            // 计算输出缓冲区大小
            int outputSize = ctx.cipher.getOutputSize(encrypted.readableBytes());
            ByteBuf output = alloc.buffer(outputSize);

            try {
                // 零拷贝解密
                ByteBuffer inputBuffer = encrypted.nioBuffer();
                ByteBuffer outputBuffer = output.nioBuffer(0, outputSize);
                int decryptedSize = ctx.cipher.doFinal(inputBuffer, outputBuffer);
                output.writerIndex(decryptedSize);

                return output;
            } catch (Exception e) {
                output.release();
                throw e;
            }
        } finally {
            ctx.recycle();
        }
    }

    // 私有构造，防止实例化
    private BouncyCastleCryptoUtils() {}
}