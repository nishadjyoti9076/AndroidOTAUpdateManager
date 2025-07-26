This project showcases an Android app that checks for available updates using a local Express.js server. When a newer APK is found, it downloads and installs the update, complete with version details and release notes.


## 📦 Features
✅ Version check using latest_version.json
📥 APK file served via local Express server
🔔 Update prompt with release notes
📦 APK download and installation flow
🛠 Built using OkHttp + Gson + FileProvider



## 🧑‍💻 Prerequisites
Android Studio (Arctic Fox or newer)
Node.js installed (v16+ recommended)
Java JDK 17 correctly configured
Device and host machine connected to the same network

## 🚀 Setup Instructions
🖥️ Server Setup (Express.js)
Place server.js, OTADemo_v2.apk, and latest_version.json in the same folder


## Make sure server.js contains:
js
const express = require('express');
const app = express();
const PORT = 8080;
app.use(express.static(__dirname));
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server is running at http://<your-local-ip>:${PORT}`);
});

## Run the server:
bash
node server.js


## 📂 Folder structure:

myapks/
├── server.js
├── OTADemo_v2.apk
└── latest_version.json


## 📲 Android App Setup
Open the project in Android Studio
Build and run the app on a device/emulator
Tap the "Check Update" button
If a newer version exists, you’ll see a prompt with release notes
Accept to begin downloading and installation

## MIT License

Copyright (c) 2025 Jyoti Nishad

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction...

