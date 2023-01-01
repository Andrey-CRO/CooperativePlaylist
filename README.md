# Cooperative Playlist
Cooperative Playlist is an Android application that allows Android devices to connect locally and manage a playlist cooperatively. 

<img src="https://user-images.githubusercontent.com/110786361/210132444-07350cfd-027d-4446-8542-7effadf07f6a.jpg" width="20%"></img> <img src="https://user-images.githubusercontent.com/110786361/210132429-90a2d0ac-04c3-4220-a9c6-e954fb214e1a.jpg" width="20%"></img> <img src="https://user-images.githubusercontent.com/110786361/210131931-b82c64b8-d5a3-4468-96cb-4c1149cc11e3.jpg" width="20%"></img> <img src="https://user-images.githubusercontent.com/110786361/210132521-b5762e10-7600-4a32-82d4-cb7cf64c7e30.jpg" width="20%"></img> <img src="https://user-images.githubusercontent.com/110786361/210132463-53b362b3-509d-4dd4-af74-fc2f7ed9b057.jpg" width="20%"></img> <img src="https://user-images.githubusercontent.com/110786361/210132521-b5762e10-7600-4a32-82d4-cb7cf64c7e30.jpg" width="20%"></img> <img src="https://user-images.githubusercontent.com/110786361/210137424-0c15a250-c04e-4daf-a596-cd99dae40a04.jpg" width="20%"></img> 

A single device in the group is the playlist owner, maintaining explicit control over the playlist.
The other devices (peers) connected to the owner can add songs to the playlist queue or vote to skip the current song (majority required).
Wi-Fi Direct (P2P) is used to connect the Android devices together into a Local Area Network (LAN).
The device accepting the incoming connection requests is expected to be designated as the playlist owner. 

When adding a new song, the user is expected to enter a Youtube search query. Based on the query, the application will display the top 5 Youtube results from which the user can choose.
The audio of the chosen Youtube video will be added to the playlist.
This functionality is akin to popular Discord music bots, which have commonly allowed users to choose songs based on simple Youtube title queries.

Currently, Youtube is the only supported audio API.

Every user action can be performed through the use of explicit buttons displayed on the application user inteface. However, for ease-of-use, those actions can likewise be performed via swiping gestures:
- swipe top - display playlist
- swipe bottom - discover nearby devices
- swipe right - add a new song to the playlist
- swipe left - vote to skip current song

The application is based on [Google's WifiDirectDemo](https://android.googlesource.com/platform/development/+/master/samples/WiFiDirectDemo).

[HaarigerHarald's Android based YouTube URL extractor](https://github.com/HaarigerHarald/android-youtubeExtractor) is used to extract audio from Youtube video sources.

The .apk file used for installing the Android application can be found in the root of the repository or downloaded at the following [Dropbox link](https://www.dropbox.com/s/z0b568mhe8kkz9q/cooperative-playlist.apk?dl=0).

[VirusTotal scan of the .apk file](https://www.virustotal.com/gui/file/f4fb7bfc47d9e8260d44191b7ce36791bc645cab688fa916ded523d0e84417e9/detection)

