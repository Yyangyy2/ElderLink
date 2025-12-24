package com.example.elderlink.view_medication_v2;

public class Model_medication_v2 {
    private String id;
    private String brandname;
    private String medname;
    private String timesperday;
    private String imageBase64; // Add this field for image

    // Empty constructor required for Firestore
    public Model_medication_v2() {
    }

    // Constructor with all fields
    public Model_medication_v2(String id, String brandname, String medname, String timesperday, String imageBase64) {
        this.id = id;
        this.brandname = brandname;
        this.medname = medname;
        this.timesperday = timesperday;
        this.imageBase64 = imageBase64;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBrandname() {
        return brandname;
    }

    public void setBrandname(String brandname) {
        this.brandname = brandname;
    }

    public String getMedname() {
        return medname;
    }

    public void setMedname(String medname) {
        this.medname = medname;
    }

    public String getTimesperday() {
        return timesperday;
    }

    public void setTimesperday(String timesperday) {
        this.timesperday = timesperday;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}