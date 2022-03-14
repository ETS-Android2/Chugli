package com.example.gupshup.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.example.gupshup.Models.MessageBackup;
import com.example.gupshup.Models.MessageBackupArray;
import com.example.gupshup.Models.User;
import com.example.gupshup.Tools.DBHandler;
import com.example.gupshup.databinding.ActivityPrivateKeyDecryptionBinding;
import com.example.gupshup.databinding.ActivityPrivateKeyEncryptionBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.scottyab.aescrypt.AESCrypt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class PrivateKeyDecryptionActivity extends AppCompatActivity {

    ActivityPrivateKeyDecryptionBinding binding;
    String strAESKey;
    String strPrivateKey;
    User user;
    FirebaseDatabase database;
    String userId;
    FirebaseAuth auth;
    ProgressDialog dialog;
    PrivateKey privateKey;
    PublicKey publicKey;
    String strPublicKey = "";
    int keyGenerated = 0;
    DBHandler dbHandler;
    String uid = FirebaseAuth.getInstance().getUid();
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    ArrayList<MessageBackup> messageBackupArraylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivateKeyDecryptionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Key...");
        dialog.setCancelable(false);

        Intent intent = getIntent();
        strPrivateKey = intent.getStringExtra("strPrivateKey");
        user = (User) intent.getSerializableExtra("user");

        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getUid();


        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                strAESKey = binding.tvKey.getText().toString();

                boolean isValid = true;

                if (strAESKey.isEmpty()) {
                    binding.tvKey.setError("Please enter Key."); //for showing message on the tip of editText
                    return;
                }
                else{
                    dialog.show();

                    //decryption algorithm
                    try {
                        String strDecryptedPrivateKey = AESCrypt.decrypt(strAESKey, strPrivateKey);
                        saveToSharedPreferences(strDecryptedPrivateKey);
                        Log.i("DPRK :", strDecryptedPrivateKey);

                        String strAESKeyLocal = strAESKey;
                        Log.i("strAESKey PKDAct", strAESKeyLocal);


                        dbHandler = new DBHandler(PrivateKeyDecryptionActivity.this, strAESKeyLocal);

                        try {
                            dbHandler.createDatabase();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        dbHandler.openDatabase();
                        dbHandler.createTable();

                        //check if backup exist or not in firestore
                        firestore.collection("backups").document(uid)
                                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        MessageBackupArray messageBackupArray = document.toObject(MessageBackupArray.class);
                                        messageBackupArraylist = messageBackupArray.getMessageBackupArrayList();
                                        Log.i("arr len PKDAct", messageBackupArraylist.size()+"");
                                        dbHandler.insertBackupData(messageBackupArraylist);
                                        Log.i("Backup insertion", "Completed");
                                    } else {
                                        Log.i("Backup", "Document does not exist!");
                                    }
                                } else {
                                    Log.i("Backup", "Task Failed with: ", task.getException());
                                }
                            }
                        });
                    }catch (GeneralSecurityException e){
                        //handle error - could be due to incorrect password or tampered encryptedMsg
                        dialog.dismiss();
                        binding.tvKey.setError("Invalid Key!");
                    }
                }
            }
        }
        );

        binding.resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("generateKey Called", ": called");
                generateRSAKeys();

                Log.i("convertKey Called", ": called");
                convertKeyToString(privateKey, publicKey);

                user.setPrivateKey("");
                user.setPublicKey(strPublicKey);

                Intent intent = new Intent(PrivateKeyDecryptionActivity.this, PrivateKeyEncryptionActivity.class);
                intent.putExtra("strPrivateKey", strPrivateKey);
                intent.putExtra("user", user);
                startActivity(intent);
                finish();

            }
            }
        );

    }


    public void generateRSAKeys(){
        Log.i("generateRSAKeys", ": inside");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            KeyPair pair = generator.generateKeyPair();
            privateKey=pair.getPrivate();
            publicKey= pair.getPublic();

            Log.i("generation Completed", ": inside");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void convertKeyToString(PrivateKey privateKeyLocal, PublicKey publicKeyLocal){
        Log.i("ConvertRSAKeys", ": inside");
        //Conversion starts
        strPrivateKey = Base64.encodeToString(privateKey.getEncoded(), 2);
        strPublicKey = Base64.encodeToString(publicKey.getEncoded(), 2);
        //Conversion ends

        Log.i("Conversion Completed", ": inside");

        Log.i("Private Key", strPrivateKey);
        Log.i("Public Key", strPublicKey);

        keyGenerated = 1;
    }

    public void saveToSharedPreferences(String strDecryptedPrivateKey){

        String SHARED_PREFS = "sharedPrefs";

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("strPKey", strDecryptedPrivateKey);
        editor.putString("strAESKey", strAESKey);
        editor.apply();
        dialog.dismiss();
        Intent intent = new Intent(PrivateKeyDecryptionActivity.this, MainActivity.class);
        intent.putExtra("ActivityName", "PKDAct");
        startActivity(intent);
        finish();

//        public static String loadData(Context context) {
//            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
//            String text = sharedPreferences.getString(KEY, "");
//            return text;
//        }
    }
}