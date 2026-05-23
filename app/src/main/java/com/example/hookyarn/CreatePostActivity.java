package com.example.hookyarn;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CreatePostActivity extends AppCompatActivity {

    private static final int MAX_SIZE_MB = 100;

    private ImageView ivMediaPreview;
    private ImageView ivAddMedia;
    private ImageView ivRemoveMedia;
    private EditText etCaption;
    private Button btnPost;
    private ProgressBar progressBar;
    private TextView tvFileSize;
    private LinearLayout llMediaInfo;

    private Uri mediaUri;
    private String mediaType = "";
    private long mediaSize = 0;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    mediaUri = result.getData().getData();
                    mediaType = "image";
                    handleImageSelection();
                }
            }
    );

    private final ActivityResultLauncher<Intent> videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    mediaUri = result.getData().getData();
                    mediaType = "video";
                    handleVideoSelection();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        initViews();
        setupClickListeners();
        checkPermissions();
    }

    private void initViews() {
        ivMediaPreview = findViewById(R.id.iv_media_preview);
        ivAddMedia = findViewById(R.id.iv_add_media);
        ivRemoveMedia = findViewById(R.id.iv_remove_media);
        etCaption = findViewById(R.id.et_caption);
        btnPost = findViewById(R.id.btn_post);
        progressBar = findViewById(R.id.progress_bar);
        tvFileSize = findViewById(R.id.tv_file_size);
        llMediaInfo = findViewById(R.id.ll_media_info);
    }

    private void setupClickListeners() {
        ivAddMedia.setOnClickListener(v -> showMediaPickerDialog());
        btnPost.setOnClickListener(v -> publishPost());

        if (ivRemoveMedia != null) {
            ivRemoveMedia.setOnClickListener(v -> removeMedia());
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> handleBackNavigation());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
    }

    private void handleBackNavigation() {
        Intent intent = new Intent(CreatePostActivity.this, HomeActivity.class);
        intent.putExtra("SHOW_CREATE_SHEET", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showMediaPickerDialog() {
        String[] options = {"📷 Take Photo", "📱 Choose from Gallery", "🎥 Choose Video"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Media (Max 100MB)")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        pickImage();
                    } else {
                        pickVideo();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openCamera() {
        ImagePicker.with(this)
                .cameraOnly()
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent(intent -> {
                    imagePickerLauncher.launch(intent);
                    return kotlin.Unit.INSTANCE;
                });
    }

    private void pickImage() {
        ImagePicker.with(this)
                .galleryOnly()
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .createIntent(intent -> {
                    imagePickerLauncher.launch(intent);
                    return kotlin.Unit.INSTANCE;
                });
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        videoPickerLauncher.launch(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void handleImageSelection() {
        try {
            mediaSize = getFileSize(mediaUri);

            if (mediaSize > MAX_SIZE_MB * 1024 * 1024) {
                showSizeError();
                return;
            }

            Glide.with(this)
                    .load(mediaUri)
                    .centerCrop()
                    .into(ivMediaPreview);

            ivMediaPreview.setVisibility(View.VISIBLE);
            if (llMediaInfo != null) {
                llMediaInfo.setVisibility(View.VISIBLE);
                tvFileSize.setText(String.format("Size: %.2f MB", mediaSize / (1024.0 * 1024.0)));
            }
            ivAddMedia.setVisibility(View.GONE);
            if (ivRemoveMedia != null) ivRemoveMedia.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Image selected successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVideoSelection() {
        try {
            mediaSize = getFileSize(mediaUri);

            if (mediaSize > MAX_SIZE_MB * 1024 * 1024) {
                showSizeError();
                return;
            }

            Glide.with(this)
                    .load(mediaUri)
                    .centerCrop()
                    .into(ivMediaPreview);

            ivMediaPreview.setVisibility(View.VISIBLE);
            if (llMediaInfo != null) {
                llMediaInfo.setVisibility(View.VISIBLE);
                tvFileSize.setText(String.format("Video Size: %.2f MB", mediaSize / (1024.0 * 1024.0)));
            }
            ivAddMedia.setVisibility(View.GONE);
            if (ivRemoveMedia != null) ivRemoveMedia.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Video selected successfully!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error loading video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        if (uri == null) return size;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                cursor.moveToFirst();
                size = cursor.getLong(sizeIndex);
                cursor.close();
            }
        } else {
            String path = getRealPathFromURI(uri);
            if (path != null) {
                File file = new File(path);
                size = file.length();
            }
        }
        return size;
    }

    private void showSizeError() {
        Toast.makeText(this, "File size exceeds " + MAX_SIZE_MB + "MB limit!", Toast.LENGTH_LONG).show();
        removeMedia();
    }

    private void removeMedia() {
        mediaUri = null;
        mediaType = "";
        mediaSize = 0;
        if (ivMediaPreview != null) {
            ivMediaPreview.setImageDrawable(null);
            ivMediaPreview.setVisibility(View.GONE);
        }
        if (llMediaInfo != null) {
            llMediaInfo.setVisibility(View.GONE);
        }
        ivAddMedia.setVisibility(View.VISIBLE);
        if (ivRemoveMedia != null) ivRemoveMedia.setVisibility(View.GONE);
    }

    private void publishPost() {
        String caption = etCaption.getText().toString().trim();

        if (TextUtils.isEmpty(mediaType) && TextUtils.isEmpty(caption)) {
            Toast.makeText(this, "Please add an image or write a caption", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnPost.setEnabled(false);
        
        if (mediaUri != null) {
            uploadToServer();
        } else {
            savePostToFirestore(com.google.firebase.auth.FirebaseAuth.getInstance().getUid(), "");
        }
    }

    private void uploadToServer() {
        if (mediaUri == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnPost.setEnabled(false);

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "post_" + System.currentTimeMillis() + (mediaType.equals("video") ? ".mp4" : ".jpg");

        SupabaseHelper.uploadFile(this, "posts", fileName, mediaUri, new SupabaseHelper.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                savePostToFirestore(uid, publicUrl);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnPost.setEnabled(true);
                Toast.makeText(CreatePostActivity.this, "Supabase Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePostToFirestore(String uid, String mediaUrl) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        String postId = db.collection("posts").document().getId();
        String caption = etCaption.getText().toString().trim();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            String username = documentSnapshot.getString("username");
            if (username == null || username.isEmpty()) {
                username = "user" + (uid != null && uid.length() >= 5 ? uid.substring(0, 5) : "XXXXX");
            }
            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

            java.util.Map<String, Object> post = new java.util.HashMap<>();
            post.put("id", postId);
            post.put("uid", uid);
            post.put("username", username != null ? username : "User");
            post.put("profileImageUrl", profileImageUrl);
            post.put("imageUrl", mediaUrl);
            post.put("caption", caption);
            post.put("likeCount", 0);
            post.put("commentCount", 0);
            post.put("likedBy", new java.util.ArrayList<String>());
            post.put("createdAt", com.google.firebase.Timestamp.now());

            db.collection("posts").document(postId).set(post)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Post published successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        btnPost.setEnabled(true);
                        Toast.makeText(this, "Failed to save post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @SuppressWarnings("deprecation")
    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null)) {
            if (cursor == null) return null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception e) {
            return null;
        }
    }

    private void checkPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.CAMERA
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            };
        }

        Dexter.withContext(this)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (!report.areAllPermissionsGranted()) {
                            Toast.makeText(CreatePostActivity.this,
                                    "Camera and storage permissions needed to create posts",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                                   PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }
}