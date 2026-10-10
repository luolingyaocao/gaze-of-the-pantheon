package com.onceheart.gazeofthepantheon.client;

/**
 * 客户端「是否被神行压制」状态。
 *
 * 由网络包 SwiftShieldPacket 写入，被两个客户端 mixin 读取。
 * 纯本地静态字段，不持久化。
 */
public class StopClientState {

    private static volatile boolean suppressed = false;

    public static boolean isSuppressed() {
        return suppressed;
    }

    public static void setSuppressed(boolean value) {
        suppressed = value;
    }
}