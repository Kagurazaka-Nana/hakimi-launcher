package com.minecraft.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.launcher.versionmanifest.VersionInfo;
import com.minecraft.launcher.versionmanifest.VersionManifest;

import java.io.File;

public class Main {
    private final String VERSIONS_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private final String CUSTOM_USER_AGENT =
            "MinecraftLauncher/1.0 (Java 21)";
    private final String VERSIONS_MANIFEST_PATH =
            "./temp/version_manifest.json";

    public static void main(String[] args) throws Exception {
        Main app = new Main();
        HttpDownloader downloader = new HttpDownloader();

        System.out.println("Hello My Launcher");
        System.out.println("Current Configurations:");
        System.out.println("VERSIONS_MANIFEST_URL: " + app.VERSIONS_MANIFEST_URL);
        System.out.println("CUSTOM_USER_AGENT: " + app.CUSTOM_USER_AGENT);

        downloader.downloadFile(app.VERSIONS_MANIFEST_URL, app.VERSIONS_MANIFEST_PATH);
        System.out.println("Done.");

        ObjectMapper mapper = new ObjectMapper();
        VersionManifest manifest = mapper.readValue(new File("./temp/version_manifest.json"), VersionManifest.class);

        // 现在可以正确获取版本列表

        System.out.println("最新的发行版: " + manifest.getLatest().getRelease());
        System.out.println("最新的快照: " + manifest.getLatest().getSnapshot());
        System.out.println("版本数量: " + manifest.getVersions().size());
        for (VersionInfo info : manifest.getVersions()) {
            System.out.println("版本: ");
            System.out.println("{");
            System.out.println("id: " + info.getId());
            System.out.println("type: " + info.getType());
            System.out.println("url: " + info.getUrl());
            System.out.println("time: " + info.getTime());
            System.out.println("releaseTime: " + info.getReleaseTime());
            System.out.println("sha1: " + info.getSha1());
            System.out.println("complianceLevel: " + info.getComplianceLevel());
            System.out.println("}");
        }
    }
}