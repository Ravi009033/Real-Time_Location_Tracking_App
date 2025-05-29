# Android Real-Time Location Tracking App

This Android application enables real-time location sharing using **Google Maps API** and **Firebase Realtime Database**. Users can:

- View their current location on the map
- Manually select any location via map clicks
- Share their location to Firebase
- Fetch and display a second shared location
- Track two points and visualize them with markers

## 🛠️ Features

- Google Maps Integration
- Real-Time Location Fetching using `FusedLocationProviderClient`
- Firebase Realtime Database Integration
- Manual Location Selection via Map Click
- Address Search with Geocoder
- Marker Placement and Camera Movement
- Location Sharing & Receiving
- Dynamic permissions handling

---


## 🔧 Prerequisites

- Android Studio Arctic Fox or later
- Minimum SDK: 21 (Android 5.0)
- Google Maps API key
- Firebase project with Realtime Database enabled

---

## 🔑 API Key Configuration

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create or open a project.
3. Enable **Maps SDK for Android**.
4. Generate an API key.
5. Add the key to your `AndroidManifest.xml`:

xml
```
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE"/>
```

