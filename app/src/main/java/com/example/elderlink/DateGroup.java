// Servers as a data model to group medications by date and calculate progress
// This java file servers CheckOnElderActivity and MainActivityElder's DashboardAdapter as a data holder
package com.example.elderlink;

import com.example.elderlink.view_medication.Model_medication;
import java.util.List;

public class DateGroup {
    private String date;
    private List<Model_medication> medications;


    // Group the medications for a specific date using Model_medication
    public DateGroup(String date, List<Model_medication> medications) {
        this.date = date;
        this.medications = medications;
    }

    public String getDate() {
        return date;
    }

    public List<Model_medication> getMedications() {
        return medications;
    }

    // Calculate only per date progress as percentage string
    public String getProgress() {
        if (medications == null || medications.isEmpty()) {
            return "0%";
        }

        int total = medications.size();
        int taken = 0;
        for (Model_medication med : medications) {
            if ("Taken".equalsIgnoreCase(med.getStatus())) {
                taken++;
            }
        }
        int percentage = (int) ((taken * 100.0f) / total);
        return percentage + "%";
    }
}
