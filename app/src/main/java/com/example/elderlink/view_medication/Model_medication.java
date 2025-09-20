package com.example.elderlink.view_medication;


public class Model_medication {
    private String id;
    private String name;
    private String date;
    private String endDate;
    private String time;
    private String dosage;
    private String unit;
    private String imageBase64;
    private String repeatType;
    private Boolean switchReminder;

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getDosage() {
        return dosage;
    }
    public void setDosage(String dosage) {
        this.dosage = dosage;
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

    public String getEndDate() {
        return endDate;
    }
    public void setEndDate(String endDate) {
        this.endDate = endDate;
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

    public String getRepeatType() {return repeatType;}
    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public Boolean getSwitchReminder() { return switchReminder;}

    public void setSwitchReminder(Boolean switchReminder) { this.switchReminder = switchReminder; }




    public Model_medication() {

    }

    public Model_medication(String id, String name, String date, String endDate, String time, String dosage,String unit, String imageBase64, String repeatType, Boolean switchReminder) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.endDate = endDate;
        this.time = time;
        this.dosage = dosage;
        this.unit = unit;
        this.imageBase64 = imageBase64;
        this.repeatType = repeatType;
        this.switchReminder = switchReminder;
    }

    //Method for MedicationActivityElderSide to convert date+time into milliseconds--------------------------------------------------------------------------------------
    public long getTimeMillis() {
        try {
            String dateTime = date + " " + time;
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
            java.util.Date parsedDate = sdf.parse(dateTime);
            return parsedDate != null ? parsedDate.getTime() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }



}
