package com.minecraft.launcher.model.version.arguments.game;


import java.io.File;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class GameVersionScanner {

    Scanner scanner = new Scanner(System.in);
    String gameDir;
    //给入游戏路径扫描
    public GameVersionScanner(String gameDir){
        this.gameDir = gameDir;
    }

    //获取本地已安装的所有mc版本
    public List<String> getInstalledVersions(String gameDir) {
        List<String> versions = new ArrayList<>();
        File versionsFolder = new File(gameDir, "versions");

        if (versionsFolder.exists() && versionsFolder.isDirectory()) {
            File[] files = versionsFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    // 确保是文件夹，且文件夹内存在对应的 json 配置文件
                    if (file.isDirectory()) {
                        File jsonFile = new File(file, file.getName() + ".json");
                        if (jsonFile.exists()) {
                            versions.add(file.getName());
                        }
                    }
                }
            }
        }
        return versions;
    }

    public String GameVersionSelecter() {
        List<String> versions = getInstalledVersions(gameDir);
        if (versions.isEmpty()) {
            System.err.println("未在 " + gameDir + "/versions 下找到任何有效版本！");
            return "-1";
        }

        System.out.println("\n[检测到的游戏版本]:");
        for (int i = 0; i < versions.size(); i++) {
            System.out.println((i + 1) + ". " + versions.get(i));
        }
        System.out.print("请选择要启动的版本编号 (1-" + versions.size() + "): ");
        int versionChoice = scanner.nextInt() - 1;
        return versions.get(Math.max(0, Math.min(versionChoice, versions.size() - 1)));
    }

    public String GameVersionGet() {
        return GameVersionSelecter();
    }
}
