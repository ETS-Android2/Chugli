package com.example.gupshup.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.example.gupshup.Models.User;
import com.example.gupshup.databinding.ActivityOtpactivityBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mukesh.OnOtpCompletionListener;

import java.util.concurrent.TimeUnit;

import static java.lang.Boolean.TRUE;

public class OTPActivity extends AppCompatActivity {

    ActivityOtpactivityBinding binding;
    FirebaseAuth auth;

    String verificationId;

    FirebaseDatabase database;

    ProgressDialog dialog;

    Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpactivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        new FireBaseAuth.settings.isAppVerificationDisabledForTesting = TRUE;

        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending OTP...");
        dialog.setCancelable(false);
        dialog.show();

        auth = FirebaseAuth.getInstance();

        getSupportActionBar().hide();

        String phoneNumber = getIntent().getStringExtra("phoneNumber");   //this phone number does contain country code - +91 1234567891

        binding.phoneLbl.setText(new StringBuilder().append("Verify ").append(phoneNumber.substring(0, 3)).append(" ").append(phoneNumber.substring(3)).toString()); //+91 1234567891

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)  //the phone number for the account the user is signing up for or signing into. Make sure to pass in a phone number with country code prefixed with plus sign ('+').
                .setTimeout(60L, TimeUnit.SECONDS)  //the maximum amount of time you are willing to wait for SMS auto-retrieval to be completed by the library. Maximum allowed value is 2 minutes. Use 0 to disable SMS-auto-retrieval.
                .setActivity(OTPActivity.this)  //we are working in OTPActivity
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e("verification failed", "onVerificationFailed: "+e);
                    }

                    @Override
                    public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {  //Optional callback. It will trigger when an SMS has been sent to the users phone, and will include a verificationId and PhoneAuthProvider.ForceResendingToken.
                        super.onCodeSent(verifyId, forceResendingToken);
                        Log.i("code sent", "onCodeSent: crossed");
                        dialog.dismiss();  //dismiss the progress dialog
                        verificationId = verifyId;

                        InputMethodManager imm = (InputMethodManager)   getSystemService(Context.INPUT_METHOD_SERVICE);  //requesting keyboard to open automatically
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        binding.otpView.requestFocus();
                    }
                }).build();

        PhoneAuthProvider.verifyPhoneNumber(options);


        binding.otpView.setOtpCompletionListener(new OnOtpCompletionListener() {
            @Override
            public void onOtpCompleted(String otp) {  //this function will be called when user types 6 characters code sent to his mobile phone
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);  //using the verification code and the verification ID that was passed to the onCodeSent to verify the code that was sent tot he user

                auth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {  //also add usesCleartextTraffic in android manifest
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {//successfully login if otp entered is correct

//                            Toast.makeText(OTPActivity.this, phoneNumber, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(OTPActivity.this, SetupProfileActivity.class);
                            startActivity(intent);
                            finishAffinity();


                        } else { //failed login if otp entered is incorrect
                            Toast.makeText(OTPActivity.this, "Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });





    }
}