package com.example.secureme;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class VaultActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private VaultAdapter adapter;
    private FloatingActionButton fabAdd;

    private ActivityResultLauncher<Intent> vaultEntryLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> openVaultEntryWithAnimation());

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupAdapter();

        // Set up launcher to detect result when coming back from VaultEntryActivity
        vaultEntryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && adapter != null) {
                        adapter.notifyDataSetChanged(); // Simply refresh the view safely!
                    }
                }
        );
    }

    private void setupAdapter() {
        String userId = auth.getCurrentUser().getUid();

        Query query = db.collection("users")
                .document(userId)
                .collection("vault")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<VaultItem> options = new FirestoreRecyclerOptions.Builder<VaultItem>()
                .setQuery(query, VaultItem.class)
                .setLifecycleOwner(this) // Attach to this Activity's lifecycle
                .build();

        adapter = new VaultAdapter(options, this);
        recyclerView.setAdapter(adapter);

        // Optional: show animation if empty
        query.get().addOnSuccessListener(snapshot -> {
            if (snapshot.isEmpty()) {
                showFabCoachMark();
            }
        });
    }

    private void openVaultEntryWithAnimation() {
        Intent intent = new Intent(this, VaultEntryActivity.class);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                Pair.create((View) fabAdd, "fabTransition")
        );
        vaultEntryLauncher.launch(intent);
    }

    private void showFabCoachMark() {
        TapTargetView.showFor(this,
                TapTarget.forView(fabAdd, "Tap here twice to add an account.")
                        .outerCircleColor(R.color.white)
                        .outerCircleAlpha(1)
                        .targetCircleColor(android.R.color.holo_blue_dark)
                        .titleTextSize(23)
                        .titleTextColor(android.R.color.black)
                        .descriptionTextSize(17)
                        .descriptionTextColor(android.R.color.black)
                        .textColor(android.R.color.black)
                        .cancelable(true)
                        .tintTarget(false)
                        .transparentTarget(true)
        );
    }
}
