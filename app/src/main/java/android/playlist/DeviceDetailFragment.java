package android.playlist;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.playlist.networking.client.ClientMessageService;
import android.playlist.networking.server.PlayerHandler;
import android.playlist.networking.server.ServerMainThread;
import android.playlist.networking.tasks.SearchVideos;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;

/**
 * A fragment that manages a particular peer and allows interaction with the peer
 * i.e. adding a new song to the playlist or voting to skip the current song
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;
    ServerMainThread service;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(v -> {
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            config.groupOwnerIntent = 0; //device pressing connect has the smallest inclination to be the group owner
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                    "Connecting to: " + device.deviceName, true, true
                    );
            ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);

        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                v -> ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect());

        mContentView.findViewById(R.id.btn_add_song).setOnClickListener(
                v -> {
                    addSong();
                });

        mContentView.findViewById(R.id.btn_vote_skip).setOnClickListener(
                v -> voteToSkip());

        return mContentView;
    }


    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText((info.isGroupOwner) ? "Playlist owner" : "Playlist peer");

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Playlist owner IP - " + info.groupOwnerAddress.getHostAddress());

        /**
         * The group owner can use the player and starts a server service. Other peers can connect to the playlist owner as clients
         * and add videos to the playlist or vote to skip the current song.
         **/
        if (info.groupFormed) {
            mContentView.findViewById(R.id.btn_add_song).setVisibility(View.VISIBLE);
            if (info.isGroupOwner) {
                Log.d(MainActivity.TAG, "Starting server service ");
                Handler uiHandler = new Handler(Looper.getMainLooper()); //used to communicate back to UI thread from background server thread
                PlayerFragment playerFragment = (PlayerFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_player);
                PlayerHandler playerHandler = new PlayerHandler(playerFragment); //used to handle all player activies
                service = new ServerMainThread(playerHandler, uiHandler);
                new Thread(service).start();
                playerFragment.initPlayer();
                playerFragment.showPlayer();
                Toast.makeText(getContext(), "Playlist owner", Toast.LENGTH_LONG).show();
            } else {
                // The other device acts as the client.
                mContentView.findViewById(R.id.btn_vote_skip).setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Playlist peer", Toast.LENGTH_LONG).show();
            }

            // hide the connect button
            mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
            mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.VISIBLE);
        }
    }
    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.deviceName);
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        if(service != null) service.stopServerSocket();
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_add_song).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_disconnect).setVisibility(View.GONE);
        mContentView.findViewById(R.id.btn_vote_skip).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    public void addSong(){
        if(info == null || !info.groupFormed) return; //cannot add song if not in group
        AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setTitle("Add song to playlist");
        alert.setMessage("Enter Youtube search query:");

        // Set an EditText view to get user input
        final EditText input = new EditText(getContext());
        alert.setView(input);
        alert.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String searchInput = input.getText().toString();
                if(searchInput != null && !searchInput.isEmpty()){
                    SearchVideos fetchEvents = null;
                    try {
                        fetchEvents = new SearchVideos(getActivity(), info, searchInput);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fetchEvents.execute();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    public void voteToSkip() {
        Intent serviceIntent = new Intent(getContext(), ClientMessageService.class);
        serviceIntent.setAction(ClientMessageService.ACTION_VOTE_SKIP);
        Bundle bundle = new Bundle();
        bundle.putString(ClientMessageService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra("bundle", bundle);
        getContext().startService(serviceIntent);
        Toast.makeText(getContext(),"Submitting vote...",Toast.LENGTH_LONG).show();
    }
}
