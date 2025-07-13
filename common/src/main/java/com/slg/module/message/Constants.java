package com.slg.module.message;

//常量
public final class Constants {
    // 防止实例化该类
    private Constants() {
    }

    public static final String ProtoMinId = "min";
    public static final String ProtoMaxId = "max";
    public static final String GroupId = "g";
    public static final String ServerId = "s";

    // 标志位掩码
    public static final byte COMPRESSION_MASK = 0b00000010; // 压缩标志位
    public static final byte ENCRYPTION_MASK = 0b00000001;  // 加密标志位
    public static long HeartTime = 30000;
    //不压缩
    public static byte NoZip = 0;
    public static byte Zip = 1;
    //不加密
    public static byte NoEncrypted = 0;
    public static byte Encrypted = 1;

    public static short NoLength = 0;

} 