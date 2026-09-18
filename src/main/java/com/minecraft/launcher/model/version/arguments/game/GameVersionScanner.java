package com.minecraft.launcher.model.version.arguments.game;


import java.io.File;
import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class GameVersionScanner {

    private final Scanner scanner = new Scanner(System.in);
    private final String gameDir;
    //给入游戏路径扫描
    public GameVersionScanner(String gameDir){
        this.gameDir = Objects.requireNonNull(gameDir, "gameDir");
    }

    //获取本地已安装的所有mc版本
    public List<String> getInstalledVersions() {
        List<String> versions = new ArrayList<>();
        File versionsFolder = new File(this.gameDir, "versions");

        if (versionsFolder.exists() && versionsFolder.isDirectory()) {
            File[] files = versionsFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    // 确保是文件夹，且文件夹内存在对应的 json 配置文件
                    if (file.isDirectory()) {
                        File jsonFile = new File(file, file.getName() + ".json");
                        if (jsonFile.isFile()) {
                            versions.add(file.getName());
                        }
                    }
                }
            }
        }
        return versions;
    }

    public Optional<String> GameVersionSelecter() {
        List<String> versions = getInstalledVersions();
        if (versions.isEmpty()) {
            System.err.println("未在 " + gameDir + "\\versions 下找到任何有效版本！");
            return Optional.empty();
        }

        System.out.println("\n[检测到的游戏版本]:");
        for (int i = 0; i < versions.size(); i++) {
            System.out.println((i + 1) + ". " + versions.get(i));
        }

        while (true) {
            System.out.print("请选择要启动的版本编号 (1-" + versions.size() + "): ");

            //nt versionChoice = scanner.nextInt() - 1;
            if (!scanner.hasNextInt()) {
                scanner.nextLine(); // 吃掉非法 token，否则死循环
                System.out.println("请输入数字。");
                continue;
            }
            int choice = scanner.nextInt();
            scanner.nextLine(); // 吃掉换行
            if (choice < 1 || choice > versions.size()) {
                System.out.println("编号超出范围，请重新输入。");
                continue;
            }
            return Optional.of(versions.get(choice - 1));
            //return versions.get(Math.max(0, Math.min(versionChoice, versions.size() - 1)));
        }
    }
    public Optional<String> GameVersionGet() {
        return GameVersionSelecter();
    }
}
