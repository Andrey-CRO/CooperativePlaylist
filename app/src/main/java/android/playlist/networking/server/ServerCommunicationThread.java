package android.playlist.networking.server;

import android.os.Handler;
import android.playlist.MainActivity;
import android.playlist.networking.client.ClientMessageService;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Worker server thread used to handle a specific client connection.
 * Uses a UI Handler to send operations back to the main UI thread, and a PlayerHandler to manage the player.
 * A BufferedReader reads the input from the server socket.
 * Received messages are split using the expected delimiter, to differentiate the message action and content.
 */

public class ServerCommunicationThread implements Runnable{
    private Handler uiHandler;
    private Socket client;
    private PlayerHandler playerHandler;
    private BufferedReader input;

    public ServerCommunicationThread (Socket client, Handler uiHandler, PlayerHandler playerHandler) {
        this.client = client;
        this.uiHandler = uiHandler;  //runs on the UI thread, used to send operations back to it
        this.playerHandler = playerHandler; //handler between the server thread and the player
        try
        {
            this.input = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try
        {
            Log.d(MainActivity.TAG, "Buffered reader ready: " +  input.ready());
            while (!Thread.currentThread().isInterrupted())
            {
                String message = input.readLine();
                Log.d(MainActivity.TAG, "Message received: " + message);
                if(message == null) {
                    break;
                }
                parseMessage(message);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally{
            stopThread();
        }
    }

    private void parseMessage(String message) {
        String[] splitMessage = message.split(ClientMessageService.DELIMITER); //separate action from content
        if (splitMessage.length < 1) return;
        String action = splitMessage[0];
        if (ClientMessageService.ACTION_SEND_SONG.equals(action)) {
            String videoId = splitMessage[1];
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    playerHandler.addAudioSource(videoId);
                }
            });
         } else if (ClientMessageService.ACTION_VOTE_SKIP.equals(action)){
            String peerAddress = client.getInetAddress().toString();
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        playerHandler.voteToSkip(peerAddress);
                    }
                });
        }
    }

    public void stopThread()
    {
        try
        {
            if (client != null) client.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
