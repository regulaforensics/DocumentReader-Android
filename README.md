# Regula Document Reader (Android version)
Regula Document Reader SDK allows you to read various kinds of identification documents, passports, driving licenses, ID cards, etc. All processing is performed completely ***offline*** on your device. No any data leaving your device.

You can use native camera to scan the documents or image from gallery for extract all data from it.

We have provided a simple application that demonstrates the ***API*** calls you can use to interact with the DocumentReader Library.

<img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_1.jpg" width="250"> <img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_2.jpg" width="250"> <img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_3.jpg" width="250">

# Content
* [How to build demo application](#how-to-build-demo-application)
* [How to add DocumentReader library to your project](#how-to-add-documentreader-library-to-your-project)
* [Troubleshooting license issues](#troubleshooting-license-issues)
* [Documentation](#documentation)
* [Additional information](#additional-information)

## How to build demo application
1. Download and install latest [Android Studio](https://developer.android.com/studio/index.html).
2. Download or clone current repository using the command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
3. Launch Android Studio and select _Open an existing Android Studio project_ then select _DocumentReader-sample_ project in file browser.
4. Download additional files proposed by Android Studio to build project (e.g., build tools).
5. Visit [licensing.regulaforensics.com](https://licensing.regulaforensics.com) to get your trial license(`regula.license`) and documents database(`db.dat`). Download both files to your computer. You can create license by clicking "Generate demo license". License creation wizard will guide you through necessary steps. You can select and download appropriate database version on "Databases" page.
6. Change the application ID in the `DocumentReader-sample/app/build.gradle`  file to the one you have specified during registration at [licensing.regulaforensics.com](https://licensing.regulaforensics.com) (`com.regula.documentreader` by default).
7. Copy `regula.license` file to `DocumentReader-sample/app/src/main/res/raw` folder.
8. Copy downloaded database (`db.dat` file) to `DocumentReader-sample/app/src/main/assets/Regula` folder.
9. Build and run the application.

## How to add DocumentReader library to your project
Document Reader libraries are available in our [Maven repository](http://maven.regulaforensics.com/RegulaDocumentReader/com/regula/documentreader).

First of all, install **API** library, simply adding the following lines of code to the `build.gradle` file of your project:
```
implementation ('com.regula.documentreader:api:+aar'){
    transitive = true
}
```

And then add one of the Core libraries depend on the functionality that you wish and the license capabilities:
* Install **barcode** library edition:
```
implementation 'com.regula.documentreader.barcode:core:+@aar'
```

* Install **barcodemrz** library edition:
```
implementation 'com.regula.documentreader.barcodemrz:core:+@aar'
```

* Install **bounds** library edition:
```
implementation 'com.regula.documentreader.bounds:core:+@aar'
```

* Install **doctype** library edition:
```
implementation 'com.regula.documentreader.doctype:core:+@aar'
```

* Install **full** library edition:
```
implementation 'com.regula.documentreader.full:core:+@aar'
```

* Install **fullrfid** library edition:
```
implementation 'com.regula.documentreader.fullrfid:core:+@aar'
```

* Install **mrz** library edition:
```
implementation 'com.regula.documentreader.mrz:core:+@aar'
```

* Install **mrzrfid** library edition:
```
implementation 'com.regula.documentreader.mrzrfid:core:+@aar'
```

* Install **ocrandmrz** library edition:
```
implementation 'com.regula.documentreader.ocrandmrz:core:+@aar'
```

## Troubleshooting license issues
If you have issues with license verification when running the application, please verify that next is true:
1. The OS, which you use, is specified in the license (Android).
2. The application ID, which you use, is specified in the license.
3. The license is valid (not expired).
4. The date and time on the device, where you run the application, are valid.
5. You use the latest release version of the Document Reader SDK.
6. You placed the license into the correct folder (`DocumentReader-sample/app/src/main/res/raw`) as described in [How to build demo application](#how-to-build-demo-application).

## Documentation
You can find documentation on API [here](https://docs.regulaforensics.com/android).

## Additional information
If you have any technical questions, feel free to [contact](mailto:android.support@regulaforensics.com) us or create an issue [here](https://github.com/regulaforensics/DocumentReader-Android/issues).

To use our SDK in your own app you need to [purchase](https://pipedrivewebforms.com/form/5f1d771cbe4f844a1f78f8a06fbf94361841159) a commercial license.
