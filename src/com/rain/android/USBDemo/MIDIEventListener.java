package com.rain.android.USBDemo;

/**
 * Created with IntelliJ IDEA.
 * User: dustin
 * Date: 10/21/13
 * Time: 11:24 PM
 */
public interface MIDIEventListener {
    /**
     * Note-off
     * Code Index Number : 0x8
     *
     * @param cable    0-15
     * @param channel  0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    void onMidiNoteOff(int cable, int channel, int note, int velocity);

    /**
     * Note-on
     * Code Index Number : 0x9
     *
     * @param cable    0-15
     * @param channel  0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    void onMidiNoteOn(int cable, int channel, int note, int velocity);
}
