package android.playlist.networking.tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.playlist.PlayerFragment;
import android.playlist.R;
import android.playlist.model.YoutubeSearchResult;
import android.playlist.networking.client.ClientMessageService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

/**
 * Asynchronous task that performs a Youtube search based on the received query and returns the top {@value #RESULT_LIMIT} results.
 * The Google Youtube API is used to perform the search.
 * The resulting JSON is parsed, after which the user is presented with the list of results.
 * Based on the user choice, the selected Youtube video ID is sent to the playlist owner (via the ClientMessageService; peers) or added to the playlist (owner).
 */

public class SearchVideos extends AsyncTask<String, ArrayList<YoutubeSearchResult>, ArrayList<YoutubeSearchResult>> {
    static final int RESULT_LIMIT = 5;
    private static final String YOUTUBE_API_KEY = "AIzaSyDpT91YjS8wMLOuYlQ08H6BgrnOZLBM1po";
    Context context;
    WifiP2pInfo info;
    String searchParam;
    ProgressDialog progressDialog;

    public SearchVideos(Context context, WifiP2pInfo info, String searchParam) throws IOException {
        this.context = context;
        this.info = info;
        this.searchParam = searchParam;
    }


    @Override
    protected void onPreExecute() {
        progressDialog = ProgressDialog.show(context, "Searching Youtube...", "Searching for " + searchParam, true,
                true, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                    }
                });
    }

    @Override
    protected ArrayList<YoutubeSearchResult> doInBackground(String... params) {
        String result = "";
        String inputLine;
        try {
                Log.d("fetch_yt", "Fetching Youtube results");
                URL databaseEndpoint = new URL("https://youtube.googleapis.com/youtube/v3/search?part=snippet&maxResults=" + RESULT_LIMIT + "&q=" + URLEncoder.encode(searchParam, String.valueOf(StandardCharsets.UTF_8)) + "&key=" + YOUTUBE_API_KEY);
                HttpsURLConnection myConnection = (HttpsURLConnection) databaseEndpoint.openConnection();
                myConnection.setConnectTimeout(2500);
                myConnection.setReadTimeout(2500);
                try {
                    if (myConnection.getResponseCode() == 200) {
                        //Create a new InputStreamReader
                        InputStreamReader streamReader = new InputStreamReader(myConnection.getInputStream());   //Create a new buffered reader and String Builder
                        BufferedReader reader = new BufferedReader(streamReader);
                        StringBuilder stringBuilder = new StringBuilder();     //Check if the line we are reading is not null
                        while ((inputLine = reader.readLine()) != null) {
                            stringBuilder.append(inputLine);
                        }         //Close our InputStream and Buffered reader
                        reader.close();
                        streamReader.close();    //Set our result equal to our stringBuilder
                        result = stringBuilder.toString();
                        Log.d("fetch_result", result);
                        ArrayList<YoutubeSearchResult> searchResults = parseJson(result); //parse into JSON
                        return searchResults;
                    } else {
                        Log.e("fetch_error", "error in fetching events" + myConnection.getResponseCode());
                        Toast.makeText(context,"Unable to search Youtube",Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.d("error", e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (myConnection != null) {
                        myConnection.disconnect();
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("fetch_yt_connect_error", e.getMessage());
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onPostExecute(ArrayList<YoutubeSearchResult> searchResults) {
        if(progressDialog != null) progressDialog.dismiss();
        if(searchResults == null || searchResults.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Youtube video");
        String[] titles = searchResults.stream().map(video -> video.getTitle()).toArray(String[]::new); //map video titles to array
        builder.setItems(titles, new DialogInterface.OnClickListener() { //display list of results to user
            @Override
            public void onClick(DialogInterface dialog, int which) { //user selected video
                YoutubeSearchResult selectedSearchResult = searchResults.get(which);
                String selectedVideoId = selectedSearchResult.getVideoId();
                if(info.isGroupOwner){ //group owner can just add it to the playlist locally
                    PlayerFragment playerFragment = (PlayerFragment) ((Activity)context).getFragmentManager()
                            .findFragmentById(R.id.frag_player);
                    playerFragment.playYoutubeVideo(selectedVideoId);
                } else { //peer has to send the videoId to the playlist owner
                    Intent serviceIntent = new Intent(context, ClientMessageService.class);
                    serviceIntent.setAction(ClientMessageService.ACTION_SEND_SONG);
                    Bundle bundle = new Bundle();
                    bundle.putString(ClientMessageService.EXTRAS_VIDEO_ID, selectedVideoId);
                    bundle.putString(ClientMessageService.EXTRAS_GROUP_OWNER_ADDRESS,
                            info.groupOwnerAddress.getHostAddress());
                    serviceIntent.putExtra("bundle", bundle);
                    context.startService(serviceIntent);
                    Toast.makeText(context,"Sending song...",Toast.LENGTH_LONG).show();
                }
            }
        });
        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /** Youtube JSON result snippet:
           {
           "kind": "youtube#searchListResponse",
           "etag": "NM0hpdzSG4UHyQcDSiFs6SkhNO4",
           "nextPageToken": "CBkQAA",
           "pageInfo": {
               "totalResults": 1000000,
               "resultsPerPage": 25
           },
           "items": [
               {
                   "kind": "youtube#searchResult",
                   "etag": "XciMzvTveMmo047kcPUk-Ng6NsY",
                   "id": {
                       "kind": "youtube#video",
                       "videoId": "waAlgFq9Xq8"
                   }
            ...
        */
    private ArrayList<YoutubeSearchResult> parseJson(String json){
        ArrayList<YoutubeSearchResult> searchResults = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            Log.d("root", root.toString());
            JSONArray items = root.getJSONArray("items");
            for(int i = 0; i<items.length();i++){
                JSONObject item = items.getJSONObject(i);
                String title = item.getJSONObject("snippet").getString("title");
                Log.d("title", title);
                String videoId = item.getJSONObject("id").getString("videoId");
                Log.d("videoId", videoId);
                YoutubeSearchResult searchResult = new YoutubeSearchResult(title, videoId);
                searchResults.add(searchResult);
            }
            return searchResults;
        } catch (JSONException e) {
            Log.e("json_error", "Error occurred while parsing Youtube json response");
            e.printStackTrace();
            return searchResults;
        }
    }

}