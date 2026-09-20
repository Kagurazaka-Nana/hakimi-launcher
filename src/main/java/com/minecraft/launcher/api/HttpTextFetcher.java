package com.minecraft.launcher.api;

import java.util.Map;

/** 只读 HTTP 文本获取抽象，便于 Provider 注入假实现做离线测试。 */
public interface HttpTextFetcher {

    /** GET 指定 URL，返回 UTF-8 响应体。 */
    String get(String url, Map<String, String> headers);
}
