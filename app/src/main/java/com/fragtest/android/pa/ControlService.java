package com.fragtest.android.pa;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.fragtest.android.pa.AcousticFeatureExtraction.StageManager;
import com.fragtest.android.pa.Core.AudioFileIO;
import com.fragtest.android.pa.Core.EventReceiver;
import com.fragtest.android.pa.Core.EventTimer;
import com.fragtest.android.pa.Core.FileIO;
import com.fragtest.android.pa.Core.SingleMediaScanner;
import com.fragtest.android.pa.Core.Vibration;
import com.fragtest.android.pa.Core.XMLReader;
import com.fragtest.android.pa.Processing.MainProcessingThread;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Set;

/**
 * The brains of the operation.
 *
 * Based on https://developer.android.com/reference/android/app/Service.html
 */

public class ControlService extends Service {

    static final String LOG = "ControlService";

    /**
     * Constants for messaging. Should(!) be self-explanatory.
     */

    static final boolean USE_KIOSK_MODE = true;

    // 1* - general
    public static final int MSG_REGISTER_CLIENT = 11;
    public static final int MSG_UNREGISTER_CLIENT = 12;
    public static final int MSG_GET_STATUS = 13;
    public static final int MSG_SET_VISIBILITY = 14;
    public static final int MSG_NO_QUESTIONNAIRE_FOUND = 15;
    public static final int MSG_NO_TIMER = 16;
    public static final int MSG_CHANGE_PREFERENCE = 17;

    // 2* - alarm
    public static final int MSG_ALARM_RECEIVED = 21;
    public static final int MSG_START_COUNTDOWN = 22;

    // 3* - questionnaire
    public static final int MSG_RESET_MENU = 30;
    public static final int MSG_ISMENU = 31;
    public static final int MSG_QUESTIONNAIRE_INACTIVE = 32;
    public static final int MSG_START_QUESTIONNAIRE = 33;
    public static final int MSG_PROPOSE_QUESTIONNAIRE = 34;
    public static final int MSG_PROPOSITION_ACCEPTED = 35;
    public static final int MSG_MANUAL_QUESTIONNAIRE = 36;
    public static final int MSG_QUESTIONNAIRE_ACTIVE = 37;
    public static final int MSG_CHECK_FOR_PREFERENCES = 38;
    public static final int MSG_PREFS_IN_FOREGROUND = 39;

    // 4* - recording
    public static final int MSG_START_RECORDING = 41;
    public static final int MSG_STOP_RECORDING = 42;
    public static final int MSG_RECORDING_STOPPED = 43;
    public static final int MSG_CHUNK_RECORDED = 44;

    // 5* - processing
    public static final int MSG_CHUNK_PROCESSED = 51;

    // Shows whether questionnaire is active - tackles lifecycle jazz
    private boolean isActiveQuestionnaire = false;
    private boolean isTimerRunning = false;
    private boolean isQuestionnairePending = false;
    private boolean isQuestionnairePresent = false;
    private boolean isMenu = true;
    private XMLReader mXmlReader;
    private Vibration mVibration;
    private String mSelectQuestionnaire, mTempQuestionnaire;

    public static final String FILENAME_LOG = "log.txt";

    // preferences
    private boolean isTimer, isWave, keepAudioCache, filterHp, downsample,
            showConfigButton, showRecordingButton, questionnaireHasTimer;

    private int mFinalCountDown, mTimerInterval;

    private String samplerate, chunklengthInS, filterHpFrequency;

    private boolean restartActivity = false; // TODO: implement in settings
    private NotificationManager mNotificationManager;

    private SharedPreferences sharedPreferences;
    // Questionnaire-Timer
    EventTimer mEventTimer;

    private FileIO mFileIO;

    // Messenger to clients
    private Messenger mClientMessenger;
    // Messenger to pass to threads
    final Messenger serviceMessenger = new Messenger(new MessageHandler());

    // Audio recording
    private AudioRecorder audioRecorder;
    private StageManager stageManager;

    // Recording queue to transfer data to the processing thread
    private Queue<Message> recordingQueue = new ConcurrentLinkedQueue<>();

    // ID to access our notification
    private int NOTIFICATION_ID = 1;

    // recording/processing buffer
    int idxRecording = 0;
    int idxProcessing = 0;
    int processingBufferSize = 100;
    String[] processingBuffer = new String[processingBufferSize];

    // thread-safety
    static boolean isRecording = false;
    static boolean isProcessing = false;
    static final Object recordingLock = new Object();
    static final Object processingLock = new Object();

    Context context = this;



    private EventReceiver mMessageReceiver = new EventReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            messageClient(MSG_ALARM_RECEIVED);
            // perform checks whether running a questionnaire is valid
            if (isMenu) { //!isActiveQuestionnaire
                messageClient(MSG_PROPOSE_QUESTIONNAIRE);
                mVibration.repeatingBurstOn();
            } else {
                // React to when questionnaire is active but another one is due
                // -> probably deprecated because timer is only started after Q was finished
                Log.i(LOG,"Waiting for new questionnaire.");
            }
            isTimerRunning = false;
            isQuestionnairePending = true;

        }
    };


    class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(LOG, "Received Message: " + msg.what);

            switch (msg.what) {

                case MSG_REGISTER_CLIENT:

                    Log.e(LOG,"msg: "+msg);
                    Log.i(LOG, "Client registered to service");
                    Logger.info("Client registered to service");
                    mClientMessenger = msg.replyTo;

                    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

                    setupApplication();

                    // Bundled information about visible contents
                    Bundle bundleShow = new Bundle();
                    bundleShow.putBoolean("showConfigButton", showConfigButton);
                    bundleShow.putBoolean("showRecordingButton", showRecordingButton);
                    bundleShow.putBoolean("isQuestionnairePresent", isQuestionnairePresent);

                    messageClient(MSG_SET_VISIBILITY, bundleShow);
                    break;

                case MSG_UNREGISTER_CLIENT:
                    mClientMessenger = null;
                    Logger.info("Client unregistered from service");
                    if (restartActivity) {
                        startActivity();
                    }
                    break;

                case MSG_GET_STATUS:
                    Bundle status = new Bundle();
                    status.putBoolean("isRecording", isRecording);
                    messageClient(MSG_GET_STATUS, status);
                    break;

                case MSG_MANUAL_QUESTIONNAIRE:
                    // User has initiated questionnaire manually without/before timer
                    if (!isActiveQuestionnaire) {
                        startQuestionnaire("manual");
                    }
                    break;

                case MSG_PROPOSITION_ACCEPTED:
                    // User has accepted proposition to start a new questionnaire by selecting
                    // "Start Questionnaire" item in User Menu
                    //TODO: perform checks
                    mVibration.repeatingBurstOff();
                    if (!isActiveQuestionnaire) {
                        startQuestionnaire("auto");
                    }
                    break;

                case MSG_ISMENU:
                    isMenu = true;
                    break;

                case MSG_QUESTIONNAIRE_ACTIVE:
                        isMenu = false;
                        isActiveQuestionnaire = true;
                        mEventTimer.stopTimer();
                        isTimerRunning = false;
                        Log.i(LOG, "Questionnaire active");
                    break;

                case MSG_QUESTIONNAIRE_INACTIVE:
                    // In case questionnaires are no longer present, program crashes..
                    // but shouldn't be a problem since it is destructive user interference (DUI).
                    isMenu = true;
                    isActiveQuestionnaire = false;
                    if (isTimer) {
                        setAlarmAndCountdown();
                    } else {
                        messageClient(MSG_NO_TIMER);
                    }
                    break;

                case MSG_CHECK_FOR_PREFERENCES:
                    Bundle prefs = msg.getData();
                    updatePreferences(prefs);
                    checkForPreferences();
                    break;

                case MSG_START_RECORDING:

                    stageManager = new StageManager();

                    Log.d(LOG, "Start caching audio");
                    Logger.info("Start caching audio");

                    // set up recorder
//                    audioRecorder = new AudioRecorder(
//                            serviceMessenger,
//                            recordingQueue,
//                            samplerate);

                    // set up processing
//                    Bundle settings = getPreferences();
//                    MainProcessingThread processingThread =
//                            new MainProcessingThread(serviceMessenger,
//                                    recordingQueue,
//                                    settings);
//
//                    // let's go..
//                    audioRecorder.start();
//                    processingThread.start();

                    stageManager.start();

                    isRecording = true;
                    isProcessing = true;

                    messageClient(MSG_START_RECORDING);
                    break;

                case MSG_STOP_RECORDING:
                    Log.d(LOG, "Requesting stop caching audio");
                    Logger.info("Requesting stop caching audio");
//                    audioRecorder.stop();
                    stageManager.stop();
                    isRecording = false;
                    messageClient(MSG_GET_STATUS);
                    break;

                case MSG_RECORDING_STOPPED:
                    Log.d(LOG, "Stop caching audio");
                    Logger.info("Stop caching audio");
//                    audioRecorder.close();
                    isRecording = false;
                    messageClient(MSG_GET_STATUS);
                    break;

                case MSG_CHUNK_RECORDED:
//                    String filename = msg.getData().getString("filename");
//
//                    addProccessingBuffer(idxRecording, filename);
//                    idxRecording = (idxRecording + 1) % processingBufferSize;
//
//                    if (!getIsProcessing()) {
//                        Bundle settings = getPreferences();
//                        settings.putString("filename", processingBuffer[idxProcessing]);;
//                        MainProcessingThread processingThread =
//                                new MainProcessingThread(serviceMessenger, settings);
//                        setIsProcessing(true);
//                        processingThread.start();
//                    }
//
//                    if (keepAudioCache) {
//                        new SingleMediaScanner(context, new File(filename));
//                    }
//
//                    Log.d(LOG, "New cache: " + filename);
//                    Logger.info("New cache:\t{}", filename);
//                    break;

                case MSG_CHUNK_PROCESSED:

//                    ArrayList<String> featureFiles = msg.getData().
//                            getStringArrayList("featureFiles");
//
//                    if (!keepAudioCache) {
//                        AudioFileIO.deleteFile(processingBuffer[idxProcessing]);
//                    }

//                    for (String file : featureFiles) {
//                        if (file != null) {
//                            new SingleMediaScanner(context, new File(file));
//                        }
//                    }

//                    deleteProccessingBuffer(idxProcessing);
//                    idxProcessing = (idxProcessing + 1) % processingBufferSize;

//                    if (processingBuffer[idxProcessing] != null) {
//                        Bundle settings = getPreferences();
//                        settings.putString("filename", processingBuffer[idxProcessing]);
//                        MainProcessingThread processingThread =
//                                new MainProcessingThread(serviceMessenger, settings);
//                        setIsProcessing(true);
//                        processingThread.start();
//                    } else {
//                        setIsProcessing(false);
//                    }

                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    final Messenger mMessengerHandler = new Messenger(new MessageHandler());

    @Override
    public void onCreate() {

        Log.e(LOG, "onCreate");

        // log file
        Configurator.currentConfig()
                .writer(new FileWriter(FileIO.getFolderPath() + File.separator + FILENAME_LOG, false, true))
                .level(Level.INFO)
                .formatPattern("{date:yyyy-MM-dd_HH:mm:ss.SSS}\t{message}")
                .activate();

        Logger.info("Service onCreate");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();


        this.registerReceiver(mMessageReceiver, new IntentFilter("AlarmReceived"));


        Log.e(LOG,"ControlService started");
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int StartID) {
        Log.d(LOG, "onStartCommand");
        Logger.info("Service started");
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(LOG,"onConfigurationChanged");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(LOG,"onLowMemory");
    }

    @Override
    public void onDestroy() {
        Log.d(LOG, "onDestroy");
        stopAlarmAndCountdown();
        mNotificationManager.cancel(NOTIFICATION_ID);

        this.unregisterReceiver(mMessageReceiver);

        Toast.makeText(this, "ControlService stopped", Toast.LENGTH_SHORT).show();
        Log.e(LOG,"ControlService stopped");
        Logger.info("Service stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG, "onBind");
        return mMessengerHandler.getBinder();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e(LOG,"onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(LOG,"onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e(LOG,"onTaskRemoved");
        stopAlarmAndCountdown();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        Log.e(LOG,"onDump");
        super.dump(fd, writer, args);
    }

    // Send message to connected client with additional data
    private void messageClient(int what, Bundle data) {

        if (mClientMessenger != null) {
            try {
                Message msg = Message.obtain(null, what);
                msg.setData(data);
                mClientMessenger.send(msg);
            } catch (RemoteException e) {
            }
        } else {
            Log.d(LOG, "mClientMessenger is null.");
        }
    }

    // Send message to connected client
    private void messageClient(int what) {

        if (mClientMessenger != null) {
            try {
                Message msg = Message.obtain(null, what);
                mClientMessenger.send(msg);
            } catch (RemoteException e) {
            }
        } else {
            Log.d(LOG, "mClientMessenger is null.");
        }
    }

    public void startActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void showNotification() {

        // Launch activity when notification is selected
        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Configure notification
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.logo)
                .setTicker(getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.app_name))
                .setContentIntent(intent)
                .build();

        // Post notification to status bar, use startForeground() instead of
        // NotificationManager.notify() to prevent service from being killed
        // by ActivityManager when the Activity gets shut down.
        startForeground(NOTIFICATION_ID, notification);
    }

    private void setupApplication() {
        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        initialiseValues();

        mFileIO = new FileIO();
        isQuestionnairePresent = mFileIO.setupFirstUse(this);

        Log.e(LOG, "Messenger Control S: "+mMessengerHandler);
        mEventTimer = new EventTimer(this, serviceMessenger); // mMessengerHandler
        mVibration = new Vibration(this);

        checkForPreferences();

        // Determine whether to show or hide preferences menu
        showConfigButton = (mFileIO.checkConfigFile());
    }

    // Load preset values from shared preferences, default values from external class InitValues
    private void initialiseValues() {

        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // preferences
        isTimer = sharedPreferences.getBoolean("isTimer", true);
        isWave = sharedPreferences.getBoolean("isWave", false);
        keepAudioCache = sharedPreferences.getBoolean("keepAudioCache", false);
        filterHp = sharedPreferences.getBoolean("filterHp", false);
        downsample = sharedPreferences.getBoolean("downsample", true);
        showRecordingButton = sharedPreferences.getBoolean("showRecordingButton", true);

        // Cave: These are Strings
        filterHpFrequency = sharedPreferences.getString("filterHpFrequency", "100");
        samplerate = sharedPreferences.getString("samplerate", "16000");
        chunklengthInS = sharedPreferences.getString("chunklengthInS", "60");

        mFinalCountDown = InitValues.finalCountDown;
        mTimerInterval = InitValues.timerInterval;
    }

    private void setSinglePreference(String key, boolean value) {
        Bundle data = new Bundle();
        data.putString("type", "boolean");
        data.putString("key", key);
        data.putBoolean(key, value);
        sharedPreferences.edit().putBoolean(key, value).apply();
        messageClient(MSG_CHANGE_PREFERENCE, data);
    }

    private void updatePreferences(Bundle dataPreferences) {
        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Extract preferences from data Bundle
        mSelectQuestionnaire = dataPreferences.getString("whichQuest", mSelectQuestionnaire);

        isWave = dataPreferences.getBoolean("isWave", isWave);
        isTimer = dataPreferences.getBoolean("isTimer", isTimer);
        filterHp = dataPreferences.getBoolean("filterHp", filterHp);
        filterHpFrequency = dataPreferences.getString("filterHpFrequency", "" + filterHpFrequency);
        downsample = dataPreferences.getBoolean("downsample", downsample);
        keepAudioCache = dataPreferences.getBoolean("keepAudioCache", keepAudioCache);

        samplerate = dataPreferences.getString("samplerate", "" + samplerate);
        chunklengthInS = dataPreferences.getString("chunklengthInS", "" + chunklengthInS);

        ArrayList<String> listActiveFeatures = dataPreferences.getStringArrayList("features");
        Set<String> activeFeatures = new HashSet<>();
        activeFeatures.addAll(listActiveFeatures);

        // Editor accumulates new preferences and writes them to shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Boolean
        editor.putBoolean("keepAudioCache", keepAudioCache);
        editor.putBoolean("isWave", isWave);
        editor.putBoolean("isTimer", isTimer);
        editor.putBoolean("filterHp", filterHp);
        editor.putBoolean("downsample", downsample);
        editor.putBoolean("showCofigButton", showConfigButton);
        editor.putBoolean("showRecordingButton", showRecordingButton);
        // String Set
        editor.putStringSet("features", activeFeatures);
        // String
        editor.putString("whichQuest", mSelectQuestionnaire);
        editor.putString("filterHpFrequency", "" + filterHpFrequency);
        editor.putString("samplerate", "" + samplerate);
        editor.putString("chunklengthInS", "" + chunklengthInS);
        // Make changes permanent
        editor.apply();
    }

    private void checkForPreferences() {

        Bundle bundle = getPreferences();
        isTimer = bundle.getBoolean("isTimer", isTimer);

        if (!Objects.equals(mSelectQuestionnaire, mTempQuestionnaire) && !mSelectQuestionnaire.isEmpty()) {

            if (BuildConfig.DEBUG) {
                Log.i(LOG, "New Questionnaire selected.");
            }

            mTempQuestionnaire = mSelectQuestionnaire;
            // Reads new XML file
            renewQuestionnaire();
            isTimerRunning = false;
            questionnaireHasTimer = mXmlReader.getQuestionnaireHasTimer();
        }

        if (isTimer && questionnaireHasTimer) {
            setAlarmAndCountdown();
        } else {
            stopAlarmAndCountdown();
        }
        messageClient(MSG_RESET_MENU);
    }


    private Bundle getPreferences() {

        isQuestionnairePresent = mFileIO.setupFirstUse(this);

        //sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // recording
        samplerate = sharedPreferences.getString("samplerate","16000");
        chunklengthInS = sharedPreferences.getString("chunklengthInS", "60");
        keepAudioCache = sharedPreferences.getBoolean("keepAudioCache", keepAudioCache);
        isWave = sharedPreferences.getBoolean("isWave", isWave);

        // Use automatic timer
        isTimer = sharedPreferences.getBoolean("isTimer", isTimer);

        // Scan file system for questionnaires
        if (isQuestionnairePresent) {
            String[] fileList = mFileIO.scanQuestOptions();

            if (fileList.length == 0) {
                Log.e(LOG, "No Questionnaires available.");
                messageClient(MSG_NO_QUESTIONNAIRE_FOUND);
                isQuestionnairePresent = false;
            } else {
                // Load questionnaire if selected, otherwise load default
                mSelectQuestionnaire = sharedPreferences.getString("whichQuest", fileList[0]);

                if (mTempQuestionnaire == null || mTempQuestionnaire.isEmpty()) {
                    mTempQuestionnaire = "";
                }

                if (mSelectQuestionnaire == null || mSelectQuestionnaire.isEmpty()) {
                    mSelectQuestionnaire = fileList[0];
                    if (BuildConfig.DEBUG) {
                        Log.i(LOG, "Using default questionnaire: " + mSelectQuestionnaire);
                    }
                }
            }
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("whichQuest", "").apply();
            if (BuildConfig.DEBUG) {
                Log.e(LOG, "No Questionnaires available.");
            }
            messageClient(MSG_NO_QUESTIONNAIRE_FOUND);
        }

        // processing
       HashSet<String> activeFeatures =
                (HashSet<String>) sharedPreferences.getStringSet("features", null);

        filterHp = sharedPreferences.getBoolean("filterHp", true);

        Bundle processingSettings = new Bundle();
        processingSettings.putBoolean("isTimer", isTimer);
        processingSettings.putInt("samplerate", Integer.parseInt(samplerate));
        processingSettings.putInt("chunklengthInS", Integer.parseInt(chunklengthInS));
        processingSettings.putBoolean("isWave", isWave);
        processingSettings.putSerializable("features", activeFeatures);
        processingSettings.putBoolean("filterHp", filterHp);
        processingSettings.putInt("filterHpFrequency", Integer.parseInt(filterHpFrequency));
        processingSettings.putBoolean("downsample", downsample);
        processingSettings.putString("whichQuest", mSelectQuestionnaire);

        return processingSettings;
    }

    private void setAlarmAndCountdown() {

        if (isQuestionnairePresent && isTimer) {

            mXmlReader = new XMLReader(this, mSelectQuestionnaire);
            mTimerInterval = mXmlReader.getNewTimerInterval();
            questionnaireHasTimer = mXmlReader.getQuestionnaireHasTimer();

            // Needed for the first run
            if (questionnaireHasTimer) {
                if (!isTimerRunning) {

                    mEventTimer.stopTimer();
                    mVibration.repeatingBurstOff();
                    mEventTimer.setTimer(mTimerInterval);
                    mFinalCountDown = mEventTimer.getFinalCountDown();
                    isTimerRunning = true;
                    if (BuildConfig.DEBUG) {
                        Log.e(LOG, "Timer set to " + mTimerInterval + "s");
                    }
                } else {
                    // Usually when app is restarted
                    if (BuildConfig.DEBUG) {
                        Log.i(LOG, "Timer already set. Reinstating countdown");
                    }
                }
            } else {
                messageClient(MSG_NO_TIMER);
            }

            // Send message to initialise / update timer
            Bundle data = new Bundle();
            data.putInt("finalCountDown", mFinalCountDown);
            data.putInt("countDownInterval", mTimerInterval);
            messageClient(MSG_START_COUNTDOWN, data);
        }
    }

    private void stopAlarmAndCountdown() {

        Log.e(LOG, "Cancelling Alarm.");

        messageClient(MSG_NO_TIMER);
        isTimerRunning = false;
        mEventTimer.stopTimer();
        mVibration.repeatingBurstOff();
    }

    // Load new questionnaire (initiated after quest change in preferences)
    public void renewQuestionnaire() {
        if (isQuestionnairePresent) {
            String[] questList = mFileIO.scanQuestOptions();

            if (mSelectQuestionnaire.isEmpty() && questList.length > 0) {
                mSelectQuestionnaire = questList[0];
            }
            mXmlReader = new XMLReader(this, mSelectQuestionnaire);
        }
    }

    // Starts a new questionnaire, motivation can be {"auto", "manual"}
    private void startQuestionnaire(String motivation) {

        if (isQuestionnairePresent) {
            Bundle data = new Bundle();
            ArrayList<String> questionList = mXmlReader.getQuestionList();
            String head = mXmlReader.getHead();
            String foot = mXmlReader.getFoot();
            String surveyUri = mXmlReader.getSurveyURI();

            data.putStringArrayList("questionList", questionList);
            data.putString("head", head);
            data.putString("foot", foot);
            data.putString("surveyUri", surveyUri);
            data.putString("motivation", "<motivation='" + motivation + "'>");

            messageClient(MSG_START_QUESTIONNAIRE, data);

            Log.i(LOG, "Questionnaire initiated: " + motivation);

            isQuestionnairePending = false;
        }
    }

    /**
     * Thread-safe status variables
     */

    static void setIsRecording(boolean status) {
        synchronized (recordingLock) {
            isRecording = status;
        }
    }

    static boolean getIsRecording() {
        synchronized (recordingLock) {
            return isRecording;
        }
    }

    static void setIsProcessing(boolean status) {
        synchronized (processingLock) {
            isProcessing = status;
        }
    }

    static boolean getIsProcessing() {
        synchronized (processingLock) {
            return isProcessing;
        }
    }

    void addProccessingBuffer(int idx, String filename) {
        synchronized (processingLock) {
            processingBuffer[idx] = filename;
        }
    }

    void deleteProccessingBuffer(int idx) {
        synchronized (processingLock) {
            processingBuffer[idx] = null;
        }
    }

}
