package com.fragtest.android.pa;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.fragtest.android.pa.Questionnaire.QuestionnairePagerAdapter;
import com.fragtest.android.pa.Test.TestIO;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.util.ArrayList;

import static android.R.color.darker_gray;
import static android.R.color.holo_green_dark;


public class MainActivity extends AppCompatActivity {

    static final String LOG = "MainActivity";

    public ViewPager mViewPager = null;
    public TextView mLogo;
    public View mRecord, mArrowBack, mArrowForward, mRevert, mProgress, mRegress, mConfig;
    private QuestionnairePagerAdapter mAdapter;
    private boolean mServiceIsBound;
    private boolean mServiceIsRecording;
    private boolean showPreferences = true;
    private Messenger mServiceMessenger;
    final Messenger mMessageHandler = new Messenger(new MessageHandler());

    private Window mWindow;


    class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            if (BuildConfig.DEBUG) {
                Log.d(LOG, "Message received: " + msg.what);
            }

            switch (msg.what) {

                case ControlService.MSG_START_COUNTDOWN:
                    Log.e(LOG,"Trying to set FCD");
                    int finalCountDown = msg.getData().getInt("finalCountDown");
                    int countDownInterval = msg.getData().getInt("countDownInterval");
                    mAdapter.setFinalCountDown(finalCountDown, countDownInterval);
                    mAdapter.startCountDown();
                    break;

                case ControlService.MSG_START_QUESTIONNAIRE:
                    Bundle dataQuest = msg.getData();
                    ArrayList<String> questionList = dataQuest.getStringArrayList("questionList");
                    String head = dataQuest.getString("head");
                    String motivation = dataQuest.getString("motivation");
                    mAdapter.createQuestionnaire(questionList, head, motivation);
                    break;

                case ControlService.MSG_PROPOSE_QUESTIONNAIRE:
                    mAdapter.proposeQuestionnaire();
                    break;

                case ControlService.MSG_GET_STATUS:
                    // Set UI to match ControlService's state
                    Bundle status = msg.getData();
                    mServiceIsRecording = status.getBoolean("isRecording");

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


    @Override
    public void onBackPressed() {
        Log.i(LOG,"You like pressed the back button but i like don't like wanna do it...");
    }


    // Send message to connected client
    public void messageService(int what) {

        if (mServiceMessenger != null) {
            try {
                Message msg = Message.obtain(null, what);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(LOG,"OnCreate");

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mLogo = (TextView) findViewById(R.id.Action_Logo);
        mRecord = findViewById(R.id.Action_Record);
        mArrowBack = findViewById(R.id.Action_Back);
        mArrowForward = findViewById(R.id.Action_Forward);
        mRevert = findViewById(R.id.Action_Revert);
        mProgress = findViewById(R.id.progress);
        mRegress = findViewById(R.id.regress);
        mConfig = findViewById(R.id.Action_Config);

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

        if (showPreferences) {
            mConfig.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                }
            });
        }

        handleNewPagerAdapter();
        doBindService();

        mAdapter.createMenu();

        mWindow = this.getWindow();
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        new TestIO();
    }

    @Override
    protected void onDestroy() {
        Log.e(LOG,"onDestroy");
        super.onDestroy();
        doUnbindService();
    }

    @Override
    protected void onStart() {
        Log.e(LOG,"onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.e(LOG,"onRestart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.e(LOG,"onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.e(LOG,"onPause");
        mAdapter.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.e(LOG,"onResume");
        mAdapter.onResume();
        super.onResume();
    }

}
