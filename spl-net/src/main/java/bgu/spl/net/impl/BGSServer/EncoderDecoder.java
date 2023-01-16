package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EncoderDecoder implements MessageEncoderDecoder<String> {
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private int start = 0;
    private String result="";
    private boolean op =false;

    @Override
    public String decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if (nextByte == '\0' && start!=len) {
            result += popString() + " ";
            start = len;
            return null;

        }
        if (len-start == 2 && !op) {
            byte[]temp = new byte[2];
            temp[0] = bytes[len-2];
            temp[1] = bytes[len-1];
            result += bytesToShort(temp)+ " ";
            start = len;
            op = true;
        }
        if (nextByte == ';') {
//            len = 0;
            String temp = result + popString();
            result ="";
            start = len;
            op = false;
            return temp;
        }

        pushByte(nextByte);

        return null; //not a line yet
    }

    @Override
    public byte[] encode(String message) {
        int size=0;
        byte[] bytes1 = new byte[1 << 10];
        String op = message.substring(0,message.indexOf(" ")); //getting op code 9/10/11
        Short shortOp = Short.valueOf(op);
        byte[] byteOp = shortToBytes(shortOp);
        bytes1[size++] = byteOp[0];
        bytes1 = checkResize(bytes1, size);
        bytes1[size++] = byteOp[1];
        bytes1 = checkResize(bytes1, size);
        message = message.substring(message.indexOf(" ")+1);

        if (op.equals("10") || op.equals("11")) {
            String msgOp = message.substring(0, message.indexOf(" ")); //getting msg op code
            Short shortMsgOp = Short.valueOf(msgOp);
            byte[] byteMsgOp = shortToBytes(shortMsgOp);
            bytes1[size++] = byteMsgOp[0];
            bytes1 = checkResize(bytes1, size);
            bytes1[size++] = byteMsgOp[1];
            bytes1 = checkResize(bytes1, size);
            if (op.equals("10") && msgOp.equals("4")){
                message = message.substring(message.indexOf(" ")+1);
                byte[] str = message.getBytes(StandardCharsets.UTF_8);
                for (byte b : str){
                    bytes1[size++] = b;
                    bytes1 = checkResize(bytes1, size);}
                bytes1[size++] = (byte) '\0';
                bytes1 = checkResize(bytes1, size);
            }
            if (op.equals("10") && (msgOp.equals("7") || msgOp.equals("8"))){
                for (int i = 0; i<3; i++){
                    message = message.substring(message.indexOf(" ")+1);
                    String temp = message.substring(0, message.indexOf(" ")); //getting msg op code
                    Short shortTemp = Short.valueOf(temp);
                    byte[] byteTemp = shortToBytes(shortTemp);
                    bytes1[size++] = byteTemp[0];
                    bytes1 = checkResize(bytes1, size);
                    bytes1[size++] = byteTemp[1];
                    bytes1 = checkResize(bytes1, size);
                }
                message = message.substring(message.indexOf(" ")+1);
                Short shortTemp = Short.valueOf(message);
                byte[] byteTemp = shortToBytes(shortTemp);
                bytes1[size++] = byteTemp[0];
                bytes1 = checkResize(bytes1, size);
                bytes1[size++] = byteTemp[1];
                bytes1 = checkResize(bytes1, size);
            }
        }
        if (op.equals("9")){
            String notType = message.substring(0, message.indexOf(" "));
            byte[] notByte = notType.getBytes(StandardCharsets.UTF_8);
            for (byte b : notByte) {
                bytes1[size++] = b;
            }
            bytes1 = checkResize(bytes1, size);
            message = message.substring(message.indexOf(" ")+1);
            String poster = message.substring(0, message.indexOf(" "));
            byte[] str = poster.getBytes(StandardCharsets.UTF_8);
            for (byte b : str){
                bytes1[size++] = b;
                bytes1 = checkResize(bytes1, size);}
            bytes1[size++] = (byte) '\0';
            bytes1 = checkResize(bytes1, size);
            message = message.substring(message.indexOf(" ")+1);
            byte[] content = message.getBytes(StandardCharsets.UTF_8);
            for (byte b : content){
                bytes1[size++] = b;
                bytes1 = checkResize(bytes1, size);}
            bytes1[size++] = (byte) '\0';
            bytes1 = checkResize(bytes1, size);
        }

        byte[] ans = new byte[size];
        for (int i = 0; i< size; i++){
            ans[i] = bytes1[i];
        }
        return ans;
    }


    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private byte[] checkResize(byte[] bytes1, int size){
        if (size >= bytes1.length) {
            bytes1 = Arrays.copyOf(bytes1, size * 2);
        }
        return bytes1;
    }

    private String popString() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        String add = new String(bytes, start, len-start, StandardCharsets.UTF_8);
        return add;
    }

    public short bytesToShort(byte[] byteArr)
    {
        short num = (short)((byteArr[0] & 0xff) << 8);
        num += (short)(byteArr[1] & 0xff);
        return num;
    }

    public byte[] shortToBytes(short num)
    {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }
}
