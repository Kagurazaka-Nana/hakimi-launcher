package com.minecraft.launcher.model.users;

import lombok.Getter;

import java.util.Scanner;

@Getter
public class Users {
    private String username;

    public void changeUsername(){

        Scanner scanner = new Scanner(System.in);

        System.out.print("\n请输入玩家名字 (默认 Steve): ");
        this.username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Steve";
        }
    }

}
