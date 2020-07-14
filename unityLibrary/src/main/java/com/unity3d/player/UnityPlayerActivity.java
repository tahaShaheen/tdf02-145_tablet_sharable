package com.unity3d.player;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.Toast;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * @file UnityPlayerActivity.java
 * @brief The main activity
 * @details The first screen to appear when the user launches the app
 */

/**
 * @brief This is where everything important happens.
 * @details This is the first screen to appear when the user launches the app. This handles everything and calls everything.
 */
public class UnityPlayerActivity extends Activity {

    /**
     * @brief Debugging tool
     * @details
     * @li Toggle to FALSE when using it with the two HC-05's on a breadboard. Check their MAC Addresses before using.
     * @li FALSE implies controlling the robot body
     */
    private final boolean TESTING = false;

    /**
     * @brief Universally Unique Identifier (UUID)
     * @details
     * @li Creating a UUID which represents a 128-bit value.
     * @li More information on UUIDs by the Internet Engineering Task Force can be found <a href="https://www.ietf.org/rfc/rfc4122.txt">here</a>.
     * @li UUIDs are not tied to particular devices. They identify software services. You just need both sides to use the same one.
     * @li "00001101-0000-1000-8000-00805F9B34FB" is the one and only UUID for SPP (serial port profile). Check out <a href="https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#createRfcommSocketToServiceRecord(java.util.UUID)">the Android Developer's page</a>.
     */
    static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * @brief Handles Bluetooth stuff
     * @details Custom class. Handles Bluetooth stuff.
     */
    public static MyBluetoothService myBluetoothService;

    /**
     * Debugging tool
     */
    private static String TAG = "DEBUG_UNITY_PLAYER";

    private final int HAPPY = 0, SAD = 1, SURPRISED = 2, ANGRY = 3, IDLE = 4;

    /**
     * @brief a BroadcastReceiver type object
     * @details
     * @li Receives and handles broadcast intents sent by <a href="https://developer.android.com/reference/android/content/Context.html#sendBroadcast(android.content.Intent)">Context.sendBroadcast(Intent)</a>.
     * @li This is an implementation of BroadcastReceiver registered to be run in the main activity thread. Its receiver is called with any broadcast Intent that matches filter, in this case is <b>BluetoothAdapter.ACTION_STATE_CHANGED</b> (in other words, when the state of the local Bluetooth adapter has been changed).
     */
    private final BroadcastReceiver mBTStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Lets us know through Toast messages if BT status changes //
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(context, R.string.BT_STATE_OFF_TEXT, Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Toast.makeText(context, R.string.BT_STATE_TURNING_OFF_TEXT, Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(context, R.string.BT_STATE_ON_TEXT, Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Toast.makeText(context, R.string.BT_STATE_TURNING_ON_TEXT, Toast.LENGTH_SHORT).show();
                        break;
                }

            }
        }
    };

    /**
     * Referenced from native coded
     * Don't change the name of this variable
     */
    protected UnityPlayer mUnityPlayer;

    /**
     * @brief Media Player for running audio files that "voice emotion"
     * @details A MediaPlayer class object can be used to control playback of audio/video files and streams
     */
    MediaPlayer voiceYourEmotion;

    /**
     * MAC ADDRESS of the Bluetooth device to establish communication with
     */
    private String MAC_ADDRESS;

    /**
     * @var mainCameraObject
     * @brief References Unity object
     * @details A String that represents the Unity object "Main Camera"
     * 
     * @var eyebrowsObject
     * @brief References Unity object
     * @details A String that represents the Unity object "eyebrows"
     *
     * @var mouthObject
     * @brief References Unity object
     * @details A String that represents the Unity object "mouth"
     *
     * @var tearObject
     * @brief References Unity object
     * @details A String that represents the Unity object "tear"
     *
     * @var ouchZoneObject
     * @brief References Unity object
     * @details A String that represents the Unity object "ouch_zone"
     *
     * @var eyelidsObject
     * @brief References Unity object
     * @details A String that represents the Unity object "eyelids"
     *
     * @var changeBackgroundColorFunction
     * @brief Represents Unity object
     * @details A String that Represents the Unity function "ChangeBackgroundColor"
     *
     * @var setEmotionFunction
     * @brief Represents Unity object
     * @details A String that represents the Unity function "SetEmotion"
     *
     * @var setSpeakingFunction
     * @brief Represents Unity object
     * @details A String that represents the Unity function "SetSpeaking"
     *
     * @var setEyePokeEnabledStateFunction
     * @brief Represents Unity object
     * @details A String that represents the Unity function "SetEyePokeEnabledState"
     *
     * @var goToSleepFunction
     * @brief Represents Unity object
     * @details A String that represents the Unity function "GoToSleep"
     *
     * @var startSpeaking
     * @brief Unity parameter
     * @details A String that holds the parameter "START". This is passed to a Unity function to start speaking animation.
     *
     * @var stopSpeaking
     * @brief Unity parameter
     * @details A String that holds the parameter "STOP". This is passed to a Unity function to stop the speaking animation.
     *
     * @var trueString
     * @brief Unity parameter
     * @details A String that holds the parameter "TRUE". This is passed to a Unity function to disable the poking animation.
     *
     * @var falseString
     * @brief @details A String that holds the parameter "FALSE". This is passed to a Unity function to enable the animation.
     *
     */
    private String mainCameraObject, eyebrowsObject, mouthObject, tearObject, ouchZoneObject, eyelidsObject;
    private String changeBackgroundColorFunction, setEmotionFunction, setSpeakingFunction, setEyePokeEnabledStateFunction, goToSleepFunction;
    private String startSpeaking, stopSpeaking;
    private String trueString, falseString;

    /**
     * @brief For text to speech functionality
     * @details
     * @li A TextToSpeech object synthesizes speech from text for immediate playback or to create a sound file.
     * @li This allows the robot to speak
     */
    private TextToSpeech textToSpeech;

    /**
     * @brief A BluetoothAdapter object
     * @details A BluetoothAdapter lets you perform fundamental Bluetooth tasks, such as initiate device discovery, query a list of bonded (paired) devices, instantiate a BluetoothDevice using a known MAC address, and create a BluetoothServerSocket to listen for connection requests from other devices, and start a scan for Bluetooth LE devices.
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * An integer passed to startActivityForResult() and received by onActivityResult(). If it is greater >= 0, this code will be returned in onActivityResult() when the activity exits. Does nothing of significance at present.
     */
    private int REQUEST_ENABLE_BT = 1;

    /**
     * @brief Manages the volume when the app is running
     * @details An AudioManager object provides access to volume and ringer mode control
     */
    private AudioManager audioManager;

    /**
     * @brief Saves the current volume setting of the device
     * @details After the app is closed,this allows for the volume to go back to what it was before the app started
     */
    private int savedVolume;

    /**
     * @brief Public interface for managing policies enforced on a device
     * @details Most clients of this class must be registered with the system as a device administrator
     * @note I'm not sure how this works
     */
    private DevicePolicyManager mDevicePolicyManager;

    /**
     * An integer passed to startActivityForResult() and received by onActivityResult(). If it is greater >= 0, this code will be returned in onActivityResult() when the activity exits. Does nothing of significance at present.
     */
    private static final int ADMIN_INTENT = 15;

    /**
     * Identifier for a specific application component (Activity, Service, BroadcastReceiver, or ContentProvider) that is available.
     */
    private ComponentName mComponentName;

    /**
     * Used to schedule messages and runnables to be executed at some point in the future
     */
    final Handler handler = new Handler();

    /**
     * @param savedInstanceState A Bundle object containing the activity's previously saved state. If the activity has never existed before, the value of the Bundle object is null. (Bundle is generally used for passing data between various activities of android.)
     * @brief fires when the system first creates the activity.
     * @details In the onCreate() method, you perform basic application startup logic that should happen only once for the entire life of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Make this activity, full screen //
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // By calling super.onCreate(savedInstanceState);, you tell the Dalvik VM (an android virtual machine optimized for mobile devices) to run your code in addition to the existing code in the onCreate() of the parent class. If you leave out this line, then only your code is run. The existing code is ignored completely. //
        super.onCreate(savedInstanceState);

        // This bit of code preserves the state of the app. Switching focus back to this app from another app will not lead to a long load time. //
        String cmdLine = updateUnityCommandLineArguments(getIntent().getStringExtra("unity"));
        getIntent().putExtra("unity", cmdLine);


        // Constructor for UnityPlayer View object //
        mUnityPlayer = new UnityPlayer(this);

        // the Activity class takes care of creating a window for you in which you can place your UI with setContentView(View) //
        setContentView(mUnityPlayer);

        // Call this to try to give focus to a specific view //
        mUnityPlayer.requestFocus();


        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Save volume //
        savedVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        // Set volume to max //
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        // String code //
        mainCameraObject = getString(R.string.MAIN_CAMERA_OBJECT);
        eyebrowsObject = getString(R.string.EYEBROWS_OBJECT);
        mouthObject = getString(R.string.MOUTH_OBJECT);
        tearObject = getString(R.string.TEAR_OBJECT);
        ouchZoneObject = getString(R.string.OUCH_ZONE);
        eyelidsObject = getString(R.string.EYELIDS_OBJECT);

        changeBackgroundColorFunction = getString(R.string.CHANGE_BACKGROUND_COLOR_FUNCTION);
        setEmotionFunction = getString(R.string.SET_EMOTION_FUNCTION);
        setSpeakingFunction = getString(R.string.SET_SPEAKING_FUNCTION);
        setEyePokeEnabledStateFunction = getString(R.string.SET_EYE_POKE_ENABLED_STATE_FUNCTION);
        goToSleepFunction = getString(R.string.GO_TO_SLEEP_FUNCTION);

        startSpeaking = getString(R.string.START_SPEAKING);
        stopSpeaking = getString(R.string.STOP_SPEAKING);
        trueString = getString(R.string.TRUE);
        falseString = getString(R.string.FALSE);

        //Bluetooth code //
        if (TESTING)
            MAC_ADDRESS = getString(R.string.TESTING_MAC_ADDRESS_1); // MAC_ADDRESS IN USE - EASIER TO CHANGE HERE THAN ALL OVER THE PLACE //
        else
            MAC_ADDRESS = getString(R.string.MAC_ADDRESS); // MAC_ADDRESS IN USE - EASIER TO CHANGE HERE THAN ALL OVER THE PLACE //

        myBluetoothService = new MyBluetoothService(UnityPlayerActivity.this) {
            @Override
            public void showToast(int resourceID) {
                // Will receive a resourceID, convert it into a String, and send it to showToastMethod() //
                showToastMethod(getString(resourceID));
            }

            @Override
            public void messageProcessorFunction(String[] messagePieces) {
                // Will receive String array, and call the appropriate method for execution //
                Log.d(TAG, messagePieces[0]);
                switch (messagePieces[0].trim()) {
                    case "E":
                        setEmotion(messagePieces[1]);
                        break;
                    case "G":
                        speakOut(messagePieces);
                        break;
                    case "C":
                        settingsUpdate(messagePieces);
                        break;
                }
            }
        };

        // Get a handle/reference to the default local Bluetooth adapter of the device being used //
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
            Toast.makeText(this, "Your device does not support Bluetooth", Toast.LENGTH_LONG).show();
        else {
            if (!bluetoothAdapter.isEnabled()) {

                // Creating an Intent //
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                // A dialog appears requesting user permission to enable Bluetooth. //
                // If the user responds "Yes", the system begins to enable Bluetooth //
                // Focus returns to your application once the process completes (or fails) //
                // onActivityResult() that gets called upon return of focus //
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            } else {
                beginConnection();
            }
            // Informs us when BT condition changes //
            // Registers a BroadcastReceiver to be run in the main activity thread. //
            // The receiver will be called with any broadcast Intent that matches filter //
            // The Broadcast Receiver implementation is outside onCreate() //
            registerReceiver(mBTStateBroadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

            //Text to speech code //
            textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR)
                        textToSpeech.setLanguage(Locale.ENGLISH);

                    // This bit of code handles mouth animation during TTS //
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            UnityPlayer.UnitySendMessage(mouthObject, setSpeakingFunction, startSpeaking);
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            UnityPlayer.UnitySendMessage(mouthObject, setSpeakingFunction, stopSpeaking);
                        }

                        @Override
                        public void onError(String utteranceId) {
                        }
                    });
                }
            });

        }

        // Screen Control code //
        mDevicePolicyManager = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Ask the user to add a new device administrator to the system. //
        mComponentName = new ComponentName(this, MyAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
        startActivityForResult(intent, ADMIN_INTENT);
    }

    /**
     * @brief The final call you receive before your activity is destroyed
     * @details This opportunity is used to unregister the BroadcastReceiver, mBTStateBroadcastReceiver
     */
    @Override
    protected void onDestroy() {
        mUnityPlayer.destroy();
        super.onDestroy();

        // Unregistering broadcast listener to free up resources //
        unregisterReceiver(mBTStateBroadcastReceiver);
        super.onDestroy();

        // Shutting down TextToSpeech //
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        // Release MediaPlayer object //
        if (voiceYourEmotion != null) voiceYourEmotion.release();

        // set volume to value it was at before app started //
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0);

    }

    /**
     * @brief Pause Unity
     * @brief Called when another app is run on the device
     */
    @Override
    protected void onPause() {
        super.onPause();
        mUnityPlayer.pause();
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        //set volume to value it was at before app started //
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0);

        // Stop MediaPlayer object //
        if (voiceYourEmotion != null) {
            UnityPlayer.UnitySendMessage(mouthObject, setSpeakingFunction, stopSpeaking);
            voiceYourEmotion.stop();
        }
    }

    /**
     * @brief Resume Unity
     * @details Called when focus return to this app
     */
    @Override
    protected void onResume() {
        super.onResume();
        mUnityPlayer.resume();

        // Volume to max //
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

    }

    /**
     * When the phone's memory is low, the background processes will be killed by framework.
     * Unity will be informed as well
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    /**
     * Callback for finer-grained memory management
     * @param level different types of clues about memory availability
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level == TRIM_MEMORY_RUNNING_CRITICAL) {
            // The device is running extremely low on memory. //
            // App is not yet considered a killable process, but the system will begin killing background processes if apps do not release resources. //
            // Releasing non-critical resources now to prevent performance degradation. //
            mUnityPlayer.lowMemory();
        }
    }

    /**
     * Notifies Unity of any configuration change
     * This ensures the layout will be correct
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    /**
     * Notifies Unity of the focus change
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    /**
     * @brief Text to speech code
     * @details
     * @li Sets the pitch value
     * @li Sets the speed of speech
     * @li Executes Text To Speech
     * @param messagePieces String instruction received, broken into pieces
     */
    private void speakOut(String[] messagePieces) {

        float pitchValue = Float.parseFloat(messagePieces[2]);
        textToSpeech.setPitch(pitchValue);
        textToSpeech.setSpeechRate(0.7f);

        Log.d(TAG + " pitchValue:", String.valueOf(pitchValue));

        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "texToSpeech");
        textToSpeech.speak(messagePieces[1], TextToSpeech.QUEUE_FLUSH, map);

    }


    /**
     * @brief Emotion control code
     * @details
     * @li The setEmotion() function (the one that sends commands to Unity) takes in integers
     * @li The string received gets converted to an int
     * @li Then that integer is used to work out mouth, eyebrow and other dynamic face properties
     * @param emotionString String containing information on what emotion is to be displayed
     */
    private void setEmotion(String emotionString) {

        // Was an audio also requested //
        boolean voiceYourEmotionInstruction = false;
        if(emotionString.substring(emotionString.length() - 1).equals("V")) {
            voiceYourEmotionInstruction = true;
            emotionString = emotionString.substring(0, emotionString.length() - 1);
        }

        // If audio already playing, stop it //
        if(voiceYourEmotion!=null){
            voiceYourEmotion.stop();
            voiceYourEmotion.release();
        }
        voiceYourEmotion = null;

        // String to int conversion //
        int emotionInteger;
        switch (emotionString.toUpperCase()) {
            case "HAPPY":
                emotionInteger = HAPPY;
                if(voiceYourEmotionInstruction) voiceYourEmotion = MediaPlayer.create(UnityPlayerActivity.this, R.raw.happy);
                break;
            case "SAD":
                emotionInteger = SAD;
                if(voiceYourEmotionInstruction) voiceYourEmotion = MediaPlayer.create(UnityPlayerActivity.this, R.raw.sad);
                break;
            case "ANGRY":
                emotionInteger = ANGRY;
                if(voiceYourEmotionInstruction) voiceYourEmotion = MediaPlayer.create(UnityPlayerActivity.this, R.raw.angry);
                break;
            case "SURPRISED":
                emotionInteger = SURPRISED;
                break;
            case "IDLE":
                emotionInteger = IDLE;
                break;
            default:
                emotionInteger = 10;    //because 10 isn't a registered emotion, nothing happens //
        }

        // This part talks to UNITY. Using the emotionInteger //
        // The format is UnityPlayer.UnitySendMessage(unityObjectName, methodName, parameterToPass) //
        // All must be String values //
        String backgroundColor = emotionBackgroundColor(emotionInteger);
        UnityPlayer.UnitySendMessage(mainCameraObject, changeBackgroundColorFunction, backgroundColor);
        switch (emotionInteger) {
            case HAPPY:
                UnityPlayer.UnitySendMessage(eyebrowsObject, setEmotionFunction, "HAPPY");
                UnityPlayer.UnitySendMessage(mouthObject, setEmotionFunction, "HAPPY");
                UnityPlayer.UnitySendMessage(tearObject, setEmotionFunction, "HAPPY");
                break;
            case SAD:
                UnityPlayer.UnitySendMessage(eyebrowsObject, setEmotionFunction, "SAD");
                UnityPlayer.UnitySendMessage(mouthObject, setEmotionFunction, "SAD");
                UnityPlayer.UnitySendMessage(tearObject, setEmotionFunction, "SAD");
                break;
            case SURPRISED:
                UnityPlayer.UnitySendMessage(eyebrowsObject, setEmotionFunction, "SURPRISED");
                UnityPlayer.UnitySendMessage(mouthObject, setEmotionFunction, "SURPRISED");
                UnityPlayer.UnitySendMessage(tearObject, setEmotionFunction, "SURPRISED");
                break;
            case ANGRY:
                UnityPlayer.UnitySendMessage(eyebrowsObject, setEmotionFunction, "ANGRY");
                UnityPlayer.UnitySendMessage(mouthObject, setEmotionFunction, "ANGRY");
                UnityPlayer.UnitySendMessage(tearObject, setEmotionFunction, "ANGRY");
                break;
            case IDLE:
                UnityPlayer.UnitySendMessage(eyebrowsObject, setEmotionFunction, "IDLE");
                UnityPlayer.UnitySendMessage(mouthObject, setEmotionFunction, "IDLE");
                UnityPlayer.UnitySendMessage(tearObject, setEmotionFunction, "IDLE");
                break;
            default:
        }

        // This part makes the mouth move if speaking is happening //
        if (voiceYourEmotion != null) {
            voiceYourEmotion.start();
            UnityPlayer.UnitySendMessage(mouthObject, setSpeakingFunction, startSpeaking);
            voiceYourEmotion.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    // This bit gets called back when audio is finished and mouth needs to stop moving //
                    UnityPlayer.UnitySendMessage(mouthObject, setSpeakingFunction, stopSpeaking);
                }
            });
        }
    }

    /**
     * Holds Background color hex
     */
    String backgroundColor = "000000";

    /**
     * Returns emotion color
     * @param emotion emotion integer
     * @return String containing the hex code of an emotion background color.
     */
    private String emotionBackgroundColor(int emotion) {
        switch (emotion) {
            case HAPPY:
                backgroundColor = "FFB400";
                break;
            case SAD:
                backgroundColor = "058548";
                break;
            case SURPRISED:
                backgroundColor = "FFFFFF";
                break;
            case ANGRY:
                backgroundColor = "B60000";
                break;
            case IDLE:
                backgroundColor = "878787";
                break;
            default:
        }
        return (backgroundColor);
    }

    /**
     * @brief Establishes a Bluetooth serial communication
     * @details Fetches device info from paired devices.
     */
    private void beginConnection() {

        boolean deviceFound = false;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device. //
            for (BluetoothDevice device : pairedDevices) {
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceHardwareAddress.equals(MAC_ADDRESS)) {
                    // This bit matches the MAC address of your "face device" to a paired device's //
                    // Then establishes a connection on a separate thread //
                    deviceFound = true;
                    myBluetoothService.startClient(device, MY_UUID);
                    break;
                }
            }
            if (!deviceFound) {
                Toast.makeText(getBaseContext(), "ERROR: Device with MAC address " + MAC_ADDRESS + " not paired", Toast.LENGTH_LONG).show();
                this.finishAffinity();
            }

        } else {
            Toast.makeText(getBaseContext(), "ERROR:" + getString(R.string.NO_PAIRED_DEVICES), Toast.LENGTH_LONG).show();
            this.finishAffinity();
        }
    }

    /**
     * @param message Message to be shown in Toast
     * @brief Toasts for threads other than the main
     * @details Toasts can only be displayed on the main thread. To call Toasts from other threads, a public method in the Activity running on the main thread can be used.
     */
    public void showToastMethod(final String message) {
        //No idea how this works but what it does is run the Toast message called from another thread on the main thread
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * @brief Poke enable/disable and turn off tab
     * @details
     * @li Turns on or off Poking
     * @li Triggers sleepy animation and turns off face device
     * @param messagePieces String instruction received, broken into pieces
     */
    private void settingsUpdate(String[] messagePieces) {

        // Will change these to different letters later //
        switch (messagePieces[1].toUpperCase()) {
            case "POKE":
                switch (messagePieces[2]) {
                    case "DISABLE":
                        UnityPlayer.UnitySendMessage(ouchZoneObject, setEyePokeEnabledStateFunction, falseString);
                        break;
                    case "ENABLE":
                        UnityPlayer.UnitySendMessage(ouchZoneObject, setEyePokeEnabledStateFunction, trueString);
                        break;
                }
                break;
            case "SLEEP":
                // run the sleepy animation //
                UnityPlayer.UnitySendMessage(eyelidsObject, goToSleepFunction, "");

                // delay a little bit //
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // turn the device off //
                        mDevicePolicyManager.lockNow();
                    }
                }, 2500);
                break;
            default:
                break;
        }

    }

    /**
     * @param requestCode the requestCode passed as the second parameter to startActivityForResult(), here it is REQUEST_ENABLE_BT
     * @param resultCode  Possible values - RESULT_OK or RESULT_CANCELED
     * @param data        Optional parameter. An Intent, which can return result data to the caller. `@Nullable` denotes that a value can be null.
     * @brief Called back after focus is returned from process started by startActivityForResult()
     * @details If enabling Bluetooth succeeds, this activity receives the RESULT_OK result code in the onActivityResult() callback. If Bluetooth was not enabled due to an error (or the user responded "No") then the result code is RESULT_CANCELED.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            beginConnection();
        }
        if (resultCode == RESULT_CANCELED)
            Toast.makeText(this, "Unable to access Bluetooth", Toast.LENGTH_SHORT).show();
    }

    /**
     * @brief Created when exporting from Unity
     * @note Created when exporting from Unity. As far as I can tell, it helps load the app back up quickly when returning from another app.
     * @li Override this in your custom UnityPlayerActivity to tweak the command line arguments passed to the Unity Android Player
     * @li The command line arguments are passed as a string, separated by spaces
     * @li UnityPlayerActivity calls this from 'onCreate'
     * @li Supported: -force-gles20, -force-gles30, -force-gles31, -force-gles31aep, -force-gles32, -force-gles, -force-vulkan
     * @li See <a href="https://docs.unity3d.com/Manual/CommandLineArguments.html">https://docs.unity3d.com/Manual/CommandLineArguments.html</a>
     * @param cmdLine the current command line arguments, may be null
     * @return the modified command line string or null
     */
    protected String updateUnityCommandLineArguments(String cmdLine) {
        return cmdLine;
    }

    /**
     * @brief Created when exporting from Unity
     * @note Created when exporting from Unity. As far as I can tell, it helps load the app back up quickly when returning from another app.
     * @li To support deep linking, we need to make sure that the client can get access to the last sent intent.
     * @li The clients access this through a JNI api that allows them to get the intent set on launch.
     * @li To update that after launch we have to manually replace the intent with the one caught here.
     * @li When the activity is re-launched while at the top of the activity stack instead of a new instance of the activity being started, onNewIntent() will be called on the existing instance with the Intent that was used to re-launch it.
     * @li More <a href="https://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)">here</a>.
     * @param intent The new intent that was started for the activity
     */
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        mUnityPlayer.newIntent(intent);
    }

}