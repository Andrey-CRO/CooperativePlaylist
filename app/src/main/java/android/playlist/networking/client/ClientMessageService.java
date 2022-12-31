package android.playlist.networking.client;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.playlist.MainActivity;
import android.playlist.networking.server.ServerMainThread;
import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A client service used to send messages to the server (group/playlist owner).
 * Works as an Intent service (receives Intents).
 * Two actions are currently supported: sending a new song and voting to skip the current one.
 * Uses a PrintWriter to send the String messages.
 * A delimiter is used in the message to differentiate between the action and the content.
 */
public class ClientMessageService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_SONG = "SEND_VIDEO";
    public static final String ACTION_VOTE_SKIP = "VOTE_SKIP";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_VIDEO_ID = "video_id";
    public static final String DELIMITER = ";"; //delimiter to parse actions and content
    String host;
    Integer port;
    public ClientMessageService(String name) {
        super(name);
    }

    public ClientMessageService() {
        super("ClientMessageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(MainActivity.TAG, "Handling client service");
        Bundle extras = intent.getExtras();
        Bundle bundle = extras.getBundle("bundle");
        host = bundle.getString(EXTRAS_GROUP_OWNER_ADDRESS);
        if (intent.getAction().equals(ACTION_SEND_SONG)) {
            String videoID = bundle.getString(EXTRAS_VIDEO_ID);
            sendMessage(ACTION_SEND_SONG + DELIMITER + videoID);
        } else if(intent.getAction().equals(ACTION_VOTE_SKIP)){
            sendMessage(ACTION_VOTE_SKIP + DELIMITER);
        }
    }

    private void sendMessage(String message) {
        Socket socket = new Socket();
        try {
            Log.d(MainActivity.TAG, "Opening client socket - ");
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, ServerMainThread.GROUP_OWNER_PORT)), SOCKET_TIMEOUT);
            Log.d(MainActivity.TAG, "Client socket - " + socket.isConnected());
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.write(message);
            Log.d(MainActivity.TAG, "Client: Data written");
            writer.close();
        } catch (IOException e) {
            Log.e(MainActivity.TAG, e.getMessage());
        } finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
