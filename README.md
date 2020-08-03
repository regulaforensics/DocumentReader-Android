# Regula Document Reader (Android version)
Regula Document Reader SDK allows you to read various kinds of identification documents, passports, driving licenses, ID cards, etc. All processing is performed completely ***offline*** on your device. No any data leaving your device.

You can use native camera to scan the documents or image from gallery for extract all data from it.

We have provided a simple application that demonstrates the ***API*** calls you can use to interact with the Document Reader Library.

<img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_1.jpg" width="250"> <img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_2.jpg" width="250"> <img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_3.jpg" width="250">

# Content
* [How to build the demo application](#how-to-build-the-demo-application)
* [Troubleshooting license issues](#troubleshooting-license-issues)
* [Documentation](#documentation)
* [Additional information](#additional-information)

## How to build the demo application
1. Visit [licensing.regulaforensics.com](https://licensing.regulaforensics.com) to get a trial license (`regula.license` file). The license creation wizard will guide you through the necessary steps.

**Note**: optionally, you can also download the documents database (`db.dat` file) to add it to the project manually and use the app without the Internet. Otherwise, it'll be downloaded from the Internet while the app is running.

2. Download and install the latest [Android Studio](https://developer.android.com/studio/index.html).
3. Download or clone current repository using the command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
4. Launch Android Studio and select _Open an existing Android Studio project_, then select _DocumentReader-sample_ or _DocumentReader-sample_kotlin_ project in the file browser.
5. Download additional files proposed by Android Studio to build the project (e.g., build tools).
6. Change the application ID in the `/app/build.gradle` file to the one you have specified during the registration at [licensing.regulaforensics.com](https://licensing.regulaforensics.com) (`com.regula.documentreader` is set by default).
7. Copy the `regula.license` file to the `/app/src/main/res/raw` folder. If the database has been downloaded manually, place it to the `/app/src/main/assets/Regula` folder.
8. Build and run the application.

## Troubleshooting license issues
If you have issues with license verification when running the application, please verify that next is true:
1. The OS, which you use, is specified in the license (Android).
2. The application ID, which you use, is specified in the license.
3. The license is valid (not expired).
4. The date and time on the device, where you run the application, are valid.
5. You use the latest release version of the Document Reader SDK.
6. You placed the license into the project.

## Documentation
The documentation on the SDK can be found [here](https://docs.regulaforensics.com/android).

## Additional information
If you have any technical questions or suggestions, feel free to [contact](mailto:android.support@regulaforensics.com) us or create an issue [here](https://github.com/regulaforensics/DocumentReader-Android/issues).

To use our SDK in your own app you have to [purchase](https://pipedrivewebforms.com/form/5f1d771cbe4f844a1f78f8a06fbf94361841159) a commercial license.
