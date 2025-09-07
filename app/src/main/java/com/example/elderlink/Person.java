package com.example.elderlink;

public class Person {
    private String id;
    private String name;
    private String imageBase64;
    private String pin;

    public Person() {

    }

    public Person(String name, String imageBase64,String pin) {
        this.name = name;
        this.imageBase64 = imageBase64;
        this.pin = pin;

    }



    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public String getPin() { return pin; }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

}


