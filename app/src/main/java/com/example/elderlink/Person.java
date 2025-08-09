package com.example.elderlink;

public class Person {
    private String name;
    private String imageBase64;

    public Person() {

    }

    public Person(String name, String imageBase64) {
        this.name = name;
        this.imageBase64 = imageBase64;
    }

    public String getName() {
        return name;
    }

    public String getImageBase64() {
        return imageBase64;
    }
}
