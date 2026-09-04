# SpellBee AI - Android Assistant

A floating overlay Android app for spelling competitions with offline Vosk speech-to-text and phoneme AI suggestion engine.

## Quick Start (Two Ways to Build the APK)

### Method 1: Automatic Free Cloud Build with GitHub Actions (No PC needed)
1. Upload this extracted folder into a new GitHub repository (public or private).
2. Go to the "Actions" tab in your repository.
3. The workflow "Build SpellBee AI APK" (.github/workflows/build-apk.yml) runs automatically.
4. Download the generated `app-debug.apk` from the Workflow Summary under "Artifacts".

### Method 2: Build on your phone with AIDE
1. Copy this project folder to your phone's storage in `/AppProjects/SpellBeeAI`.
2. Download the Vosk offline model from: https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
3. Unzip it and rename the folder to `model-en-us`.
4. Place `model-en-us` in `app/src/main/assets/model-en-us`.
5. Open AIDE, open this project, and press Run!
