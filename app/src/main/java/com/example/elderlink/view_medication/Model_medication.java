package com.example.elderlink.view_medication;

public class Model_medication {
    private String id;
    private String name;
    private String date;
    private String time;
    private String unit;

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String imageBase64;


    public Model_medication() {

    }

    public Model_medication(String id, String name, String date, String time, String unit, String imageBase64) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.time = time;
        this.unit = unit;
        this.imageBase64 = imageBase64;
    }


}
