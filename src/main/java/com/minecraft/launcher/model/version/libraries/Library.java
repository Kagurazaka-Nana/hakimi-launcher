package com.minecraft.launcher.model.version.libraries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.minecraft.launcher.model.rule.Rule;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class Library {

    private final Download downloads;
    private final String name;
    private final List<Rule> rules;

    /** OS 键（windows/linux/osx）→ classifier 名；值可含 {arch} 占位符。 */
    private final Map<String, String> natives;

    /** 原生库解压排除规则（如 META-INF/）。 */
    private final Extract extract;

    /** 可选 Maven 仓库基址（Forge 等），覆盖默认 libraries.minecraft.net。 */
    private final String url;

    public Library(@JsonProperty("downloads") Download downloads,
                   @JsonProperty("name") String name,
                   @JsonProperty("rules") List<Rule> rules,
                   @JsonProperty("natives") Map<String, String> natives,
                   @JsonProperty("extract") Extract extract,
                   @JsonProperty("url") String url) {
        this.downloads = downloads;
        this.name = name;
        this.rules = rules;
        this.natives = natives;
        this.extract = extract;
        this.url = url;
    }

}
