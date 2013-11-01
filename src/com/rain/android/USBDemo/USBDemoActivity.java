package com.rain.android.USBDemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import java.util.*;

public class USBDemoActivity extends Activity implements Runnable, MIDIEventListener {

    private byte[] readBuffer = new byte[64];

    private Handler receiveHandler = new MIDIInputHandler(this);

    private static final String TAG = "USBDemoActivity";
    private UsbManager mUsbManager;
    private UsbDevice mDevice;
    private UsbDeviceConnection mConnection;
    private UsbEndpoint mEndpointIntr;

    private TextView mNoteTextView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mNoteTextView = (TextView)findViewById(R.id.current_note_label);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();

        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            setDevice(device);
        } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            if (mDevice != null && mDevice.equals(device)) {
                setDevice(null);
            }
        }
    }

    private void setDevice(UsbDevice device) {
        Log.d(TAG, "setDevice " + device);
        UsbInterface intf = device.getInterface(1);//inbound midi interface is first
        // endpoint should be of type interrupt
        UsbEndpoint ep = intf.getEndpoint(0);//only has one endpoint
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
            Log.e(TAG, "endpoint is not bulk type");
            return;
        }
        mDevice = device;
        mEndpointIntr = ep;
        if (device != null) {
            UsbDeviceConnection connection = mUsbManager.openDevice(device);
            if (connection != null && connection.claimInterface(intf, true)) {
                Log.d(TAG, "open SUCCESS");
                mConnection = connection;
                Thread thread = new Thread(this);
                thread.start();

            } else {
                Log.d(TAG, "open FAIL");
                mConnection = null;
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            if (mEndpointIntr == null) {
                continue;
            }
            if (mConnection == null) {
                continue;
            }

            int length = mConnection.bulkTransfer(mEndpointIntr, readBuffer, readBuffer.length, 0);
            if (length > 0) {
                byte[] read = new byte[length];
                System.arraycopy(readBuffer, 0, read, 0, length);
                Log.d(TAG, "Input:" + Arrays.toString(read));

                Message message = new Message();
                message.obj = read;

                receiveHandler.sendMessage(message);
            }
        }
    }

    private Set<Integer> mCurrentNotes = new HashSet<Integer>();

    /**
     * Note-off
     * Code Index Number : 0x8
     *
     * @param cable    0-15
     * @param channel  0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    public void onMidiNoteOff(int cable, int channel, int note, int velocity) {
        if (mCurrentNotes.contains(note)) {
            mCurrentNotes.remove(note);
            updateNotesLabel(getNoteListString(mCurrentNotes));
        }
    }

    /**
     * Note-on
     * Code Index Number : 0x9
     *
     * @param cable    0-15
     * @param channel  0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    public void onMidiNoteOn(int cable, int channel, int note, int velocity) {
        mCurrentNotes.add(note);
        updateNotesLabel(getNoteListString(mCurrentNotes));
    }

    private void updateNotesLabel(String noteListString) {
        mNoteTextView.setText(noteListString);
    }

    private String getNoteListString(Set<Integer> notes) {
        StringBuilder result = new StringBuilder();
        for(Integer note : notes) {
            result.append(note);
            result.append(",");
        }
        return result.length() > 0 ? result.substring(0, result.length() - 1): "";
    }

}
