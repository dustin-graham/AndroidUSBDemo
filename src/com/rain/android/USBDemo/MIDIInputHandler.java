package com.rain.android.USBDemo;

import android.os.Handler;
import android.os.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
* Created with IntelliJ IDEA.
* User: dustin
* Date: 10/21/13
* Time: 11:25 PM
*/
class MIDIInputHandler extends Handler {

    private ByteArrayOutputStream received;
    private MIDIEventListener mListener;

    MIDIInputHandler(MIDIEventListener listener) {
        mListener = listener;
    }

    @Override
    public void handleMessage(Message msg) {
        if (received == null) {
            received = new ByteArrayOutputStream();
        }
        try {
            received.write((byte[]) msg.obj);
        } catch (IOException e) {
            // ignore exception
        }
        if (received.size() < 4) {
            // more data needed
            return;
        }

        // USB MIDI data stream: 4 bytes boundary
        byte[] receivedBytes = received.toByteArray();
        byte[] read = new byte[receivedBytes.length / 4 * 4];
        System.arraycopy(receivedBytes, 0, read, 0, read.length);

        // Note: received.reset() method don't reset ByteArrayOutputStream's internal buffer.
        received = new ByteArrayOutputStream();

        // keep unread bytes
        if (receivedBytes.length - read.length > 0) {
            byte[] unread = new byte[receivedBytes.length - read.length];
            System.arraycopy(receivedBytes, read.length, unread, 0, unread.length);
            try {
                received.write(unread);
            } catch (IOException e) {
                // ignore exception
            }
        }

        int cable;
        int codeIndexNumber;
        int byte1;
        int byte2;
        int byte3;
        for (int i = 0; i < read.length; i += 4) {
            cable = (read[i + 0] >> 4) & 0xf;
            codeIndexNumber = read[i + 0] & 0xf;
            byte1 = read[i + 1] & 0xff;
            byte2 = read[i + 2] & 0xff;
            byte3 = read[i + 3] & 0xff;

            switch (codeIndexNumber) {
                case 8:
                    mListener.onMidiNoteOff(cable, byte1 & 0xf, byte2, byte3);
                    break;
                case 9:
                    if (byte3 == 0x00) {
                        mListener.onMidiNoteOff(cable, byte1 & 0xf, byte2, byte3);
                    } else {
                        mListener.onMidiNoteOn(cable, byte1 & 0xf, byte2, byte3);
                    }
                    break;
            }
        }
    }
}
