//This file controls the display of person_item and person_item_loginelder
//Due to having both Elder interface and Caregiver sharing same Person model and PersonAdapter, that is why have 2 views (of the code) sharing here
package com.example.elderlink;

import static com.example.elderlink.HashPIN.hashPin;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {

    private Context context;
    private List<Person> personList;
    private boolean isLoginElder; // true = elder view, false = caregiver view
    private String uid; //caregiver's uid, should be caregiverUid but idk why I wrote uid previously


    public PersonAdapter(Context context, List<Person> personList, boolean isLoginElder, String uid) {
        this.context = context;
        this.personList = personList;
        this.isLoginElder = isLoginElder;
        this.uid = uid; //caregiver's uid
    }

    @Override
    public int getItemViewType(int position) {
        return isLoginElder ? 1 : 0; // 1 = elder layout, 0 = caregiver layout
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == 1) ? R.layout.person_item_loginelder : R.layout.person_item;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Person person = personList.get(position);
        holder.personName.setText(person.getName());
        String personUid = person.getId();
        String caregiverUid = uid;


        // Decode Base64 image if available, otherwise use profile_placeholder from drawable
        if (person.getImageBase64() != null && !person.getImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(person.getImageBase64(), Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.personImage.setImageBitmap(decodedBitmap);
            } catch (IllegalArgumentException e) {
                // If decoding fails, use placeholder instead of crashing
                holder.personImage.setImageResource(R.drawable.profile_placeholder);
            }
        } else {
            holder.personImage.setImageResource(R.drawable.profile_placeholder); // fallback image
        }

        if (!isLoginElder) {
            // Caregiver mode contains= check button + 3-dot menu button
            holder.checkBtn.setVisibility(View.VISIBLE);
            holder.moreBtn.setVisibility(View.VISIBLE);

            holder.checkBtn.setOnClickListener(v -> {
                Toast.makeText(context, "Checked " + person.getName(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, CheckOnElderlyActivity.class);

                // Pass data so the new page knows which person it is
                intent.putExtra("personName", person.getName());
                //intent.putExtra("personImageBase64", person.getImageBase64());
                intent.putExtra("personUid", personUid);
                intent.putExtra("caregiverUid",caregiverUid);

                context.startActivity(intent);
            });

            holder.moreBtn.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), holder.moreBtn);
                popup.inflate(R.menu.person_menu);
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.edit) {
                        editNamePerson(person, holder.getAdapterPosition());
                        return true;
                    } else if (id == R.id.delete) {
                        deletePerson(person, holder.getAdapterPosition());
                        return true;
                    } else if (id == R.id.share) {
                        sharePerson(person, holder.getAdapterPosition());
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            // Elder mode = hide caregiver-only buttons
            if (holder.checkBtn != null) holder.checkBtn.setVisibility(View.GONE);
            if (holder.moreBtn != null) holder.moreBtn.setVisibility(View.GONE);




            //--------------PIN Section------------------[within Elder mode]---------------------------------------------------------
            holder.itemView.setOnClickListener(v -> {
                Dialog dialog = new Dialog(context);
                dialog.setContentView(R.layout.activity_login_elder_pin);
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                EditText pinInput = dialog.findViewById(R.id.pinEditText);
                Button confirmBtn = dialog.findViewById(R.id.confirmPinButton);
                Button cancelBtn = dialog.findViewById(R.id.closePopupButton);

                confirmBtn.setOnClickListener(view -> {
                    String enteredPin = pinInput.getText().toString().trim();
                    if (enteredPin.length() != 6) {
                        Toast.makeText(context, "Enter a 6-digit PIN", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //Hash entered PIN
                    String enteredHash = hashPin(enteredPin);

                    //Fetch stored hashed PIN from Firestore
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .collection("people")
                            .document(person.getId())
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String storedHash = doc.getString("pin"); // hashed pin in Firestore
                                    if (storedHash != null && storedHash.equals(enteredHash)) {
                                        Toast.makeText(context, "PIN correct for " + person.getName(), Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();

                                        Intent intent = new Intent(context, MainActivityElder.class);

                                        // Pass data ( elder name,elder uid, caregiver uid)
                                        intent.putExtra("personName", person.getName());
                                        intent.putExtra("personUid", person.getId());
                                        //intent.putExtra("personImageBase64", person.getImageBase64());
                                        intent.putExtra("caregiverUid", uid);

                                        context.startActivity(intent);

                                        if (context instanceof Activity) {
                                            ((Activity) context).finish(); //close current screen
                                        }


                                    } else {
                                        Toast.makeText(context, "Wrong PIN!", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(context, "Person not found!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                });

                cancelBtn.setOnClickListener(view -> dialog.dismiss());
                dialog.show();
            });





        }
    }

    //Edit Person name----------------------------------------------------------------------------------------------------------------------------------
    private void editNamePerson(Person person, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Person");

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_person, null);
        EditText nameInput = dialogView.findViewById(R.id.editPersonName);

        nameInput.setText(person.getName());
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            if (!newName.isEmpty()) {
                person.setName(newName);

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .collection("people")
                        .document(person.getId())
                        .set(person)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                personList.set(position, person);
                                notifyItemChanged(position);
                                Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }




    //Delete Person----------------------------------------------------------------------------------------------------------------------------------
    private void deletePerson(Person person, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("people")
                .document(person.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    int index = personList.indexOf(person);
                    if (index != -1) {
                        personList.remove(position);
                        notifyItemRemoved(position);
                    }
                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                });
    }

    //Share Person----------------------------------------------------------------------------------------------------------------------------------
    private void sharePerson(Person person, int position) {
        final String personUid = person.getId();    //Use final means This person’s ID is fixed/lock it/unchangeable, don’t let it change while the code runs|| The code inside .addOnSuccessListener() actually runs later, not immediately.
        //So, between now and that “later,” personUid could be changed by other parts of your code if it’s not locked (final).

        // find which user (caregiver) is currently sharing the person, below here is like going through customs, check logs------------------------
        String temporaryUid = this.uid;
        if (temporaryUid == null || temporaryUid.isEmpty()) {
            temporaryUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
        }

        if (temporaryUid == null) {
            Toast.makeText(context, "Error: caregiver UID not found. Please re-login.", Toast.LENGTH_SHORT).show();
            Log.e("SharePerson", "Caregiver UID is null — cannot share person.");
            return;
        }
        //I can't write "final String caregiverUid = uid" because the uid is a global variable, it may change by other code/methods.
        //Another reason is bcz listeners requires to have final
        final String caregiverUid = temporaryUid;       // lock it as final, to be used inside Firebase listeners (.addOnSuccessListener())
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create share dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share " + person.getName());
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_share_person, null);
        EditText emailInput = dialogView.findViewById(R.id.shareEmailInput);
        Button sendBtn = dialogView.findViewById(R.id.btnSendShare);
        Button cancelBtn = dialogView.findViewById(R.id.btnCancelShare);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        sendBtn.setOnClickListener(v -> {
            String targetEmail = emailInput.getText().toString().trim();
            if (targetEmail.isEmpty()) {
                Toast.makeText(context, "Enter an email", Toast.LENGTH_SHORT).show();
                return;
            }

            // STEP 1: Find target user UID
            db.collection("users")
                    .whereEqualTo("email", targetEmail)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (query.isEmpty()) {
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String targetUserUid = query.getDocuments().get(0).getId();

                        // STEP 2: Read the source person's doc
                        db.collection("users")
                                .document(caregiverUid)
                                .collection("people")
                                .document(personUid)
                                .get()
                                .addOnSuccessListener(personDoc -> {
                                    if (!personDoc.exists()) {
                                        Toast.makeText(context, "Source person not found", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        return;
                                    }

                                    // STEP 3: Write person doc to target user based on email found/inputted (copy and paste)
                                    db.collection("users")
                                            .document(targetUserUid)
                                            .collection("people")
                                            .document(personUid)
                                            .set(personDoc.getData())
                                            .addOnSuccessListener(aVoid -> {

                                                // STEP 4: Copy all subcollections under this person----------!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                                                copyAllSubcollections(db, caregiverUid, targetUserUid, personUid, dialog);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(context, "Failed to share person: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Error reading person: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Error finding user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });
    }


    // Copy all subcollections (like medications) from one person (elder) to another user’s account (caregiver).

    private void copyAllSubcollections(FirebaseFirestore db, String fromUid, String toUid, String personUid, AlertDialog dialog) {
        db.collection("users")
                .document(fromUid)
                .collection("people")
                .document(personUid)
                .collection("medications")
                .get()
                .addOnSuccessListener(subQuery -> {
                    if (subQuery.isEmpty()) {
                        dialog.dismiss();
                        showSuccessPopup();
                        return;
                    }

                    WriteBatch batch = db.batch();  // Batch write, a special Firestore tool that lets you group multiple write operations into a single request.
                    // This is more efficient and ensures all writes succeed or fail together.
                    for (QueryDocumentSnapshot doc : subQuery) {     // for loop every medication document found under this person
                        DocumentReference targetDoc = db.collection("users")
                                .document(toUid)
                                .collection("people")
                                .document(personUid)
                                .collection("medications")
                                .document(doc.getId());
                        batch.set(targetDoc, doc.getData());   // set the data of each medication document to the new location
                    }

                    batch.commit()    //commit = executes
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                showSuccessPopup();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed copying subcollections: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error reading subcollections: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
    }



    // Success popup with green check
    private void showSuccessPopup() {
        Dialog successDialog = new Dialog(context);
        successDialog.setContentView(R.layout.dialog_share_success);
        successDialog.getWindow().setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        ImageView checkIcon = successDialog.findViewById(R.id.successCheckIcon);
        Button okBtn = successDialog.findViewById(R.id.btnOk);

        okBtn.setOnClickListener(v -> successDialog.dismiss());
        successDialog.show();
    }
















    @Override
    public int getItemCount() {
        return personList.size();
    }

    // ViewHolder
    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        ImageView personImage;
        TextView personName;
        Button checkBtn;
        ImageButton moreBtn;

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            personImage = itemView.findViewById(R.id.personImage);
            personName = itemView.findViewById(R.id.personName);
            checkBtn = itemView.findViewById(R.id.checkBtn); // may not exist in elder layout
            moreBtn = itemView.findViewById(R.id.moreBtn);   // may not exist in elder layout
        }
    }
}