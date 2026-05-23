package com.example.hookyarn;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Create_Account extends AppCompatActivity {

    private TextInputLayout nameLayout, surnameLayout, emailLayout,
            passwordLayout, confirmPasswordLayout;
    private TextInputEditText nametxtfield, surnametxtbox,
            txtemailadd, passwordtxtfield, confirmPasswordField;
    private MaterialButton bttnsignup, backtologinBttn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        nameLayout            = findViewById(R.id.nameLayout);
        surnameLayout         = findViewById(R.id.surnameLayout);
        emailLayout           = findViewById(R.id.emailLayout);
        passwordLayout        = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);

        nametxtfield         = findViewById(R.id.nametxtfield);
        surnametxtbox        = findViewById(R.id.surnametxtbox);
        txtemailadd          = findViewById(R.id.txtemailadd);
        passwordtxtfield     = findViewById(R.id.passwordtxtfield);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);

        bttnsignup      = findViewById(R.id.bttnsignup);
        backtologinBttn = findViewById(R.id.backtologinBttn);

        bttnsignup.setOnClickListener(v -> validateAndRegister());

        backtologinBttn.setOnClickListener(v -> {
            startActivity(new Intent(Create_Account.this, MainActivity.class));
            finish();
        });
    }

    private void validateAndRegister() {

        String name     = nametxtfield.getText().toString().trim();
        String surname  = surnametxtbox.getText().toString().trim();
        String email    = txtemailadd.getText().toString().trim();
        String password = passwordtxtfield.getText().toString().trim();
        String confirm  = confirmPasswordField.getText().toString().trim();

        nameLayout.setError(null);
        surnameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Name is required");
            nametxtfield.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(surname)) {
            surnameLayout.setError("Surname is required");
            surnametxtbox.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            txtemailadd.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Enter a valid email");
            txtemailadd.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            passwordtxtfield.requestFocus();
            return;
        }
        if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            passwordtxtfield.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordLayout.setError("Passwords do not match");
            confirmPasswordField.requestFocus();
            return;
        }

        registerUser(name, surname, email, password);
    }

    private void registerUser(String name, String surname,
                              String email, String password) {

        bttnsignup.setEnabled(false);
        bttnsignup.setText("Creating account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(uid, name, surname, email);

                    } else {
                        bttnsignup.setEnabled(true);
                        bttnsignup.setText("SIGN UP");

                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed. Try again.";

                        Toast.makeText(Create_Account.this,
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name,
                                     String surname, String email) {

        String defaultUsername = "user" + uid.substring(0, 5);

        Map<String, Object> user = new HashMap<>();
        user.put("name",      name);
        user.put("surname",   surname);
        user.put("username",  defaultUsername);
        user.put("email",     email);
        user.put("uid",       uid);
        user.put("createdAt", com.google.firebase.Timestamp.now());
        user.put("profileImageUrl", "");
        user.put("bio", "");

        db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(Create_Account.this,
                            "Account created! Please log in.", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(Create_Account.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {

                    bttnsignup.setEnabled(true);
                    bttnsignup.setText("SIGN UP");

                    Toast.makeText(Create_Account.this,
                            "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}