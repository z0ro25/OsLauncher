package com.amz.ios.themeclub.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by ZhangMingZhe on 11/17/16.
 */

public class Header {

    public static final int HEADER_LENGTH = 28;
    public static final int TLV_HEADER_LENGTH = 84;

    public static final int XIP_REQUEST = 1;
    public static final int XIP_RESPONSE = 2;
    public static final int XIP_NOTIFY = 3;

    private byte basicVer = 1;

    private int length = 0;

    private byte type = 1;

    private short reserved = 0;

    private long firstTransaction;

    private long secondTransaction;

    private int messageCode;

    public byte getBasicVer() {
        return basicVer;
    }

    public void setBasicVer(byte basicVer) {
        this.basicVer = basicVer;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public short getReserved() {
        return reserved;
    }

    public void setReserved(short reserved) {
        this.reserved = reserved;
    }

    public long getFirstTransaction() {
        return firstTransaction;
    }

    public void setFirstTransaction(long firstTransaction) {
        this.firstTransaction = firstTransaction;
    }

    public long getSecondTransaction() {
        return secondTransaction;
    }

    public void setSecondTransaction(long secondTransaction) {
        this.secondTransaction = secondTransaction;
    }

    public int getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(int messageCode) {
        if (messageCode <= 0) {
            throw new RuntimeException("invalid message code.");
        }
        this.messageCode = messageCode;
    }

    public void setTransaction(UUID uuid) {
        this.firstTransaction = uuid.getMostSignificantBits();
        this.secondTransaction = uuid.getLeastSignificantBits();
    }

    public UUID getTransactionAsUUID() {
        return new UUID(this.firstTransaction, this.secondTransaction);
    }

    public void setHeaderType(byte header_type) {

        this.type = header_type;
    }

    public String toString() {
        JSONObject jsObject = new JSONObject();
        try {
            jsObject.put("ver", basicVer);
            jsObject.put("type", type);
            jsObject.put("msb", firstTransaction);
            jsObject.put("lsb", secondTransaction);
            jsObject.put("mcd", messageCode);
            return jsObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "";
    }
}
