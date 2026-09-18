package com.minecraft.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minecraft.launcher.model.version.javaversion.JavaFinder;
import com.minecraft.launcher.model.version.arguments.CommandBuilder;

public class GameStart {

    private static final Logger logger = LoggerFactory.getLogger(GameStart.class.getName());
    public void gameStart(){
        try{
            CommandBuilder commandBuilder = new CommandBuilder();
            JavaFinder javaFinder = new JavaFinder();
            List<String> command = new ArrayList<>();
            String javaPath = javaFinder.JavaVersionSelector().orElseThrow(() -> new RuntimeException("未选择 Java 路径"));
            command.add(javaPath);
            command.addAll(commandBuilder.Getcommand());
            command.add("@\"" + commandBuilder.getArgFile().getAbsolutePath() + "\"");

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(commandBuilder.getGameDir()));
            processBuilder.inheritIO();
            processBuilder.redirectErrorStream(true);

            System.out.println("正在拉起 Minecraft 进程...");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            System.out.println("游戏已关闭，退出码: " + exitCode);
        } catch (InterruptedException e) {
            logger.error("进程被中断: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }catch (Exception e) {
            logger.error("Failed to start the game.", e);
        }
    }

}
