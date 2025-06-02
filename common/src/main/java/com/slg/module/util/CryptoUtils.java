package com.slg.module.util;

import io.netty.buffer.ByteBuf;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

public class CryptoUtils {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 从共享密钥生成AES密钥
     */
    public static SecretKey generateAesKey(BigInteger sharedKey) throws NoSuchAlgorithmException {
        Objects.requireNonNull(sharedKey, "Shared key cannot be null");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(sharedKey.toByteArray());
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 加密ByteBuf消息
     */
    public static ByteBuf encrypt(SecretKey key, ByteBuf message) throws Exception {
        Objects.requireNonNull(key, "Secret key cannot be null");
        Objects.requireNonNull(message, "Message cannot be null");

        byte[] iv = new byte[IV_SIZE];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, key, iv);

        byte[] plaintext = new byte[message.readableBytes()];
        message.readBytes(plaintext);

        byte[] encrypted = cipher.doFinal(plaintext);

        // 创建新的ByteBuf并写入IV和加密数据
        ByteBuf result = message.alloc().buffer(IV_SIZE + encrypted.length);
        result.writeBytes(iv);
        result.writeBytes(encrypted);

        return result;
    }

    /**
     * 解密ByteBuf消息
     */
    public static ByteBuf decrypt(SecretKey key, ByteBuf encryptedMessage) throws Exception {
        Objects.requireNonNull(key, "Secret key cannot be null");
        Objects.requireNonNull(encryptedMessage, "Encrypted message cannot be null");

        if (encryptedMessage.readableBytes() < IV_SIZE) {
            throw new IllegalArgumentException("Encrypted message is too short to contain IV");
        }

        byte[] iv = new byte[IV_SIZE];
        encryptedMessage.readBytes(iv);

        byte[] encrypted = new byte[encryptedMessage.readableBytes()];
        encryptedMessage.readBytes(encrypted);

        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, key, iv);
        byte[] decrypted = cipher.doFinal(encrypted);

        // 创建新的ByteBuf并写入解密数据
        return encryptedMessage.alloc().buffer(decrypted.length).writeBytes(decrypted);
    }

    private static Cipher createCipher(int mode, SecretKey key, byte[] iv) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, key, new IvParameterSpec(iv));
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IllegalStateException("Failed to initialize cipher", e);
        }
    }
}