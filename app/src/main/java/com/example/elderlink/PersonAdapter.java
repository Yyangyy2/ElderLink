// This java file is to tell what to display in the Person RecyclerView.
package com.example.elderlink;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {

    private Context context;
    private List<Person> personList;

    public PersonAdapter(Context context, List<Person> personList) {
        this.context = context;
        this.personList = personList;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.person_item, parent, false);
        return new PersonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Person person = personList.get(position);
        holder.personName.setText(person.getName());

        // Decode Base64 image if available
        if (person.getImageBase64() != null && !person.getImageBase64().isEmpty()) {
            byte[] decodedBytes = Base64.decode(person.getImageBase64(), Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            holder.personImage.setImageBitmap(decodedBitmap);
        } else {
            holder.personImage.setImageResource(R.drawable.profile_placeholder); // fallback image
        }


        holder.checkBtn.setOnClickListener(v ->{
                Toast.makeText(context, "Checked " + person.getName(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, CheckOnElderlyActivity.class);

            // Pass data so the new page knows which person it is
            intent.putExtra("personName", person.getName());
            intent.putExtra("personImageBase64", person.getImageBase64());


            context.startActivity(intent);
        });


        // Handle 3-dot menu
        holder.moreBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), holder.moreBtn);
            popup.inflate(R.menu.person_menu);
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.edit) {
                    showEditDialog(person, holder.getAdapterPosition());
                    return true;
                } else if (id == R.id.delete) {
                    deletePerson(person, holder.getAdapterPosition());
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void showEditDialog(Person person, int position) {
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

    private void deletePerson(Person person, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("people")
                .document(person.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    int index = personList.indexOf(person);
                    if (index != -1) {                //this person the removal of another item, int index of each array
                        personList.remove(position);
                        notifyItemRemoved(position);
                    }
                    Toast.makeText(context, "Deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
                });
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
            checkBtn = itemView.findViewById(R.id.checkBtn);
            moreBtn = itemView.findViewById(R.id.moreBtn);
        }
    }
}
