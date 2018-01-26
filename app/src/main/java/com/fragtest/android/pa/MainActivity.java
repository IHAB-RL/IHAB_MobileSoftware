package com.fragtest.android.pa;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.fragtest.android.pa.Questionnaire.QuestionnairePagerAdapter;

import java.util.ArrayList;

import static android.R.color.darker_gray;
import static android.R.color.holo_green_dark;
import static com.fragtest.android.pa.ControlService.MSG_NO_QUESTIONNAIRE_FOUND;


public class MainActivity extends AppCompatActivity {

    static final String LOG = "MainActivity";
    private static final String KEY_QUEST = "whichQuest";
    private static final String KEY_PREFS_IN_FOREGROUND = "prefsInForeGround";
    private static final String KEY_LOCKED = "isLocked";
    public ViewPager mViewPager = null;
    public TextView mLogo;
    public View mRecord, mArrowBack, mArrowForward, mRevert, mProgress, mRegress, mConfig;
    private QuestionnairePagerAdapter mAdapter;
    private boolean mServiceIsBound;
    private boolean isPrefsInForeGround = false;
    private boolean isActivityRunning = false;
    private boolean mServiceIsRecording;
    private boolean showConfigButton = true;
    private boolean showRecordingButton = true;
    private boolean isQuestionnairePresent = false;
    private boolean isTimer = false;
    private Messenger mServiceMessenger;
    final Messenger mMessageHandler = new Messenger(new MessageHandler());




    private final static int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 0;
    private final static int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    private final static int MY_PERMISSIONS_RECEIVE_BOOT_COMPLETED = 2;
    private final static int MY_PERMISSIONS_RECORD_AUDIO = 3;
    private final static int MY_PERMISSIONS_VIBRATE = 4;
    private final static int MY_PERMISSIONS_WAKE_LOCK = 5;
    private final static int MY_PERMISSIONS_DISABLE_KEYGUARD = 6;
    private final static int MY_PERMISSIONS_CAMERA = 7;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceMessenger = new Messenger(service);
            messageService(ControlService.MSG_REGISTER_CLIENT);
            messageService(ControlService.MSG_GET_STATUS);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
        }
    };
    private ViewPager.OnPageChangeListener myOnPageChangeListener =
            new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrollStateChanged(int state) {
                }

                @Override
                public void onPageScrolled(int position,
                                           float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    mAdapter.setQuestionnaireProgressBar(position);
                    mAdapter.setArrows(position);
                }
            };

    // When back button is pressed, questionnaire navigates one page backwards, menu does nothing
    @Override
    public void onBackPressed() {
        if (!mAdapter.isMenu()) {
            if (mViewPager.getCurrentItem() != 0) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            } else {
                // Might be too unsafe for elderly people
                //mAdapter.createMenu();
            }
        }
    }

    // Send message to connected client
    public void messageService(int what) {

        if (BuildConfig.DEBUG) {
            Log.e(LOG, "Sending Message: " + what);
        }

        if (mServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null, what);
                msg.replyTo = mMessageHandler;
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
            }
        }
    }

    // Send message to connected client
    public void messageService(int what, Bundle data) {

        if (BuildConfig.DEBUG) {
            Log.e(LOG, "Sending Message: " + what);
        }

        if (mServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null, what);
                msg.setData(data);
                msg.replyTo = mMessageHandler;
                mServiceMessenger.send(msg);
            } catch (RemoteException e) {
            }
        }
    }

    // Is ControlService already running?
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ControlService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    void doBindService() {
        if (!isServiceRunning()) {
            startService(new Intent(this, ControlService.class));
        }
        bindService(new Intent(this, ControlService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mServiceIsBound = true;
    }

    void doUnbindService() {
        if (mServiceIsBound) {
            messageService(ControlService.MSG_UNREGISTER_CLIENT);
            unbindService(mConnection);
            mServiceIsBound = false;
        }
    }

    public void handleNewPagerAdapter() {

        mViewPager = null;
        mViewPager = (ViewPager) findViewById(R.id.viewpager);
        mAdapter = new QuestionnairePagerAdapter(this, mViewPager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(myOnPageChangeListener);
    }

    /**
     * Lifecycle methods
     **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {





        Log.d(LOG, "Requesting Permissions.");

        //Android 6.0.1+
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_READ_EXTERNAL_STORAGE);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED},
                MY_PERMISSIONS_RECEIVE_BOOT_COMPLETED);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                MY_PERMISSIONS_RECORD_AUDIO);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.VIBRATE},
                MY_PERMISSIONS_VIBRATE);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WAKE_LOCK},
                MY_PERMISSIONS_WAKE_LOCK);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.DISABLE_KEYGUARD},
                MY_PERMISSIONS_DISABLE_KEYGUARD);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                MY_PERMISSIONS_CAMERA);









        if (BuildConfig.DEBUG) {
            Log.e(LOG, "OnCreate");
        }

        if (!isActivityRunning) {
            super.onCreate(savedInstanceState);

            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.activity_main);

            mLogo = (TextView) findViewById(R.id.Action_Logo);
            mRecord = findViewById(R.id.Action_Record);
            mArrowBack = findViewById(R.id.Action_Back);
            mArrowForward = findViewById(R.id.Action_Forward);
            mRevert = findViewById(R.id.Action_Revert);
            mProgress = findViewById(R.id.progress);
            mRegress = findViewById(R.id.regress);
            mConfig = findViewById(R.id.Action_Config);


            //mConfig.setVisibility(View.INVISIBLE);

            mRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mServiceIsBound) {
                        if (mServiceIsRecording) {
                            messageService(ControlService.MSG_STOP_RECORDING);
                        } else {
                            messageService(ControlService.MSG_START_RECORDING);
                        }
                        messageService(ControlService.MSG_GET_STATUS);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Not connected to service.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });

            if (showConfigButton) {
                mConfig.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e(LOG, "CLICK");
                        startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                        isPrefsInForeGround = true;
                        mAdapter.setPrefsInForeGround(isPrefsInForeGround);
                    }
                });
            }

            handleNewPagerAdapter();
            doBindService();

            mAdapter.createMenu();

            //mWindow = this.getWindow();
            //mWindow.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            //mWindow.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            //mWindow.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            mAdapter.onCreate();

            isActivityRunning = true;
        }
    }









    @Override
    protected void onDestroy() {
        if (BuildConfig.DEBUG) {
            Log.e(LOG, "onDestroy");
        }
        super.onDestroy();
        doUnbindService();

    }

    @Override
    protected void onStart() {
        if (BuildConfig.DEBUG) {
            Log.e(LOG, "onStart");
        }
        super.onStart();
        mAdapter.onStart();






/*
        // start lock task mode if it's not already active
        ActivityManager am = (ActivityManager) getSystemService(
                Context.ACTIVITY_SERVICE);
        // ActivityManager.getLockTaskModeState api is not available in pre-M.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (!am.isInLockTaskMode()) {
                startLockTask();
                //setLockTaskPackages();
            }
        } else {
            if (am.getLockTaskModeState() ==
                    ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }
*/





    }







    @Override
    protected void onRestart() {
        if (BuildConfig.DEBUG) {
            Log.e(LOG, "onRestart");
        }
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (BuildConfig.DEBUG) {
            Log.e(LOG, "onStop");
        }
        mAdapter.onStop();
    }

    @Override
    protected void onPause() {
        if (BuildConfig.DEBUG) {
            Log.e(LOG, "onPause");
        }
        mAdapter.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (BuildConfig.DEBUG) {
            Log.e(LOG, "onResume");
        }

        if (isPrefsInForeGround) {
            isPrefsInForeGround = false;
            mAdapter.setPrefsInForeGround(isPrefsInForeGround);

            /*
            SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            String quest = sharedPreferences.getString(KEY_QUEST, "");


            boolean isLocked = sharedPreferences.getBoolean(KEY_LOCKED, false);

            Bundle data = new Bundle();
            data.putBoolean(KEY_LOCKED, isLocked);

            if (!quest.isEmpty()) {
                data.putString(KEY_QUEST, quest);
            }

            messageService(ControlService.MSG_CHECK_FOR_PREFERENCES, data);*/
            messageService(ControlService.MSG_CHECK_FOR_PREFERENCES);
        }
        mAdapter.onResume();
        super.onResume();
    }

    private void setConfigVisibility() {
        if (showConfigButton) {
            mConfig.setVisibility(View.VISIBLE);
        } else {
            mConfig.setVisibility(View.GONE);
        }

        Log.e(LOG, "Config visibility set to: "+showConfigButton);
    }

    private void setRecordingVisibility() {
//        if (showRecordingButton) {
            mRecord.setVisibility(View.VISIBLE);
//        } else {
//            mRecord.setVisibility(View.INVISIBLE);
//        }
//        Log.e(LOG, "Recording visibility set to: "+showRecordingButton);
    }

    class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            if (BuildConfig.DEBUG) {
                Log.d(LOG, "Message received: " + msg.what);
            }

            switch (msg.what) {

                case ControlService.MSG_SET_VISIBILITY:

                    Bundle data = msg.getData();
                    showConfigButton = data.getBoolean("showConfigButton", false);
                    showRecordingButton = data.getBoolean("showRecordingButton", false);
                    isQuestionnairePresent = data.getBoolean("isQuestionnairePresent", false);

                    if (isQuestionnairePresent) {
                        mAdapter.questionnairePresent();
                        mAdapter.displayManualStart();
                    }

                    setConfigVisibility();
                    setRecordingVisibility();
                    break;

                case MSG_NO_QUESTIONNAIRE_FOUND:
                    Log.i(LOG, "NO QUEST FOUND.");
                    mAdapter.noQuestionnaires();
                    showConfigButton = msg.getData().getBoolean("showConfigButton", false);
                    showRecordingButton = msg.getData().getBoolean("showRecordingButton", false);
                    isQuestionnairePresent = msg.getData().getBoolean("isQuestionnairePresent", false);

                    setConfigVisibility();
                    setRecordingVisibility();
                    break;

                case ControlService.MSG_START_COUNTDOWN:
                    Log.e(LOG, "isPrefsInForeGround: "+isPrefsInForeGround);

                    isTimer = true;
                    showConfigButton = msg.getData().getBoolean("showConfigButton", false);
                    showRecordingButton = msg.getData().getBoolean("showRecordingButton", false);
                    isQuestionnairePresent = msg.getData().getBoolean("isQuestionnairePresent", false);

                    setConfigVisibility();
                    setRecordingVisibility();

                    if (isQuestionnairePresent) {
                        Log.e(LOG, "Trying to set FCD");
                        int finalCountDown = msg.getData().getInt("finalCountDown");
                        int countDownInterval = msg.getData().getInt("countDownInterval");
                        if (isTimer) {
                            Log.i(LOG, "here.");
                            mAdapter.setFinalCountDown(finalCountDown, countDownInterval);
                            mAdapter.startCountDown();
                        } else {
                            Log.i(LOG, "here here.");
                            mAdapter.setQuestionnaireProgressBar(100);
                        }
                        mAdapter.questionnairePresent();
                        mAdapter.displayManualStart();
                    } else {
                        Log.i(LOG, "here here here.");
                        mAdapter.noQuestionnaires();
                    }


                    break;

                case ControlService.MSG_START_QUESTIONNAIRE:
                    Bundle dataQuest = msg.getData();
                    ArrayList<String> questionList = dataQuest.getStringArrayList("questionList");
                    String head = dataQuest.getString("head");
                    String foot = dataQuest.getString("foot");
                    String surveyUri = dataQuest.getString("surveyUri");
                    String motivation = dataQuest.getString("motivation");
                    mAdapter.createQuestionnaire(questionList, head, foot, surveyUri, motivation);
                    break;

                case ControlService.MSG_PROPOSE_QUESTIONNAIRE:

                    mAdapter.proposeQuestionnaire();
                    break;

                case ControlService.MSG_GET_STATUS:
                    // Set UI to match ControlService's state
                    Bundle status = msg.getData();
                    mServiceIsRecording = status.getBoolean("isRecording");
                    isQuestionnairePresent = status.getBoolean("isQuestionnairePresent");
                    showConfigButton = status.getBoolean("showConfigButton");
                    showRecordingButton = status.getBoolean("showRecordingButton");
                    isTimer = status.getBoolean("isTimer");

                    if (!isTimer) {
                        mAdapter.setQuestionnaireProgressBar(100);
                    } else {

                    }

                    if (isQuestionnairePresent) {
                        mAdapter.questionnairePresent();
                        mAdapter.displayManualStart();
                    } else {
                        mAdapter.noQuestionnaires();
                    }

                    setConfigVisibility();
                    setRecordingVisibility();

                    if (status.getBoolean("isQuestionnairePending", false)) {
                        mAdapter.proposeQuestionnaire();
                    }

                    Log.d(LOG, "recording state: " + mServiceIsRecording);

                    if (mServiceIsRecording) {
                        mRecord.setBackgroundTintList(
                                ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                                        holo_green_dark, null)));
                    } else {
                        mRecord.setBackgroundTintList(
                                ColorStateList.valueOf(ResourcesCompat.getColor(getResources(),
                                        darker_gray, null)));
                    }

                    break;

                case ControlService.MSG_CHECK_FOR_PREFERENCES:
                    messageService(msg.what, msg.getData());
                    break;

                case ControlService.MSG_RESET_MENU:
                    mAdapter.resetMenu();

                    showConfigButton = msg.getData().getBoolean("showConfigButton");
                    showRecordingButton = msg.getData().getBoolean("showRecordingButton");
                    isQuestionnairePresent = msg.getData().getBoolean("isQuestionnairePresent");
                    if (!isQuestionnairePresent) {
                        mAdapter.noQuestionnaires();
                        Log.e(LOG, "Received: no questionnaires");
                    }

                    setConfigVisibility();
                    setRecordingVisibility();
                    break;

                case ControlService.MSG_PREFS_IN_FOREGROUND:
                    isPrefsInForeGround = msg.getData().getBoolean(KEY_PREFS_IN_FOREGROUND);
                    Log.e(LOG, "Foreground: "+isPrefsInForeGround);
                    break;

                case ControlService.MSG_START_RECORDING:
                    break;

                case ControlService.MSG_STOP_RECORDING:
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

}
