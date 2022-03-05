package com.example.gupshup.Models;

import java.io.Serializable;

public class User implements Serializable {

    private String uid, name, phoneNumber, profileImage, strPrivateKey, strPublicKey;
    private int keyGenerated;

    public User() {  //Remember we need empty constructor always when we deal with firebase

    }

    public User(String uid, String name, String phoneNumber, String profileImage, String privateKey, String publicKey, int keyGenerated) {
        this.uid = uid;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.profileImage = profileImage;
        this.strPrivateKey = privateKey;
        this.strPublicKey = publicKey;
        this.keyGenerated = keyGenerated;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getPrivateKey() {
        return strPrivateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.strPrivateKey = privateKey;
    }

    public String getPublicKey() {
        return strPublicKey;
    }

    public void setPublicKey(String publicKey) {
        this.strPublicKey = publicKey;
    }

    public int getKeyGenerated() {
        return keyGenerated;
    }

    public void setKeyGenerated(int keyGenerated) {
        this.keyGenerated = keyGenerated;
    }
}

