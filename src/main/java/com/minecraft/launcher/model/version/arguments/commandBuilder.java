package com.minecraft.launcher.model.version.arguments;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import com.minecraft.launcher.model.users.users;
import com.minecraft.launcher.model.version.arguments.game.GameVersionScanner;
import com.minecraft.launcher.model.version.arguments.game.MinecraftFinder;
import lombok.Getter;

public class commandBuilder {

    // ObjectMapper 线程安全，可以静态复用
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String buildClasspath(String gameDir, String version) {
        List<String> jarPaths = new ArrayList<>();
        List<String> missingLibs = new ArrayList<>();

        // 1. 定位版本 JSON
        File jsonFile = new File(gameDir, "versions/" + version + "/" + version + ".json");
        if (!jsonFile.exists()) {
            throw new RuntimeException("版本配置文件不存在: " + jsonFile.getAbsolutePath());
        }

        File libDir = new File(gameDir, "libraries");

        try {
            // Jackson 直接读文件，不需要手动 FileReader
            JsonNode root = MAPPER.readTree(jsonFile);
            JsonNode libraries = root.get("libraries");

            if (libraries != null && libraries.isArray()) {
                for (JsonNode libObj : libraries) {

                    // 规则过滤：只加载适用于当前操作系统的库
                    if (libObj.has("rules")) {
                        JsonNode rules = libObj.get("rules");
                        if (rules != null && rules.isArray() && !shouldAllow(rules)) {
                            continue;
                        }
                    }

                    // 跳过 native 库
                    if (libObj.has("natives")) {
                        continue;
                    }

                    // 处理库坐标
                    if (libObj.has("name")) {
                        String name = libObj.get("name").asText();
                        String relPath = nameToPath(name);
                        File jarFile = new File(libDir, relPath);

                        if (jarFile.exists()) {
                            jarPaths.add(jarFile.getAbsolutePath());
                        } else {
                            missingLibs.add(jarFile.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取版本 JSON 失败: " + jsonFile.getAbsolutePath(), e);
        }

        // 2. 添加主版本 jar，放在最前面
        File versionJar = new File(gameDir, "versions/" + version + "/" + version + ".jar");
        if (versionJar.exists()) {
            jarPaths.add(0, versionJar.getAbsolutePath());
        } else {
            throw new RuntimeException("核心 Jar 包不存在: " + versionJar.getAbsolutePath());
        }

        // 3. 检查缺失依赖
        if (!missingLibs.isEmpty()) {
            throw new RuntimeException(
                    "以下依赖库缺失，请检查 .minecraft/libraries 目录：\n"
                            + String.join("\n", missingLibs)
            );
        }

        // 4. 使用系统标准路径分隔符，比手动判断 os.name 更稳
        return String.join(File.pathSeparator, jarPaths);
    }

    /**
     * 将 Maven 坐标转换为本地文件路径（支持 classifier）。
     * 示例：com.google.guava:guava:32.1.2-jre → com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar
     *       org.lwjgl:lwjgl:3.3.1:natives-windows → .../lwjgl-3.3.1-natives-windows.jar
     */
    private static String nameToPath(String name) {
        String[] parts = name.split(":");
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = (parts.length > 3) ? "-" + parts[3] : "";
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
    }

    /**
     * 判断当前操作系统是否允许加载该库。
     * 规则逻辑：无 rules → 允许；有 rules → 默认禁止，按顺序匹配，后匹配覆盖先前结果。
     */
    private static boolean shouldAllow(JsonNode rules) {
        // 无 rules、不是数组、空数组 → 允许
        if (rules == null || !rules.isArray() || rules.size() == 0) {
            return true;
        }

        boolean allowed = false; // 有 rules 时默认禁止
        String currentOs = System.getProperty("os.name").toLowerCase();

        for (JsonNode rule : rules) {
            String action = rule.path("action").asText();
            boolean matches = true;

            if (rule.has("os")) {
                JsonNode os = rule.get("os");
                String osName = os.path("name").asText();
                if (osName.equals("windows") && !currentOs.contains("win")) {
                    matches = false;
                } else if (osName.equals("linux") && !currentOs.contains("linux")) {
                    matches = false;
                } else if (osName.equals("osx") && !currentOs.contains("mac")) {
                    matches = false;
                }
                // 可扩展其他 OS
            }

            // 若规则匹配当前环境，则根据 action 设置允许状态（后匹配覆盖）
            if (matches) {
                allowed = "allow".equals(action);
            }
        }
        return allowed;
    }

    /**
     * 从版本 JSON 中读取 assetIndex 的 id。
     *
     * @return 资源索引 ID（如 "17"）
     * @throws RuntimeException 如果 JSON 解析失败或缺少 assetIndex 字段
     */
    public static String getAssetIndex(String gameDir, String version) {
        File jsonFile = new File(gameDir, "versions/" + version + "/" + version + ".json");

        try {
            JsonNode root = MAPPER.readTree(jsonFile);

            JsonNode assetIndex = root.get("assetIndex");
            if (assetIndex == null || assetIndex.isNull()) {
                throw new RuntimeException("版本 JSON 中缺少 assetIndex 字段");
            }

            JsonNode id = assetIndex.get("id");
            if (id == null || id.isNull()) {
                throw new RuntimeException("assetIndex 中缺少 id 字段");
            }

            return id.asText();
        } catch (IOException e) {
            throw new RuntimeException("读取 assetIndex 失败: " + jsonFile.getAbsolutePath(), e);
        }
    }

    private final List<String> argFileLines = new ArrayList<String>();

    MinecraftFinder minecraftFinder = new MinecraftFinder();
    @Getter
    private final String gameDir = minecraftFinder.GamePathSelecter();
    GameVersionScanner GameVersionScanner = new GameVersionScanner(gameDir);
    @Getter
    private final String version = GameVersionScanner.GameVersionGet();
    private final String classPath = buildClasspath(gameDir, version);
    private final String assetIndex = getAssetIndex(gameDir, version);
    private final String nativesPath = gameDir + "/versions/" + version + "/" + version + "-natives";
    @Getter
    private final File argFile = new File(gameDir, "command_args.txt");

    public void commandConnect() {
        try {
            System.out.println("\nCommand Connecting...");
            users user = new users();
            user.changeUsername();//要求修改用户名

            // 如果 natives 目录不存在，简单提示
            File nativesDir = new File(nativesPath);
            if (!nativesDir.exists()) nativesDir.mkdirs();

            argFileLines.add("-Xms1024M");
            argFileLines.add("-Xmx2048M");
            argFileLines.add("-Djava.library.path=" + nativesPath);
            argFileLines.add("-cp");
            argFileLines.add(classPath);
            argFileLines.add("net.minecraft.client.main.Main");
            argFileLines.add("--username");
            argFileLines.add(user.getUsername());
            argFileLines.add("--version");
            argFileLines.add(version);
            argFileLines.add("--gameDir");
            argFileLines.add(gameDir);
            argFileLines.add("--assetsDir");
            argFileLines.add(new File(gameDir, "assets").getAbsolutePath());
            argFileLines.add("--assetIndex");
            argFileLines.add(assetIndex);
            argFileLines.add("--uuid");
            argFileLines.add("00000000-0000-0000-0000-000000000000");
            argFileLines.add("--accessToken");
            argFileLines.add("0");
            argFileLines.add("--userType");
            argFileLines.add("legacy");

            Files.write(argFile.toPath(), argFileLines, StandardCharsets.UTF_8);

        } catch (Exception e) {
            System.err.println("Connect false: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public List<String> Getcommand(){
        commandConnect();
        return argFileLines;
    }

}
