package com.trekmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrekMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrekMateApplication.class, args);
        System.out.println("==================================================================");
        System.out.println("   TrekMate Application Started Successfully!                     ");
        System.out.println("   Open your browser and navigate to: http://localhost:8080       ");
        System.out.println("   Trekker demo login: trekker@example.com / pass123              ");
        System.out.println("   Host demo login:    host@example.com / admin123                ");
        System.out.println("==================================================================");
    }
}

