package com.example.gupshup.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.bumptech.glide.Glide;
import com.example.gupshup.Adapters.MessagesAdapter;
import com.example.gupshup.Models.Message;
import com.example.gupshup.Models.MessageBackup;
import com.example.gupshup.Models.MessageBackupArray;
import com.example.gupshup.Models.User;
import com.example.gupshup.R;
import com.example.gupshup.Tools.DBHandler;
import com.example.gupshup.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;

    MessagesAdapter adapter;
    ArrayList<Message> messages;

    String senderRoom, receiverRoom;

    FirebaseDatabase database;
    FirebaseStorage storage;

    ProgressDialog dialog;
    String senderUid;
    String receiverUid;

    PrivateKey privateKey;
    PublicKey receiverPublicKey;
    String strReceiverPublicKey;
    DBHandler dbHandler;
    String SHARED_PREFS = "sharedPrefs";
    ArrayList<MessageBackup> messageBackupArraylist;
    String uid = FirebaseAuth.getInstance().getUid();
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);  //setting our own actionbar instead of android's default action bar

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        String SHARED_PREFS = "sharedPrefs";
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String strPrivateKey = sharedPreferences.getString("strPKey", "");
        String strAESKey = sharedPreferences.getString("strAESKey", "");

        dbHandler = new DBHandler(this, strAESKey);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);

        messages = new ArrayList<>();


        String name = getIntent().getStringExtra("receiverName");
        String profile = getIntent().getStringExtra("receiverImage");
        strReceiverPublicKey = getIntent().getStringExtra("receiverPublicKey");
        try {
            receiverPublicKey = convertPublicStringToKey(strReceiverPublicKey);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("Conversion Error", "Couldn't convert str to PublicKey");
        }



        if(strPrivateKey!="") {
            try {
                Log.i("Conversion Private", "called");
                privateKey = convertPrivateStringToKey(strPrivateKey);
                Log.i("Conversion Private", "done");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("Conversion Error", "Couldn't convert str to PrivateKey");
            }
        }


        binding.name.setText(name);//setting name in toolbar
        Glide.with(ChatActivity.this).load(profile)//setting image in toolbar
                .placeholder(R.drawable.avatar)
                .into(binding.profile);

        binding.imageView2.setOnClickListener(new View.OnClickListener() {  //user should go back on icon back pressed
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        receiverUid = getIntent().getStringExtra("receiverUid");
        senderUid = FirebaseAuth.getInstance().getUid();

        database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {  //checking if clicked user is online or offline, here receiverUid is the id of the user that is clicked in the user list in MainActivity
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);//get the value in String type
                    if (!status.isEmpty()) {
                        if (status.equals("Offline")) {
                            binding.status.setVisibility(View.GONE);
                        } else {
                            binding.status.setText(status);
                            binding.status.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        adapter = new MessagesAdapter(this, messages, senderRoom, receiverRoom, privateKey, strAESKey);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        linearLayoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(adapter);

//
//
//        binding.recyclerView.scrollToPosition(messages.size()-1);


        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Log.i("Scroll 1", "called");

                        binding.recyclerView.scrollToPosition(messages.size());

                        messages.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Message message = snapshot1.getValue(Message.class);

//                            if (!FirebaseAuth.getInstance().getUid().equals(message.getSenderId())) {
//                                //message is received
//                                Log.i("Decryption", "called");
//                                Log.i("getMessage", message.getMessage());
//                                String decryptedMessageTxt = decryptString(message.getMessage());
//                                message.setMessage(decryptedMessageTxt);
//                            }
                            message.setMessageId(snapshot1.getKey());//message id is set to name of child of messages
                            messages.add(message);
                        }

                        adapter.notifyDataSetChanged();
//                        Log.i("Scroll", "called");
//                        binding.recyclerView.scrollToPosition(messages.size());
//                        Log.i("Scroll", "completed");

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageTxt = binding.messageBox.getText().toString();

                String encryptedMessageTxt = encryptString(messageTxt);
//                String decryptedMessageTxt = decryptString(encryptedMessageTxt);
                Log.i("encryptedMessageTxt", encryptedMessageTxt);


                Date date = new Date();
                Message message = new Message(encryptedMessageTxt, senderUid, date.getTime());
                binding.messageBox.setText("");

                //for making the same name of child of messages in both sender room and receiver room nodes. So that we can track which message was touched and hold for long.
                String randomKey = database.getReference().push().getKey();//this push() method generates a unique key every time a new child is added to the specific firebase reference and we have got its key

                Log.i("insertMessage", "start");
                dbHandler.insertMessage(randomKey, messageTxt);
                Log.i("insertMessage", "completed");



                HashMap<String, Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg", message.getMessage()); //getting last message
                lastMsgObj.put("lastMsgTime", date.getTime()); //getting last message time

                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);//adding last message not in the body of Message but in sender room itself as its child and names of childs are lastMsg and lastMsgTime
                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);//adding last message not in the body of Message but in receiver room itself as its child and names of childs are lastMsg and lastMsgTime

                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey)//the name of child of messages of senderRoom
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        database.getReference().child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)//the name of child of messages of receiverRoom
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                            }
                        });
                    }
                });

            }
        });

        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 25);
            }
        });


        //Typing user logic

        final Handler handler = new Handler();  //this will help us in changing the text from typing... to online or offline once the current user has stopped typing
        binding.messageBox.addTextChangedListener(new TextWatcher() {  //This will be called once the users starts typing and the text inside the editText changes
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                database.getReference().child("presence").child(senderUid).setValue("typing...");  //set the presence of current user as typing...
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping, 1000);//timing to show online again after typing... is 1000 milliseconds
            }

            Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(senderUid).setValue("Online");  //show online on the presence of the current user once he has stopped typing for last 1 second
                }
            };
        });


        getSupportActionBar().setDisplayShowTitleEnabled(false);//default actionbar of android

//        getSupportActionBar().setTitle(name);//default actionbar of android
//
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);//default actionbar of android
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String strAESKey = sharedPreferences.getString("strAESKey", "");


        dbHandler = new DBHandler(this, strAESKey);
        messageBackupArraylist = dbHandler.getAESEncryptedDecryptedForBackup();
        Log.i("arr len MAct", messageBackupArraylist.size() + "");
        MessageBackupArray messageBackupArray = new MessageBackupArray(messageBackupArraylist);

        firestore.collection("backups").document(uid)
                .set(messageBackupArray, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.i("Firestore Upload", "Successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore Upload", "Error adding document", e);
                    }
                });

    }

//    public String encrypt(String message) throws Exception{
//        byte[] messageToBytes = message.getBytes();
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//        cipher.init(Cipher.ENCRYPT_MODE,publicKey);
//        byte[] encryptedBytes = cipher.doFinal(messageToBytes);
//        return encode(encryptedBytes);
//    }
//
//    private String encode(byte[] data){
//        return Base64.encodeToString(data, 2);
//    }
//    private byte[] decode(String data){
//
//        return Base64.decode(data, 2);
//    }

    public PublicKey convertPublicStringToKey(String strPublicKeyLocal) throws Exception {
        Log.i("PublicString to key", ": inside");
        try {/*w w w.  j  a  va 2s. c om*/
            byte[] buffer = Base64.decode(strPublicKeyLocal, Base64.DEFAULT);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            Log.i("PublicString Completed", ": inside");

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("");
        } catch (InvalidKeySpecException e) {
            throw new Exception("?");
        } catch (NullPointerException e) {
            throw new Exception("?");
        }
    }

    public PrivateKey convertPrivateStringToKey(String strPrivateKeyLocal) throws Exception {
        Log.i("PrivateString to key", ": inside");
        try {/*w w w.  j  a  va 2s. c om*/
            byte[] buffer = Base64.decode(strPrivateKeyLocal, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            Log.i("PrivateString Completed", ": inside");

            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("");
        } catch (InvalidKeySpecException e) {
            throw new Exception("?");
        } catch (NullPointerException e) {
            throw new Exception("?");
        }
    }

    public String encryptString(String value) {
        byte[] encodedBytes = null;
        try {
            //Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, receiverPublicKey);
            encodedBytes = cipher.doFinal(value.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Base64.encodeToString(encodedBytes, Base64.DEFAULT);
    }

    public String decryptString(String value) {
        byte[] decodedBytes = null;
        try {
            //Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.DECRYPT_MODE, privateKey);
            decodedBytes = c.doFinal(Base64.decode(value, Base64.DEFAULT));
            Log.i("Decryption", "completed");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String(decodedBytes);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 25) {
            if (data != null) {
                if (data.getData() != null) {
                    Uri selectedImage = data.getData();
                    Calendar calendar = Calendar.getInstance();
                    StorageReference reference = storage.getReference().child("chats").child(calendar.getTimeInMillis() + "");
                    dialog.show();
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            dialog.dismiss();
                            if (task.isSuccessful()) {
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String filePath = uri.toString();

                                        String messageTxt = binding.messageBox.getText().toString();

                                        Date date = new Date();
                                        Message message = new Message(messageTxt, senderUid, date.getTime());
                                        message.setMessage("photo");  //for helping us recognizing this message ias actually an image
                                        message.setImageUrl(filePath);
                                        binding.messageBox.setText("");

                                        String randomKey = database.getReference().push().getKey();

                                        HashMap<String, Object> lastMsgObj = new HashMap<>();
                                        lastMsgObj.put("lastMsg", message.getMessage());
                                        lastMsgObj.put("lastMsgTime", date.getTime());

                                        database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                                        database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                                        database.getReference().child("chats")
                                                .child(senderRoom)
                                                .child("messages")
                                                .child(randomKey)
                                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                database.getReference().child("chats")
                                                        .child(receiverRoom)
                                                        .child("messages")
                                                        .child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {

                                                    }
                                                });
                                            }
                                        });

                                        //Toast.makeText(ChatActivity.this, filePath, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {  //if user directly presses home button while staying on ChatActivity then this code would change its presence from online to offline
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}