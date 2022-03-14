package com.example.gupshup.Models;

public class MessageBackup {
    private String messageId, AESEncryptedDecryptedMessage;

    public MessageBackup() {
    }

    public MessageBackup(String messageId, String AESEncryptedDecryptedMessage) {
        this.messageId = messageId;
        this.AESEncryptedDecryptedMessage = AESEncryptedDecryptedMessage;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAESEncryptedDecryptedMessage() {
        return AESEncryptedDecryptedMessage;
    }

    public void setAESEncryptedDecryptedMessage(String decryptedMessage) {
        this.AESEncryptedDecryptedMessage = decryptedMessage;
    }
}