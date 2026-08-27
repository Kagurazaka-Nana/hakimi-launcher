package com.minecraft.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minecraft.launcher.model.manifest.VersionInfo;
import com.minecraft.launcher.model.manifest.VersionManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final String VERSIONS_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest.json";
    private static final String CUSTOM_USER_AGENT =
            "MinecraftLauncher/1.0 (Java 21)";

    private static final Path TEMP_DIR =
            Path.of("./temp");
    private static final Path TEST_DIR =
            Path.of("./launcherTest/.minecraft");
    private static final Path ASSETS_DIR =
            TEST_DIR.resolve( "assets");
    private static final Path LIBRARIES_DIR =
            TEST_DIR.resolve( "libraries");
    private static final Path VERSIONS_DIR =
            TEST_DIR.resolve( "versions");

    private static final Path VERSIONS_MANIFEST_JSON_PATH =
            TEMP_DIR.resolve("version_manifest.json");
    private static final Logger logger =
            LoggerFactory.getLogger(Main.class.getName());
    private static final HttpDownloader downloader =
            new HttpDownloader();

    public static void main(String[] args) throws Exception {

        logger.info("Hello My Hakimi Launcher");
        logger.info("Current Configurations:");
        logger.info("VERSIONS_MANIFEST_URL: " + VERSIONS_MANIFEST_URL);
        logger.info("CUSTOM_USER_AGENT: " + CUSTOM_USER_AGENT);

        logger.info("Start download and parse versions_manifest.json");
        Files.createDirectories(VERSIONS_MANIFEST_JSON_PATH.getParent());
        downloader.downloadFile(VERSIONS_MANIFEST_URL, VERSIONS_MANIFEST_JSON_PATH);
        logger.info("Done.");

        ObjectMapper mapper = new ObjectMapper();
        VersionManifest manifest = mapper.readValue(VERSIONS_MANIFEST_JSON_PATH.toFile(), VersionManifest.class);

        logger.info("Start download client.json of the latest release");
        downloadLatestReleaseMeta(manifest);

    }

    private static void downloadLatestReleaseMeta(VersionManifest manifest) throws IOException, InterruptedException {
        VersionInfo latestRelease = manifest.getLatestRelease()
                .orElseThrow(() -> new IllegalStateException("Manifest 中未找到最新 release 版本信息"));

        String id = latestRelease.getId();
        Path latestReleasePath = VERSIONS_DIR.resolve(id);
        Files.createDirectories(latestReleasePath);
        Path latestReleaseClientJsonPath = latestReleasePath.resolve(id + ".json");
        downloader.downloadFile(latestRelease.getUrl(), latestReleaseClientJsonPath);
    }
}

