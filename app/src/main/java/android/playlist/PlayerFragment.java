package android.playlist;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.playlist.model.YoutubeSearchResult;
import android.playlist.networking.client.ClientMessageService;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.ui.TrackSelectionView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * UI Fragment that displays the player on the main activity.
 */

public class PlayerFragment extends Fragment {
    PlayerView playerView;
    ExoPlayer player;
    private View mContentView = null;
    boolean playWhenReady = true;
    Context context;
    TextView playerText;
    TextView voteSkipText;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.activity_player, null);
        context = getActivity();
        playerView = mContentView.findViewById(R.id.player_view);
        playerText = mContentView.findViewById(R.id.player_text);
        playerText.setOnClickListener(
                v -> showPlaylist());
        voteSkipText = mContentView.findViewById(R.id.votes_skip);
        initPlayer();
        return mContentView;
    }

    public void hidePlayer(){
        playerView.setVisibility(View.GONE);
    }

    public void showPlayer(){
        playerView.setVisibility(View.VISIBLE);
    }

    public void initPlayer() {
        Log.d("player_init", "Initializing player");
        player = new ExoPlayer.Builder(context).build();
        playerView.setPlayer(player);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.requestFocus();
        playerView.setKeepScreenOn(true);
        player.addListener(new Player.Listener(){
            @Override
            public void onMediaItemTransition(
                    @Nullable MediaItem mediaItem, @Player.MediaItemTransitionReason int reason) { //playlist transition
                voteSkipText.setText(""); //reset vote counter
                if (mediaItem != null && mediaItem.localConfiguration != null) {
                   Log.d("metadata", (String) mediaItem.localConfiguration.tag);
                    playerText.setText((String) mediaItem.localConfiguration.tag); //get Youtube title tag and display it
                }

            }
        });
    }

    public void playYoutubeVideo(String videoId) {
        Log.d("playYT", "Adding youtube video with ID " + videoId);
        new YouTubeExtractor(context) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if (ytFiles != null) {
                    int audioTag = 251;
                    String ytTitle = vMeta.getTitle();
                    //MediaSource audioSource = new ProgressiveMediaSource.Factory(new DefaultHttpDataSource.Factory()).createMediaSource());
                    MediaItem item = MediaItem.fromUri(ytFiles.get(audioTag).getUrl()).buildUpon().setTag(ytTitle).build(); //build item and set Youtube title as tag
                    int itemCount = player.getMediaItemCount();
                    Log.d("count", String.valueOf(itemCount));
                    player.addMediaItem(item);
                    if(itemCount == 0){ //first song, prepare and start player
                        player.prepare();
                        player.setPlayWhenReady(playWhenReady);
                        playerText.setText(ytTitle);
                    }

                    Toast.makeText(context, "Queued " + ytTitle,
                            Toast.LENGTH_SHORT).show();
                    Log.d("queueYT", "Queued " + ytTitle);
                }
            }
        }.extract(videoId);
    }

    public void resetPlayer(){
        voteSkipText.setText("");
        playerText.setText("");
        releasePlayer();
        hidePlayer();
    }

    public void releasePlayer() {
        if(player != null){
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }

    /**
     *
     * @return Index of current media item in player playlist. Returns -1 if player is unavailable.
     */
    public int getCurrentMediaIndex() {
        if(player == null) return -1;
        return player.getCurrentMediaItemIndex();
    }

    public void skipSong() {
        if(player == null) return;
        if(player.hasNext()){
            player.next();
            Toast.makeText(context, "Skipping song", Toast.LENGTH_LONG).show();
         }
    }

    public List<WifiP2pDevice> getListOfPeers(){
        DeviceListFragment listFragment = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        if(listFragment == null) return new ArrayList<>();
        List<WifiP2pDevice> peerList = listFragment.getPeers(); //get list of peers in group
        Log.d("peers", peerList.toString());
        return peerList;
    }

    public void updateVoteStatus(int votesToSkip, int numOfPeers) {
        voteSkipText.setText("Votes to skip current song: " + votesToSkip + "/" + numOfPeers);
    }

    public void showPlaylist() {
        if(playerView.getVisibility() != View.VISIBLE) return; //player isn't shown, no point in displaying playlist
        Log.d("here", "here");
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Playlist");
        int songCount = player.getMediaItemCount();
        String[] songs = new String[songCount];
        for(int i = 0; i<songCount;i++){
            MediaItem song = player.getMediaItemAt(i);
            songs[i] = song.localConfiguration.tag.toString(); //song title
        }
        builder.setItems(songs, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                player.seekToDefaultPosition(which);
            }
         });

        // create and show the playlist dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}