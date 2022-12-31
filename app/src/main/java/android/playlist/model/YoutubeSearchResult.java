package android.playlist.model;

import android.os.Parcel;
import android.os.Parcelable;

public class YoutubeSearchResult implements Parcelable {
    private String title;
    private String videoId;

    public YoutubeSearchResult(String title, String videoId) {
        this.title = title;
        this.videoId = videoId;
    }

    protected YoutubeSearchResult(Parcel in) {
        title = in.readString();
        videoId = in.readString();
    }

    public static final Creator<YoutubeSearchResult> CREATOR = new Creator<YoutubeSearchResult>() {
        @Override
        public YoutubeSearchResult createFromParcel(Parcel in) {
            return new YoutubeSearchResult(in);
        }

        @Override
        public YoutubeSearchResult[] newArray(int size) {
            return new YoutubeSearchResult[size];
        }
    };

    @Override
    public String toString() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeString(videoId);
    }
}
