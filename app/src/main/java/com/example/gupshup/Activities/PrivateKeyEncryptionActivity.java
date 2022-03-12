package com.example.gupshup.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.gupshup.Models.User;
import com.example.gupshup.R;
import com.example.gupshup.databinding.ActivityPrivateKeyEncryptionBinding;
import com.example.gupshup.databinding.ActivitySetupProfileBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.scottyab.aescrypt.AESCrypt;

import java.security.GeneralSecurityException;

public class PrivateKeyEncryptionActivity extends AppCompatActivity {

    ActivityPrivateKeyEncryptionBinding binding;
    String strAESKey;
    String strPrivateKey;
    User user;
    FirebaseDatabase database;
    String userId;
    FirebaseAuth auth;
    ProgressDialog dialog;
    String regex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivateKeyEncryptionBinding.inflate(getLayoutInflater());
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

        regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{8,20}$";


        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                strAESKey = binding.tvKey.getText().toString();

                boolean isValid = true;

                if (strAESKey.isEmpty()) {
                    binding.tvKey.setError("Please enter Key."); //for showing message on the tip of editText
                }
                if (strAESKey.length() > 20 || strAESKey.length() < 8)
                {
                    binding.tvKey.setError("Key Length must be between 8 and 20.");
                    isValid = false;
                }
                String upperCaseChars = "(.*[A-Z].*)";
                if (!strAESKey.matches(upperCaseChars ))
                {
                    binding.tvKey.setError("Key must have atleast one uppercase character");
                    isValid = false;
                }
                String lowerCaseChars = "(.*[a-z].*)";
                if (!strAESKey.matches(lowerCaseChars ))
                {
                    binding.tvKey.setError("Key must have atleast one lowercase character");
                    isValid = false;
                }
                String numbers = "(.*[0-9].*)";
                if (!strAESKey.matches(numbers ))
                {
                    binding.tvKey.setError("Key must have atleast one number");
                    isValid = false;
                }

                if(isValid==false){
                    return;
                }
                else{
                    try {
                        dialog.show();
                        encrypt();
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        dialog.dismiss();
                    }
                }
            }
        }
        );
    }

    public void encrypt() throws GeneralSecurityException {
        String strEncryptedPrivateKey = AESCrypt.encrypt(strAESKey, strPrivateKey);
//        decrypt();
        saveToSharedPreferences();
        uploadKeyToFirebase(strEncryptedPrivateKey);
    }

    public void uploadKeyToFirebase(String strEncryptedPrivateKey){
        user.setPrivateKey(strEncryptedPrivateKey);

        database.getReference()
                .child("users")
                .child(userId)
                .setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        dialog.dismiss();
                        Intent intent = new Intent(PrivateKeyEncryptionActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    public void saveToSharedPreferences(){

        String SHARED_PREFS = "sharedPrefs";

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("strPKey", strPrivateKey);
        editor.putString("strAESKey", strAESKey);
        editor.apply();

//        public static String loadData(Context context) {
//            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
//            String text = sharedPreferences.getString(KEY, "");
//            return text;
//        }
    }

//    public void decrypt() throws GeneralSecurityException {
//        String strDecryptedPrivateKey = AESCrypt.decrypt(strAESKey, temp);
//        Log.i("decrypt private: ", strDecryptedPrivateKey);
//    }
}