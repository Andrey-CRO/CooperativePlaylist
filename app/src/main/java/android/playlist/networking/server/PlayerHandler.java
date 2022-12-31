package android.playlist.networking.server;


import android.playlist.PlayerFragment;
import android.util.ArraySet;
import android.util.Log;

import java.util.Set;

/**
 * Hndles business logic as an intermediate between the background server thread and the player activity.
 * Runs on the UI thread (simple operations).
 * Two actions are currently supported: adding a new song to the playlist and submitting a vote to skip the current song.
 * The voting mechanism is contained within this class. PlayerHandler only signals the player activity when the vote has been decided and the song has to be skipped.
 */

public class PlayerHandler{
    private PlayerFragment playerFragment;
    int votesToSkip = 0;
    int lastMediaIndex = 0;
    Set<String> votedPeers = new ArraySet<>(); //peers who voted to skip song

    public PlayerHandler(PlayerFragment playerFragment) {
        this.playerFragment = playerFragment;
    }

    public void addAudioSource(String videoId){
        playerFragment.playYoutubeVideo(videoId);
    }

    /**
     *
     * Majority of peers need to vote in favor of skipping the current song. The group owner is not taken into account.
     * @param peerAddress Inet address of peer submitting a vote. Sent to ensure peer cannot vote twice to skip the same song.
     */
    public void voteToSkip(String peerAddress) {
        int currentMediaIndex = playerFragment.getCurrentMediaIndex();
        if(currentMediaIndex != lastMediaIndex) { //check if song changed in the meantime, reset votes and set if it did
            votesToSkip = 0;
            votedPeers.clear();
            lastMediaIndex = currentMediaIndex; //set new media index to follow votes for
        }
        if(votedPeers.contains(peerAddress)) return; //peer already voted

        votesToSkip++;
        int numOfPeers = playerFragment.getListOfPeers().size(); //get number of peers
        Log.d("numOfPeers", String.valueOf(numOfPeers));
        int majority = (int) Math.ceil((double) (numOfPeers) / 2); //divide peers by 2 and round up to ceiling

        playerFragment.updateVoteStatus(votesToSkip, numOfPeers);
        if(votesToSkip >= majority){
            votedPeers.clear();
            votesToSkip = 0;
            playerFragment.skipSong();
        }

    }
}
