package com.fragtest.android.pa.InputProfile;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fragtest.android.pa.AudioRecorder;
import com.fragtest.android.pa.ControlService;
import com.fragtest.android.pa.Core.AudioFileIO;
import com.fragtest.android.pa.Core.LogIHAB;


public class InputProfile_A2DP implements InputProfile {

    private BluetoothAdapter mBluetoothAdapter;
    private ControlService mContext;
    private AudioRecorder mAudioRecorder;
    private Messenger mServiceMessenger;
    private String LOG = "InputProfile_A2DP";
    private SharedPreferences mSharedPreferences;
    private Handler mTaskHandler = new Handler();
    private int mFindInterval = 100;
    private boolean mIsBound = false;
    private Runnable mAudioReleaseRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAudioRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                mAudioRecorder.release();
                mAudioRecorder = null;
                mTaskHandler.removeCallbacks(mAudioReleaseRunnable);
            } else {
                mTaskHandler.postDelayed(mAudioReleaseRunnable, mFindInterval);
            }
        }
    };
    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {

                // TODO: START RECORDING

                Log.e(LOG, "BT connected.");
                LogIHAB.log("Bluetooth: connected");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {

                stopRecording();
                Log.e(LOG, "BT disconnected.");
                LogIHAB.log("Bluetooth: disconnected");
            }
        }
    };
    private Runnable mFindDeviceRunnable = new Runnable() {
        @Override
        public void run() {

            if (mIsBound && !ControlService.getIsCharging()) {
                Log.e(LOG, "Looking for device.");

                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

                AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
                boolean found = false;
                for (AudioDeviceInfo device : devices) {
                    Log.e(LOG, "Device");
                    if (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP && device.isSource()) {
                        mAudioRecorder.setPreferredDevice(device);
                        found = true;
                        Log.e(LOG, "FOUND!, breaking loop.");
                        break;
                    }
                }

                if (found) {
                    Log.e(LOG, "Device found.");
                    startRecording();
                    mTaskHandler.removeCallbacks(mFindDeviceRunnable);
                } else {
                    Log.e(LOG, "Device not found. Trying again soon.");
                    mTaskHandler.postDelayed(mFindDeviceRunnable, mFindInterval);
                }
            } else {
                Log.e(LOG, "Client not yet bound. Retrying soon.");
                mTaskHandler.postDelayed(mFindDeviceRunnable, mFindInterval);
            }
        }
    };
    private Runnable mSetInterfaceRunnable = new Runnable() {
        @Override
        public void run() {
            if (!ControlService.getIsCharging()) {
                setInterface();
                mTaskHandler.removeCallbacks(mSetInterfaceRunnable);
            } else {
                mTaskHandler.postDelayed(mSetInterfaceRunnable, mFindInterval);
            }
        }
    };

    public InputProfile_A2DP(ControlService context, Messenger serviceMessenger) {
        this.mContext = context;
        this.mServiceMessenger = serviceMessenger;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        // Register broadcasts receiver for bluetooth state change
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(mBluetoothStateReceiver, filter);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();

            /*if (mSharedPreferences.contains("BTDevice")) {
                String btdevice = mSharedPreferences.getString("BTDevice", null);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(btdevice);
                device.createBond();
                LogIHAB.log("Connecting to device: " + device.getAddress());
            }*/
        }

    }

    @Override
    public void setInterface() {

        if (!ControlService.getIsCharging()) {
            Log.e(LOG, "Setting interface");
            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);

            String chunklengthInS = mSharedPreferences.getString("chunklengthInS", "60");
            String samplerate = mSharedPreferences.getString("samplerate", "48000");
            boolean isWave = mSharedPreferences.getBoolean("isWave", true);

            if (mAudioRecorder == null) {
                mAudioRecorder = new AudioRecorder(
                        mContext,
                        mServiceMessenger,
                        Integer.parseInt(chunklengthInS),
                        Integer.parseInt(samplerate),
                        isWave);
            }

            mTaskHandler.postDelayed(mFindDeviceRunnable, mFindInterval);
        }
    }

    @Override
    public void cleanUp() {
        mTaskHandler.removeCallbacks(mFindDeviceRunnable);
        mTaskHandler.removeCallbacks(mSetInterfaceRunnable);
        mContext.unregisterReceiver(mBluetoothStateReceiver);
        Log.e(LOG, "audiorecorder:" + mAudioRecorder);
        if (mAudioRecorder != null) {
            stopRecording();
        }
        mBluetoothAdapter = null;
    }

    @Override
    public void registerClient() {
        Log.e(LOG, "Client Registered");
        mIsBound = true;
    }

    @Override
    public void unregisterClient() {
        Log.e(LOG, "Client Unregistered");
        mIsBound = false;
    }

    @Override
    public void batteryCritical() {
        cleanUp();
    }

    @Override
    public void chargingOff() {
        Log.e(LOG, "CharginOff");
        //setInterface();
        mTaskHandler.postDelayed(mSetInterfaceRunnable, mFindInterval);
    }

    @Override
    public void chargingOn() {
        Log.e(LOG, "CharginOn");
        stopRecording();
    }

    @Override
    public void chargingOnPre() {
        Log.e(LOG, "CharginOnPre");
    }

    @Override
    public void onDestroy() {

        cleanUp();

    }


    @Override
    public void applicationShutdown() {

    }

    private void startRecording() {
        Log.d(LOG, "Start caching audio");
        LogIHAB.log("Start caching audio");
        AudioFileIO.setChunkId(mContext.getChunkId());
        if (!ControlService.getIsRecording()) {
            mAudioRecorder.start();
        }
    }

    private void stopRecording() {

        if (ControlService.getIsRecording()) {
            Log.d(LOG, "Requesting stop caching audio");
            LogIHAB.log("Requesting stop caching audio");

            mAudioRecorder.stop();
            mTaskHandler.post(mAudioReleaseRunnable);
        }
    }

}
