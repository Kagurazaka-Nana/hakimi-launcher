package com.minecraft.launcher.model.version.javaversion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class JavaFander {

    // 递归查找 javaw.exe 辅助函数（限制深度为 4，防止全盘扫描变卡）
    private static void searchJavaw(File dir, List<String> javaPaths, int depth) {
        if (depth > 4) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    searchJavaw(f, javaPaths, depth + 1);
                } else if (f.getName().equalsIgnoreCase("javaw.exe")) {
                    String absPath = f.getAbsolutePath();
                    if (!javaPaths.contains(absPath)) {
                        javaPaths.add(absPath);
                    }
                }
            }
        }
    }

    // 2. 自动探测系统中的 javaw.exe 路径列表
    public static List<String> detectJavaPaths() {
        List<String> javaPaths = new ArrayList<>();

        // 检查 JAVA_HOME 环境变量
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            File javaw = new File(javaHome, "bin/javaw.exe");
            if (javaw.exists()) javaPaths.add(javaw.getAbsolutePath());
        }

        // 检查 PATH 环境变量
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            for (String p : paths) {
                File javaw = new File(p, "javaw.exe");
                if (javaw.exists() && !javaPaths.contains(javaw.getAbsolutePath())) {
                    javaPaths.add(javaw.getAbsolutePath());
                }
            }
        }

        // 检查常见的 Java 默认安装路径 (Windows)
        String[] commonDirs = {
                "C:\\Program Files\\Java",
                "C:\\Program Files (x86)\\Java",
                System.getProperty("user.home") + "\\AppData\\Local\\Packages\\Microsoft.4297127D64374_8wekyb3d8bbwe\\LocalCache\\Local\\runtime" // 官方启动器自带Runtime
        };

        for (String dirPath : commonDirs) {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                searchJavaw(dir, javaPaths, 0);
            }
        }

        return javaPaths;
    }

    public String JavaVersionSelector(){

        Scanner scanner = new Scanner(System.in);
        List<String> javaPaths = detectJavaPaths();
        String javaPath;

        if (javaPaths.isEmpty()) {
            System.out.print("\n未自动检测到 Java，请手动输入 javaw.exe 的完整路径: ");
            scanner.nextLine(); // 吃掉换行符
            return scanner.nextLine().trim();
        } else {
            System.out.println("\n[检测到的 Java 运行环境]:");
            for (int i = 0; i < javaPaths.size(); i++) {
                System.out.println((i + 1) + ". " + javaPaths.get(i));
            }
            System.out.print("请选择 Java 路径编号 (1-" + javaPaths.size() + "): ");
            int javaChoice = scanner.nextInt() - 1;
            return javaPaths.get(Math.max(0, Math.min(javaChoice, javaPaths.size() - 1)));
        }
    }

}
