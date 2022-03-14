package com.example.gupshup.Models;

import java.util.ArrayList;

public class MessageBackupArray {

    public MessageBackupArray() {
    }

    public ArrayList<MessageBackup> messageBackupArrayList;

    public MessageBackupArray(ArrayList<MessageBackup> messageBackupArrayList) {
        this.messageBackupArrayList = messageBackupArrayList;
    }

    public ArrayList<MessageBackup> getMessageBackupArrayList() {
        return messageBackupArrayList;
    }

    public void setMessageBackupArrayList(ArrayList<MessageBackup> messageBackupArrayList) {
        this.messageBackupArrayList = messageBackupArrayList;
    }
}
