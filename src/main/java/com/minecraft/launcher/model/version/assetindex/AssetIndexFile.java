package com.minecraft.launcher.model.version.assetindex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.Map;

/**
 * 资源索引文件本体（assetIndex.url 指向的 JSON）：objects 以虚拟路径为键、内容寻址对象为值。
 * 旧版索引带 virtual 标记（需额外镜像到 assets/virtual/&lt;id&gt;/），新版为 null。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class AssetIndexFile {

    private final Map<String, AssetObject> objects;
    private final Boolean virtual;

    public AssetIndexFile(@JsonProperty("objects") Map<String, AssetObject> objects,
                          @JsonProperty("virtual") Boolean virtual) {
        this.objects = objects;
        this.virtual = virtual;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    public static class AssetObject {
        private final String hash;
        private final Integer size;

        public AssetObject(@JsonProperty("hash") String hash,
                           @JsonProperty("size") Integer size) {
            this.hash = hash;
            this.size = size;
        }

        /** 内容寻址相对路径：&lt;hash 前2位&gt;/&lt;hash&gt;。 */
        public String location() {
            return hash.substring(0, 2) + "/" + hash;
        }
    }
}
