package com.minecraft.launcher.model.users;

import java.util.Scanner;

public class users {
    private String username;

    public void changeUsername(){

        Scanner scanner = new Scanner(System.in);

        System.out.print("\n请输入玩家名字 (默认 Steve): ");
        scanner.nextLine(); // 吃掉换行符
        this.username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "Steve";
        }
    }

    public void showUsername(){
        System.out.print(this.username);
    }

    public String getUsername(){
        return this.username;
    }

}
