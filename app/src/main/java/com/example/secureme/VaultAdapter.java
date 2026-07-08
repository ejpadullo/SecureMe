package com.example.secureme;

import android.content.Context;
import android.content.Intent;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import javax.crypto.SecretKey;

public class VaultAdapter extends FirestoreRecyclerAdapter<VaultItem, VaultAdapter.VaultViewHolder> {
    private final Context context;
    public VaultAdapter(@NonNull FirestoreRecyclerOptions<VaultItem> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull VaultViewHolder holder, int position, @NonNull VaultItem model) {
        SecretKey key = AESKeyCache.getKey();
        if (key == null) {
            holder.title.setText("Decryption error");
            holder.username.setText("No key");
            holder.password.setText("No key");
            holder.notes.setVisibility(View.GONE);
            return;
        }

        try {
            String title = AESHelper.decrypt(model.getTitle(), key);
            String username = AESHelper.decrypt(model.getUsername(), key);
            String password = AESHelper.decrypt(model.getPassword(), key);
            String notes = model.getNotes() != null && !model.getNotes().isEmpty()
                    ? AESHelper.decrypt(model.getNotes(), key)
                    : "";

            holder.title.setText(title);

            if (username != null && Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
                holder.username.setText("Email: " + username);
            } else {
                holder.username.setText("Username: " + username);
            }

            holder.password.setText("Password: " + maskPassword(password));

            if (!notes.isEmpty()) {
                holder.notes.setVisibility(View.VISIBLE);
                holder.notes.setText("Notes: " + notes);
            } else {
                holder.notes.setVisibility(View.GONE);
            }

            // Tap to open details
            holder.itemView.setOnClickListener(v -> {
                int realPosition = holder.getBindingAdapterPosition();
                if (realPosition != RecyclerView.NO_POSITION) {
                    DocumentSnapshot snapshot = getSnapshots().getSnapshot(realPosition);
                    Intent intent = new Intent(context, VaultEntryActivity.class);
                    intent.putExtra("documentId", snapshot.getId());
                    intent.putExtra("title", model.getTitle());
                    intent.putExtra("username", model.getUsername());
                    intent.putExtra("password", model.getPassword());
                    intent.putExtra("notes", model.getNotes());
                    context.startActivity(intent);
                }
            });

            // Long press to show decrypted password temporarily
            holder.itemView.setOnLongClickListener(v -> {
                holder.password.setText("Password: " + password);
                return true;
            });

            // Reset mask on touch release
            holder.itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    holder.password.setText("Password: " + maskPassword(password));
                }
                return false;
            });

        } catch (Exception e) {
            holder.title.setText("⚠️ Decryption Failed");
            holder.username.setText("");
            holder.password.setText("");
            holder.notes.setText("");
            holder.notes.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public VaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vault_entry, parent, false);
        return new VaultViewHolder(view);
    }

    static class VaultViewHolder extends RecyclerView.ViewHolder {
        TextView title, username, password, notes;

        public VaultViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.vaultTitle);
            username = itemView.findViewById(R.id.vaultUsername);
            password = itemView.findViewById(R.id.vaultPassword);
            notes = itemView.findViewById(R.id.vaultNotes);
        }
    }

    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) return "";
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < password.length(); i++) {
            masked.append("•");
        }
        return masked.toString();
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        notifyDataSetChanged(); // refresh UI on changes
    }
}