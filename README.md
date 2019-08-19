# Regula Document Reader (Android version)

Regula Document Reader SDK allows you to read various kinds of identification documents, passports, driving licenses, ID cards, etc. All processing is performed completely ***offline*** on your device. No any data leaving your device.

You can use native camera to scan the documents or image from gallery for extract all data from it.

We have provided a simple application that demonstrates the ***API*** calls you can use to interact with the DocumentReader Library. [Just take me to the notes!](https://github.com/regulaforensics/DocumentReader-Android/wiki)

<img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_1.png" width="250"> <img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_2.png" width="250"> <img src="https://img.regulaforensics.com/Screenshots/SDK-5.0/LG_Nexus_5X_3.png" width="250">

# Content

* [How to build demo application](#how-to-build-demo-application)
* [How to add DocumentReader library to your project](#how-to-add-documentreader-library-to-your-project)
* [Troubleshooting license issues](#troubleshooting-license-issues)
* [Documentation](#documentation)
* [Additional information](#additional-information)

## How to build demo application
1. Download and install latest [Android Studio](https://developer.android.com/studio/index.html).
2. Clone current repository using command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
3. Launch Android Studio and select _Open an existing Android Studio project_ then select _DocumentReader-sample_ project in file browser.
4. Download additional files proposed by Android Studio to build project (build tools, for example).
5. Visit [licensing.regulaforensics.com](https://licensing.regulaforensics.com) to get your trial license(`regula.license`) and documents database(`db.dat`). Download both files to your computer. You can create license by clicking "Generate demo license". License creation wizard will guide you through necessary steps. You can select and download appropriate database version on "Databases" page.
6. Change application ID in the `DocumentReader-sample/app/build.gradle`  file to the one you have specified during registration at [licensing.regulaforensics.com](https://licensing.regulaforensics.com) (`com.regula.documentreader` by default).
7. Copy file `regula.license` to `DocumentReader-sample/app/src/main/res/raw` folder.
8. Copy downloaded database file  `db.dat` to `DocumentReader-sample/app/src/main/assets/Regula` folder.
9. Build and run application.

## How to add DocumentReader library to your project

DocumentReader libraries are available in our [Maven repository](http://maven.regulaforensics.com/RegulaDocumentReader/com/regula/documentreader/). To install them, simply add the following lines to your project `build.gradle`

```gradle
implementation ('com.regula.documentreader:api:+@aar'){
	transitive = true
}
```

And one of library depend on functionality which you want and license abilities:

Loading **Full** library edition:
```gradle
implementation 'com.regula.documentreader.fullrfid:core:+@aar'
```

or

```gradle
implementation 'com.regula.documentreader.full:core:+@aar'
```

Loading **Core** library edition:
```gradle
implementation 'com.regula.documentreader.core:core:+@aar'
```

Loading **Bounds** library edition:
```gradle
implementation 'com.regula.documentreader.bounds:core:+@aar'
```

Loading **Barcode** library edition:
```gradle
implementation 'com.regula.documentreader.barcode:core:+@aar'
```

Loading **MRZ** library edition:
```gradle
implementation 'com.regula.documentreader.mrz:core:+@aar'
```

Loading **MRZ-Barcode** library edition:
```gradle
implementation 'com.regula.documentreader.barcodemrz:core:+@aar'
```

Loading **MRZ-RFID** library edition:
```gradle
implementation 'com.regula.documentreader.mrzrfid:core:+@aar'
```

Loading **OCR** library edition:
```gradle
implementation 'com.regula.documentreader.ocrandmrz:core:+@aar'
```

Loading **Bank Card** library edition:
```gradle
implementation 'com.regula.documentreader.creditcard:core:+@aar'
```

## Troubleshooting license issues
If you have issues with license verification when running the application, please verify that next is true:
1. OS you are using is the same as in the license you received (Android).
1. Application ID is the same that you specified for license.
1. Date and time on the device you are trying to run the application is correct and inside the license validity period.
1. You are using the latest release of the SDK.
1. You placed the license into the correct folder as described here [How to build demo application](#how-to-build-demo-application) (`DocumentReader-sample/app/src/main/res/raw`).

## Documentation
You can find documentation on API [here](https://regulaforensics.github.io/DocumentReader-Android/).

## Additional information
Use [Wiki](https://github.com/regulaforensics/DocumentReader-Android/wiki) to get more details. If you have any technical questions, feel free to [contact us](mailto:support@regulaforensics.com) or create issue here.

To use our SDK in your own app you will need to [purchase](https://pipedrivewebforms.com/form/5f1d771cbe4f844a1f78f8a06fbf94361841159) commercial license.

