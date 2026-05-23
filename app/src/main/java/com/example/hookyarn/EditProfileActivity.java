package com.example.hookyarn;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;


import java.util.HashMap;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView imgProfile, btnBack, btnSave;
    private TextView txtChangePhoto;

    private EditText etUsername,  etBio, etEmail;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private Uri imageUri;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        imgProfile = findViewById(R.id.imgProfile);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        txtChangePhoto = findViewById(R.id.txtChangePhoto);
        etUsername = findViewById(R.id.etUsername);

        etBio = findViewById(R.id.etBio);
        etEmail = findViewById(R.id.etEmail);



        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK
                            && result.getData() != null) {

                        imageUri = result.getData().getData();

                        Glide.with(EditProfileActivity.this)
                                .load(imageUri)
                                .circleCrop()
                                .into(imgProfile);
                    }
                }
        );

        txtChangePhoto.setOnClickListener(v -> openGallery());

        imgProfile.setOnClickListener(v -> openGallery());

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void openGallery() {
        ImagePicker.with(this)
                .cropSquare()              
                .compress(1024)            
                .maxResultSize(1080, 1080)  
                .galleryOnly()
                .createIntent(intent -> {
                    imagePickerLauncher.launch(intent);
                    return null;
                });
    }

    private void saveProfile() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = etUsername.getText().toString().trim();
        String bio = etBio.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        if (imageUri != null) {
            uploadProfileImage(username, bio, email);
        } else {
            updateFirestore(username, bio, email, null);
        }
    }

    private void uploadProfileImage(String username, String bio, String email) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            btnSave.setEnabled(true);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "profile_" + uid + ".jpg";
        SupabaseHelper.uploadFile(this, "posts", fileName, imageUri, new SupabaseHelper.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                updateFirestore(username, bio, email, publicUrl);
            }

            @Override
            public void onError(Exception e) {
                btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestore(String username, String bio, String email, String profileImageUrl) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            btnSave.setEnabled(true);
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("bio", bio);
        map.put("email", email);
        if (profileImageUrl != null) {
            map.put("profileImageUrl", profileImageUrl);
        }

        firestore.collection("users")
                .document(uid)
                .set(map, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    updateAuthProfile(username, profileImageUrl);
                    updateUserContent(uid, username, profileImageUrl);
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAuthProfile(String username, String profileImageUrl) {
        if (auth.getCurrentUser() == null) return;

        com.google.firebase.auth.UserProfileChangeRequest.Builder builder = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(username);
        
        if (profileImageUrl != null) {
            builder.setPhotoUri(Uri.parse(profileImageUrl));
        }

        auth.getCurrentUser().updateProfile(builder.build());
    }

    private void updateUserContent(String uid, String username, String profileImageUrl) {
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        
        firestore.collection("posts").whereEqualTo("uid", uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                batch.update(doc.getReference(), "username", username);
                if (profileImageUrl != null) batch.update(doc.getReference(), "profileImageUrl", profileImageUrl);
            }
            
            firestore.collection("threads").whereEqualTo("uid", uid).get().addOnSuccessListener(threadSnapshots -> {
                for (com.google.firebase.firestore.DocumentSnapshot doc : threadSnapshots) {
                    batch.update(doc.getReference(), "username", username);
                    if (profileImageUrl != null) batch.update(doc.getReference(), "profileImageUrl", profileImageUrl);
                }
                
                batch.commit().addOnCompleteListener(task -> {
                    Toast.makeText(EditProfileActivity.this, "Profile and Posts Updated", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            });
        }).addOnFailureListener(e -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    private void loadProfile() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        String currentEmail = auth.getCurrentUser().getEmail();
        if (currentEmail != null) etEmail.setText(currentEmail);
        
        String currentDisplayName = auth.getCurrentUser().getDisplayName();
        if (currentDisplayName != null && !currentDisplayName.isEmpty()) {
            etUsername.setText(currentDisplayName);
        } else {
            etUsername.setText("user" + uid.substring(0, 5));
        }

        if (auth.getCurrentUser().getPhotoUrl() != null) {
            Glide.with(this).load(auth.getCurrentUser().getPhotoUrl()).circleCrop().into(imgProfile);
        }

        firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if(documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");
                        String email = documentSnapshot.getString("email");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        if (username != null && !username.isEmpty()) etUsername.setText(username);
                        if (bio != null) etBio.setText(bio);
                        if (email != null && !email.isEmpty()) etEmail.setText(email);
                        
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(imgProfile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
