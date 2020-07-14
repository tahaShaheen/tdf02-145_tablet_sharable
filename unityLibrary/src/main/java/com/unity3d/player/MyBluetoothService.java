package com.unity3d.player;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * @file MyBluetoothService.java
 * @brief Handles everything Bluetooth
 * @details
 * @li Starts up and maintains Bluetooth connections between devices
 * @li Sends and handles reception of messages from connected devices
 */


/**
 * @brief Handles everything Bluetooth
 * @details
 * @li Starts up and maintains Bluetooth connections between devices
 * @li Sends and handles reception of messages from connected devices
 */
public abstract class MyBluetoothService {

    /**
     * @brief Reference to the Activity where the MyBluetoothService object instance is created
     * @details Services, such as this one, require a Context from the Activity that creates an object of its type to hook it to that Activity and provide it access to the application specific resources.
     */
    static Context mContext;

    /**
     * @brief A BluetoothAdapter object
     * @details A BluetoothAdapter lets you perform fundamental Bluetooth tasks, such as initiate device discovery, query a list of bonded (paired) devices, instantiate a BluetoothDevice using a known MAC address, and create a BluetoothServerSocket to listen for connection requests from other devices, and start a scan for Bluetooth LE devices.
     */
    private final BluetoothAdapter bluetoothAdapter;

    /**
     * @brief A progress dialog
     * @details A dialog showing a progress indicator and an optional text message or view. Only a text message or a view can be used at the same time.
     */
    ProgressDialog mProgressDialog;

    /**
     * @var mConnectThread
     * @brief Custom class ConnectThread type object
     * @details Extends <a href="https://developer.android.com/reference/java/lang/Thread">Thread</a>
     * @var mConnectedThread
     * @brief Custom class ConnectedThread type object
     * @details Extends <a href="https://developer.android.com/reference/java/lang/Thread">Thread</a>
     */
    private ConnectThread mConnectThread;
    public ConnectedThread mConnectedThread;

    /**
     * @brief Represents a remote Bluetooth device
     * @details A BluetoothDevice lets you create a connection with the respective device or query information about it, such as the name, address, class, and bonding state.
     */
    private BluetoothDevice mmDevice;


    /**
     * Debugging tool
     */
    private static final String TAG = "DEBUG_BLUETOOTH_SERVICE";

    /**
     * @param context Context of the Activity that creates an object of the MyBluetoothService class
     * @brief Constructor
     * @details The constructor used to create an object of the MyBluetoothService class
     */
    public MyBluetoothService(Context context) {
        mContext = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        cancelThreads();
    }

    /**
     * @brief Cancels any running threads
     * @details Cancels any threads attempting to create a BluetoothSocket or any
     */
    public synchronized void cancelThreads() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection //
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    /**
     * Called to start a Bluetooth connection
     *
     * @param device Represents a remote Bluetooth device. A BluetoothDevice lets you create a connection with the respective device or query information about it, such as the name, address, class, and bonding state.
     * @param uuid   Universally Unique Identifier. UUIDs are not tied to particular devices. They identify software services. You just need both sides to use the same one.
     */
    public void startClient(BluetoothDevice device, UUID uuid) {
        // progress dialog appears //
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth"
                , "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);

        // Starts Thread //
        mConnectThread.start();
    }

    /**
     * @brief Client thread that initiates a Bluetooth connection
     * @details
     * @li A Bluetooth client sends the connection request and the Bluetooth Server component accepts the request.
     * @li A thread is a thread of execution in a program. The Java Virtual Machine allows an application to have multiple threads of execution running concurrently.
     * @li This one creates a BluetoothSocket
     */
    private class ConnectThread extends Thread {

        /**
         * @brief An instance of a Bluetooth socket
         * @details
         * @li A socket is one endpoint of a two-way communication link
         * @li The most common type of Bluetooth socket is RFCOMM, which is the type supported by the Android APIs
         */
        private final BluetoothSocket mmSocket;

        /**
         * @param device BluetoothDevice object
         * @param uuid   UUID object
         * @brief Public constructor for the ConnectThread class
         * @details Creates a RFCOMM Bluetooth Socket
         */
        public ConnectThread(BluetoothDevice device, UUID uuid) {

            // Use a temporary object that is later assigned to mmSocket because mmSocket is final //
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice. //
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        /**
         * @brief Main code that runs in the Thread
         * @details Try Catch to attempt a connection to the remote device.
         */
        public void run() {

            // Cancel discovery because it otherwise slows down the connection //
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks until it succeeds or throws an exception //
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return //

                Log.d(TAG, "R.string.CONNECTION_TO_DEVICE_UNSUCCESSFUL");
                showToast(R.string.CONNECTION_TO_DEVICE_UNSUCCESSFUL);

                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }

                // dismiss the progressDialog //
                try {
                    mProgressDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                return;
            }

            // The connection attempt succeeded. Perform work associated with the connection in a separate thread. //
            Log.d(TAG, "R.string.CONNECTION_TO_DEVICE_SUCCESSFUL");
            showToast(R.string.CONNECTION_TO_DEVICE_SUCCESSFUL);

            // Start a Thread that'll manage the work associated with the connection //
            connected(mmSocket, mmDevice);
        }

        /**
         * @brief Closes the socket
         * @details Closes the client socket and causes the thread to finish. Called from the main activity to shut down the connection.
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    /**
     * Called to start a Thread to manages the Bluetooth connection
     *
     * @param mmSocket RFCOMM Bluetooth Socket object
     * @param mmDevice BluetoothDevice object
     */
    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions //
        mConnectedThread = new ConnectedThread(mmSocket);

        // Starts Thread //
        mConnectedThread.start();
    }

    /**
     * @brief Thread that manages a Bluetooth connection
     * @details
     * @li A thread is a thread of execution in a program. The Java Virtual Machine allows an application to have multiple threads of execution running concurrently.
     * @li This one manages a BluetoothSocket
     */
    private class ConnectedThread extends Thread {
        /**
         * Holds the RFCOMM Socket object
         */
        private final BluetoothSocket mmSocket;

        /**
         * @var mmInStream
         * Holds a reference to an InputStream object, one of two types of streams. One that you can read data from
         * @var mmOutStream
         * Holds a reference to an OutputStream object, one of two types of streams. One that you can either write data to
         */
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        /**
         * @param socket RFCOMM Bluetooth Socket object
         * @brief Constructor for the ConnectedThread class
         * @details Manages a RFCOMM Bluetooth Socket
         */
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;

            // Temporary storage for  an input stream for reading bytes from this socket//
            InputStream tmpIn = null;

            // Temporary storage for  an output stream for writing bytes from this socket//
            OutputStream tmpOut = null;

            // dismiss the progressDialog when connection is established //
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            // Get the InputStream and OutputStream that handle transmissions through the socket using getInputStream() and getOutputStream(), respectively. //
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        /**
         * @brief Main code that runs in the Thread
         * @details Try Catch to read data being sent through the connection with the remote device.
         */
        public void run() {
            // buffer store for the stream //
            byte[] buffer = new byte[1024];

            // String buffer to hold incoming data //
            String concatenatedString = "";

            // bytes returned from read() //
            int bytes;

            // Keep listening to the InputStream until an exception occurs //
            while (true) {
                // Read from the InputStream //
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    concatenatedString = concatenatedString + incomingMessage;

                    if (incomingMessage.contains("#")) {
                        // send the String till # to be broken up //
                        String[] messagePieces = breakUpString(concatenatedString.substring(0, concatenatedString.indexOf("#")));

                        Log.d(TAG, concatenatedString.substring(0, concatenatedString.indexOf("#")));

                        // empty the concatenatedString //
                        concatenatedString = "";

                        // send it to be processed //
                        Log.d(TAG, messagePieces[0]);
                        messageProcessorFunction(messagePieces);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }

        /**
         * @param bytes Array of bytes to be sent to the remote Bluetooth device
         * @brief Sends data to the remote device
         * @details Called this from the main activity to send data to the remote device
         */
        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /**
         * @brief Closes the socket
         * @details Closes the client socket and causes the thread to finish. Called from the main activity to shut down the connection.
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /**
     * @brief Breaks up the String instruction received
     * @details Uses the separator to break up the String instruction
     * @param messageStream the String instruction received
     * @return A String array containing the String instruction received, but in pieces
     */
    private String[] breakUpString(String messageStream) {
        String[] messagePieces = messageStream.split("_");
        return messagePieces;
    }

    /**
     * @param resourceID The resource ID of the String to be displayed in a Toast
     * @brief Displays Toasts
     * @details Toasts only run on the main thread. This allows displaying of a Toast from another thread. By being an abstract method, it can be defined in the MainActivity which runs on the main thread. A method there, showToastMethod(), can then display the Toast.
     */
    public abstract void showToast(int resourceID);

    /**
     * @brief Processes the instruction received
     * @details
     * @li I wanted to keep the message processing in the main Thread and keep the MyBluetoothService.java as free of these app specific "environmental factors" as possible
     * @li So I made the messageProcessorFunction() abstract
     * @li Any function calls from it can now be done in the main Thread
     * @param messagePieces A String array containing the String instruction received, but in pieces
     */
    public abstract void messageProcessorFunction(String[] messagePieces);

    /**
     * @param out Array of bytes
     * @brief Writes to the any Bluetooth device
     * @details
     * @li Called from the main activity
     * @li Hands over the byte Array the Thread managing communication with the Bluetooth device
     * @li Here for future use
     */
    public void write(byte[] out) {
        //perform the write
        mConnectedThread.write(out);
    }

}

