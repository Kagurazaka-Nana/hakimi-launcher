package com.minecraft.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.launcher.model.manifest.VersionInfo;
import com.minecraft.launcher.model.manifest.VersionManifest;

import java.nio.file.Path;

public class Main {
    private final String VERSIONS_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private final String CUSTOM_USER_AGENT =
            "MinecraftLauncher/1.0 (Java 21)";

    private final Path TEMP_DIR = Path.of("./temp");
    private final Path VERSIONS_MANIFEST_JSON_PATH = TEMP_DIR.resolve("version_manifest.json");

    public static void main(String[] args) throws Exception {
        Main app = new Main();
        HttpDownloader downloader = new HttpDownloader();

        System.out.println("Hello My Hakimi Launcher");
        System.out.println("Current Configurations:");
        System.out.println("VERSIONS_MANIFEST_URL: " + app.VERSIONS_MANIFEST_URL);
        System.out.println("CUSTOM_USER_AGENT: " + app.CUSTOM_USER_AGENT);

        System.out.println("Start download and parse versions_manifest.json");
        // Files.createDirectories(app.VERSIONS_MANIFEST_JSON_PATH.getParent());
        // downloader.downloadFile(app.VERSIONS_MANIFEST_URL, app.VERSIONS_MANIFEST_PATH);
        // System.out.println("Done.");
        ObjectMapper mapper = new ObjectMapper();
        VersionManifest manifest = mapper.readValue(app.VERSIONS_MANIFEST_JSON_PATH.toFile(), VersionManifest.class);

        System.out.println("Start download client.json of the latest release");
        VersionInfo latestRelease = manifest.getLatestRelease()
                .orElseThrow(() -> new IllegalStateException("Manifest 中未找到最新 release 版本信息"));
        Path latestReleaseClientJsonPath = app.TEMP_DIR.resolve(latestRelease.getId() + ".json");
        downloader.downloadFile(latestRelease.getUrl(), latestReleaseClientJsonPath);

        //JsonUtils.printPretty(latestRelease);




        // 现在可以正确获取版本列表

//        System.out.println("最新的发行版: " + manifest.getLatest().getRelease());
//        System.out.println("最新的快照: " + manifest.getLatest().getSnapshot());
//        System.out.println("版本数量: " + manifest.getVersions().size());
//        for (VersionInfo info : manifest.getVersions()) {
//            System.out.println("版本: ");
//            System.out.println("{");
//            System.out.println("id: " + info.getId());
//            System.out.println("type: " + info.getType());
//            System.out.println("url: " + info.getUrl());
//            System.out.println("time: " + info.getTime());
//            System.out.println("releaseTime: " + info.getReleaseTime());
//            System.out.println("sha1: " + info.getSha1());
//            System.out.println("complianceLevel: " + info.getComplianceLevel());
//            System.out.println("}");
//        }
    }
}

