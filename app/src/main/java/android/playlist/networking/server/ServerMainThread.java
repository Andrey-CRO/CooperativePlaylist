package android.playlist.networking.server;

import android.os.Handler;
import android.playlist.MainActivity;
import android.playlist.networking.client.ClientMessageService;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main server thread used by the playlist/group owner to receive client connections.
 * Upon each client connection request to the server socket, a new instance of the ServerCommunicationThread is spawned to handle that client.
 */

public class ServerMainThread implements Runnable{
    private PlayerHandler playerHandler;
    private Handler uiHandler;
    private boolean isRunning;
    private ServerSocket serverSocket;
    public static final int GROUP_OWNER_PORT = 8988;

    public ServerMainThread(PlayerHandler playerHandler, Handler uiHandler) {
        this.playerHandler = playerHandler;
        this.uiHandler = uiHandler;
    }

    public void run()
    {
        isRunning = true;
        Socket client = null;
        try
        {
            serverSocket = new ServerSocket(GROUP_OWNER_PORT);
            Log.d(MainActivity.TAG, "Server: Socket opened");
        } catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
        while (!Thread.currentThread().isInterrupted() && isRunning)
        {
            try
            {
                client = serverSocket.accept();
                Log.d(MainActivity.TAG, "Server: client accepted");

                ServerCommunicationThread commThread = new ServerCommunicationThread(client, uiHandler, playerHandler);
                new Thread(commThread).start();

            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        stopServerSocket();
        Log.d(MainActivity.TAG, "Server stopped");
    }


    public void stopServerSocket()
    {
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        isRunning = false;
    }

}
