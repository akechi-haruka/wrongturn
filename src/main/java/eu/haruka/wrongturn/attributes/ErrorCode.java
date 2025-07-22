package eu.haruka.wrongturn.attributes;

import eu.haruka.wrongturn.TurnPacket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ErrorCode extends TurnAttribute {

    public static final int TRY_ALTERNATE = 300;
    public static final int ERROR_BAD_REQUEST = 400;
    public static final int ERROR_UNAUTHORIZED = 401;
    public static final int ERROR_UNKNOWN_ATTRIBUTE = 420;
    public static final int ERROR_STALE_CREDENTIALS = 430;
    public static final int ERROR_INTEGRITY_CHECK_FAILURE = 431;
    public static final int ERROR_MISSING_USERNAME = 432;
    public static final int ERROR_USE_TLS = 433;
    public static final int ERROR_STALE_NONCE = 438;
    public static final int ERROR_SERVER_ERROR = 500;
    public static final int ERROR_GLOBAL_ERROR = 600;
    public static final int ERROR_UNSUPPORTED_TRANSPORT_PROTOCOL = 442;
    public static final int ERROR_ALLOCATION_MISMATCH = 437;
    public static final int ERROR_ALLOCATION_QUOTA_REACHED = 486;
    public static final int ERROR_INSUFFICIENT_CAPACITY = 508;

    private int code;
    private String message;

    public ErrorCode() {
    }

    public ErrorCode(int errorCode, String message) {
        this.code = errorCode;
        this.message = message;
    }

    @Override
    public void read(TurnPacket turnPacket, DataInputStream is, short len) throws IOException {
        is.readShort(); // padding
        byte clazz = is.readByte();
        byte number = is.readByte();
        code = clazz * 100 + number;
        if (code < 100 || code > 699) {
            throw new IOException("Invalid error code: " + code);
        }
        message = readNullTerminatedString(is, len - 2 - 1 - 1);
        skipUntilBoundary(is, len, 4);
    }

    @Override
    public void write(TurnPacket turnPacket, DataOutputStream os) throws IOException {
        os.writeShort(0);
        os.writeByte(code / 100);
        os.writeByte(code % 100);
        byte[] str = writeString(message);
        os.write(str);
        padUntilBoundary(os, str.length, 4);
    }

    @Override
    public short getLength() {
        byte[] str = writeString(message);
        return (short) (2 + 1 + 1 + str.length + getPaddingUntilBoundary(str.length, 4));
    }

    public int getErrorCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
