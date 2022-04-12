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
1. Visit [client.regulaforensics.com](https://client.regulaforensics.com) to get a trial license (`regula.license` file). The license creation wizard will guide you through the necessary steps.
1. Download or clone the current repository using the command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
1. Repository structure and steps to build the projects:
    1. `Advanced` folder contains the advanced sample project with almost all available features. To build it, do the following steps:
        1. Go to the `Advanced` folder. There you will find the project written in Kotlin.
        1. Copy the license file to the project: `Advanced/DocumentReader-Kotlin/app/src/main/res/raw/`.
        1. Open the project in an IDE.
        1. Change the application ID to the one you have specified during the registration at [client.regulaforensics.com](https://client.regulaforensics.com).
        1. Run the project.
    
    1. `Basic` folder contains the basic sample project with only main features. To build it, do the following steps:
        1. Go to the `Basic` folder. There you will two projects: one is written in Kotlin, another in Java.
        1. Copy the license file to the project: `Basic/DocumentReader-sample/app/src/main/res/raw/` or `Basic/DocumentReader-sample_kotlin/app/src/main/res/raw/`.
        1. Open the project in an IDE.
        1. Change the application ID to the one you have specified during the registration at [client.regulaforensics.com](https://client.regulaforensics.com).
        1. Run the project.

## Troubleshooting license issues
If you have issues with license verification when running the application, please verify that next is true:
1. The OS, which you use, is specified in the license (Android).
2. The application ID, which you use, is specified in the license.
3. The license is valid (not expired).
4. The date and time on the device, where you run the application, are valid.
5. You use the latest release version of the Document Reader SDK.
6. You placed the license into the project.

## Documentation
The documentation on the SDK can be found [here](https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile?utm_source=github).

## Additional information
If you have any technical questions or suggestions, feel free to [contact](mailto:android.support@regulaforensics.com) us or create an issue [here](https://github.com/regulaforensics/DocumentReader-Android/issues).

To use our SDK in your own app you have to [purchase](https://pipedrivewebforms.com/form/5f1d771cbe4f844a1f78f8a06fbf94361841159) a commercial license.
