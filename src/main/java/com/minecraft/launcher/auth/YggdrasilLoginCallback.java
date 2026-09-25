package com.minecraft.launcher.auth;

/** 第三方 Yggdrasil 登录回调（后台线程触发，UI 侧负责切回主线程）。 */
public interface YggdrasilLoginCallback {

    /** 登录成功；account 内含角色列表，可切换。 */
    void onSuccess(YggdrasilAccount account);

    /** 任一步失败。 */
    void onError(Exception error);
}
