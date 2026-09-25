package com.minecraft.launcher.auth;

/** 微软设备码登录回调（后台线程触发，UI 侧负责切回主线程）。 */
public interface MicrosoftLoginCallback {

    /** 设备码就绪：展示 userCode 与 verificationUri，等待用户在浏览器授权。 */
    void onDeviceCode(String userCode, String verificationUri);

    /** 全链完成。 */
    void onSuccess(Account account);

    /** 任一步失败。 */
    void onError(Exception error);
}
