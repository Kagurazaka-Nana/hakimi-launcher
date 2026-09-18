package com.minecraft.launcher.model.version.arguments.game;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.Scanner;

public class MinecraftFinder {

    private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
            "windows", "winsxs", "$recycle.bin", "system volume information",
            "recovery", "perflogs", "node_modules", ".git", ".gradle", ".m2",
            "temp", "tmp", "cache", ".cache",
            "proc", "sys", "dev", "run", "boot", "lost+found"
    ));

    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String HOME = System.getProperty("user.home");

    /** 返回 List<String>，元素是绝对路径字符串 */
    public static List<String> getKnownCandidates() {
        LinkedHashSet<String> set = new LinkedHashSet<>();

        if (OS.contains("win")) {
            String roaming = System.getenv("APPDATA");
            String local   = System.getenv("LOCALAPPDATA");
            if (roaming != null) {
                set.add(Paths.get(roaming, ".minecraft").toAbsolutePath().toString());
            }
            if (local != null) {
                set.add(Paths.get(local, ".minecraft").toAbsolutePath().toString());
                set.add(Paths.get(local, "Packages",
                        "Microsoft.4297127D64EC6_8wekyb3d8bbwe",
                        "LocalCache", "Local", ".minecraft").toAbsolutePath().toString());
            }
        } else if (OS.contains("mac")) {
            set.add(Paths.get(HOME, "Library", "Application Support", "minecraft")
                    .toAbsolutePath().toString());
        } else {
            set.add(Paths.get(HOME, ".minecraft").toAbsolutePath().toString());
            set.add(Paths.get(HOME, ".var", "app", "com.mojang.Minecraft", ".minecraft")
                    .toAbsolutePath().toString());
            set.add(Paths.get(HOME, "snap", "minecraft", "current", ".minecraft")
                    .toAbsolutePath().toString());
            set.add(Paths.get(HOME, ".local", "share", "minecraft")
                    .toAbsolutePath().toString());
        }
        return new ArrayList<>(set);
    }

    /** 参数改为 String，内部转 Path 判断 */
    public static boolean isValidMinecraftDir(String dirPath) {
        if (dirPath == null || dirPath.isBlank()) return false;
        return isValidMinecraftDir(Path.of(dirPath));
    }

    /** 保留 Path 重载，供内部复用 */
    public static boolean isValidMinecraftDir(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return false;

        Path fileName = dir.getFileName();
        String name = fileName == null ? "" : fileName.toString().toLowerCase();
        if (!(name.equals(".minecraft") || name.equals("minecraft")
                || name.equals("minecraft launcher"))) {
            return false;
        }

        boolean hasVersions = Files.isDirectory(dir.resolve("versions"));
        boolean hasProfile  = Files.isRegularFile(dir.resolve("launcher_profiles.json"))
                || Files.isRegularFile(dir.resolve("launcher_profiles_microsoft_store.json"));
        return hasVersions || hasProfile;
    }

    /**
     * 全盘扫描，返回 List<String>
     * @param onFound  每找到一个回调一次，参数是路径字符串
     */
    public static List<String> scanAll(Consumer<String> onFound,
                                       AtomicBoolean cancelled,
                                       Consumer<Long> onTick) {
        List<String> result = Collections.synchronizedList(new ArrayList<>());
        Set<String> seen = ConcurrentHashMap.newKeySet();
        AtomicLong counter = new AtomicLong();

        Consumer<Path> dedup = p -> {
            String s = p.toAbsolutePath().normalize().toString();
            if (seen.add(s)) {
                result.add(s);
                if (onFound != null) onFound.accept(s);
            }
        };

        File[] roots = File.listRoots();
        if (roots == null) return result;

        for (File root : roots) {
            if (cancelled.get()) break;
            if (!root.exists() || root.getTotalSpace() <= 0) continue;
            try {
                walk(root.toPath(), dedup, cancelled, counter, onTick);
            } catch (Exception ignored) {}
        }
        return result;
    }

    // 内部仍然用 Path 递归
    private static void walk(Path root, Consumer<Path> onFound,
                             AtomicBoolean cancelled, AtomicLong counter,
                             Consumer<Long> onTick) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (cancelled.get()) return FileVisitResult.TERMINATE;

                Path fileName = dir.getFileName();
                String name = fileName == null ? "" : fileName.toString().toLowerCase();

                if (SKIP_DIRS.contains(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (name.equals(".minecraft") || name.equals("minecraft")) {
                    if (isValidMinecraftDir(dir)) {
                        onFound.accept(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                long c = counter.incrementAndGet();
                if (onTick != null && (c & 0x3FF) == 0) onTick.accept(c);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Optional<String> GamePathSelecter(){
        Scanner scanner = new Scanner(System.in);
        List<String> Path = scanAll(null,new AtomicBoolean(false),null);
        if (Path.isEmpty()) {
            System.err.println("未在找到任何游戏路径。");
            return Optional.empty();
        }else{
            System.out.println("\n[检测到的游戏路径]:");
            for (int i = 0; i < Path.size(); i++) {
                System.out.println((i + 1) + ". " + Path.get(i));
            }
            while (true) {
                System.out.print("请选择要启动的版本编号 (1-" + Path.size() + "): ");
                //int versionChoice = scanner.nextInt() - 1;
                if (!scanner.hasNextInt()) {
                    scanner.nextLine(); // 吃掉非法 token，否则死循环
                    System.out.println("请输入数字。");
                    continue;
                }
                int choice = scanner.nextInt();
                scanner.nextLine(); // 吃掉换行
                if (choice < 1 || choice > Path.size()) {
                    System.out.println("编号超出范围，请重新输入。");
                    continue;
                }
                return Optional.of(Path.get(choice - 1));
                //return Path.get(Math.max(0, Math.min(versionChoice, Path.size() - 1)));
            }
        }

    }
}
