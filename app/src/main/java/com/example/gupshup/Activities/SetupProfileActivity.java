package com.example.gupshup.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.example.gupshup.Models.User;
import com.example.gupshup.R;
import com.example.gupshup.databinding.ActivitySetupProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
import android.util.Base64;

public class SetupProfileActivity extends AppCompatActivity {

    ActivitySetupProfileBinding binding;
    FirebaseAuth auth; //for telling us which user is logged in
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog;
    String userId;
    PrivateKey privateKey;
    PublicKey publicKey;
    String strPrivateKey = "";
    String strPublicKey = "";
    int keyGenerated = 0;
    String nameGlobal;
    int isUserNew = 0;
    int isKeyNeeded = 0;
    User userGlobal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating profile...");
        dialog.setCancelable(false);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getUid();

        database.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(userId)) {
                    Log.i("hasChild", "true");
                    isUserNew=0;

                    userGlobal = snapshot.child(userId).getValue(User.class);
                    if(userGlobal.getKeyGenerated()==0){
                        isKeyNeeded=1;
                    }
                    else{
                        isKeyNeeded=0;
                        keyGenerated=1;
                        strPrivateKey=userGlobal.getPrivateKey();
                        strPublicKey=userGlobal.getPublicKey();
                    }
                }
                else{
                    isUserNew=1;
                    isKeyNeeded=1;
                    Log.i("hasChild", "false");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        getSupportActionBar().hide();

        Log.i("onCreate", ": inside");

        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*"); //type of data we want in our intent window
                startActivityForResult(intent, 45);  //requestCode is of our wish
            }
        });



        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.nameBox.getText().toString();
                nameGlobal = name;

                if(name.isEmpty()) {
                    binding.nameBox.setError("Please enter a name."); //for showing message on the tip of editText
                    return;
                }

                if(isKeyNeeded==1) {
                    Log.i("generateKey Called", ": called");
                    generateRSAKeys();

                    Log.i("convertKey Called", ": called");
                    convertKeyToString(privateKey, publicKey);
                }
                else{
                    try {
                        Log.i("try private", ": inside");
                        PrivateKey localPrivateKey = convertPrivateStringToKey(strPrivateKey);
                        String strLocalPrivateKey = Base64.encodeToString(localPrivateKey.getEncoded(), 2);
                        Log.i("Local Private Key", strLocalPrivateKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("catch private", ": inside");
                    }
                    try {
                        Log.i("try public", ": inside");
                        PublicKey localPublicKey = convertPublicStringToKey(strPublicKey);
                        String strLocalPublicKey = Base64.encodeToString(localPublicKey.getEncoded(), 2);
                        Log.i("Local Public Key", strLocalPublicKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("catch public", ": inside");
                    }
                }

                Log.i("After gen in user!=null", ": inside");

                Log.i("After user!=null", ": outside");
                dialog.show(); //showing progress dialog

                if(selectedImage != null) {
                    StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if(task.isSuccessful()) {
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        String imageUrl = uri.toString();

                                        String uid = auth.getUid();
                                        String phone = auth.getCurrentUser().getPhoneNumber();
                                        String name = binding.nameBox.getText().toString();

                                        Log.i("Private Key 2", strPrivateKey);
                                        Log.i("Public Key 2", strPublicKey);
                                        Log.i("keyGenerated 2", keyGenerated + "");

                                        String emptyPrivateKey = "";

                                        User userLocal1 = new User(uid, name, phone, imageUrl, emptyPrivateKey, strPublicKey, keyGenerated);  //creating object of User

                                        uploadUserDetails(userLocal1);
                                    }
                                });
                            }
                        }
                    });
                } else {
                    String phone = auth.getCurrentUser().getPhoneNumber();

                    Log.i("Private Key 3", strPrivateKey);
                    Log.i("Public Key 3", strPublicKey);
                    Log.i("keyGenerated 3", keyGenerated + "");

                    String emptyPrivateKey = "";

                    User userLocal2 = new User(userId, name, phone, "No Image", emptyPrivateKey, strPublicKey, keyGenerated);
                    uploadUserDetails(userLocal2);


                }

            }
        });
    }



    public void uploadUserDetails(User user){

        database.getReference()
                .child("users")
                .child(userId)
                .setValue(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        dialog.dismiss();
                        if(isUserNew==0) {
                            Intent intent = new Intent(SetupProfileActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else{
                            Intent intent = new Intent(SetupProfileActivity.this, PrivateKeyEncryptionActivity.class);
                            intent.putExtra("strPrivateKey", strPrivateKey);
                            intent.putExtra("user", user);
                            startActivity(intent);
                            finish();
                        }
                    }
                });

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

//    public PublicKey convertPublicStringToKey(String strPublicKeyLocal) throws Exception {
//        Log.i("PublicString to key", ": inside");
//        try {/*w w w.  j  a  va 2s. c om*/
//            byte[] buffer = Base64.decode(strPublicKeyLocal, Base64.DEFAULT);
//            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
//            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//
//            Log.i("PublicString Completed", ": inside");
//
//            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
//        } catch (NoSuchAlgorithmException e) {
//            throw new Exception("");
//        } catch (InvalidKeySpecException e) {
//            throw new Exception("?");
//        } catch (NullPointerException e) {
//            throw new Exception("?");
//        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null) {
            if(data.getData() != null) {//user has selected an image

                selectedImage = data.getData();
                Glide
                        .with(SetupProfileActivity.this)
                        .load(data.getData())
                        .centerCrop()
                        .placeholder(R.drawable.avatar)
                        .into(binding.imageView);
            }
        }
    }
}