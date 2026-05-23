package com.example.hookyarn;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class ChatActivity extends AppCompatActivity {

    RecyclerView recyclerChat;
    EditText edtMessage;
    Button btnSend;

    FirebaseFirestore db;

    String receiverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerChat = findViewById(R.id.recyclerChat);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);

        db = FirebaseFirestore.getInstance();

        receiverId = getIntent().getStringExtra("receiverId");

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage(){

        String msg = edtMessage.getText().toString();

        String senderId =
                FirebaseAuth.getInstance().getUid();

        HashMap<String,Object> map = new HashMap<>();

        map.put("senderId", senderId);
        map.put("message", msg);
        map.put("timestamp",
                System.currentTimeMillis());

        String roomId = senderId + receiverId;

        db.collection("chats")
                .document(roomId)
                .collection("messages")
                .add(map);

        edtMessage.setText("");
    }
}