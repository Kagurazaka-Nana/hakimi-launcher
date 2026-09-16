package com.minecraft.launcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.minecraft.launcher.model.version.javaversion.JavaFander;
import com.minecraft.launcher.model.version.arguments.commandBuilder;
public class GameStart {

    public void gameStart(){
        try{
            commandBuilder commandBuilder = new commandBuilder();
            JavaFander javaFander = new JavaFander();
            List<String> command = new ArrayList<>();
            String javaPath = javaFander.JavaVersionSelector();
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
        }catch (Exception e) {
            System.err.println("启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
