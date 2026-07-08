package com.example.secureme;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ItemVaultEntry extends RecyclerView.ViewHolder {

    private TextView vaultTitle, vaultUsername, vaultPassword, vaultNotes;

    public ItemVaultEntry(@NonNull View itemView) {
        super(itemView);

        vaultTitle = itemView.findViewById(R.id.vaultTitle);
        vaultUsername = itemView.findViewById(R.id.vaultUsername);
        vaultPassword = itemView.findViewById(R.id.vaultPassword);
        vaultNotes = itemView.findViewById(R.id.vaultNotes);
    }

    public void bindData(String title, String username, String password, String notes) {
        vaultTitle.setText(title);
        vaultUsername.setText("Username: " + username);
        vaultPassword.setText("Password: " + maskPassword(password));

        if (notes != null && !notes.trim().isEmpty()) {
            vaultNotes.setVisibility(View.VISIBLE);
            vaultNotes.setText("Notes: " + notes);
        } else {
            vaultNotes.setVisibility(View.GONE);
        }
    }

    private String maskPassword(String password) {
        // Just to make sure the password stays masked when displayed
        return password.replaceAll(".", "•");
    }
}
