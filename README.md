# Regula Document Reader (Android version)
If you have any questions, feel free to contact us at support@regulaforensics.com

* [How to build demo application](#how_to_build_demo_application)
* [Build variants description](#build_variants_description)
* [How to add DocumentReader library to your project](#how_to_add_documentreader_library_to_your_project)
* [Troubleshooting license issues](#troubleshooting_license_issues)
* [Additional information](#additional_information)

## <a name="how_to_build_demo_application"></a> How to build demo application
1. Get trial license for demo application at [licensing.regulaforensics.com](https://licensing.regulaforensics.com) (`regula.license` file).
1. Clone current repository using command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
1. Download and install latest [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
1. Download and install latest [Android Studio](https://developer.android.com/studio/index.html).
1. Copy file `regula.license` to `DocumentReader-sample/app/src/main/res/raw` folder. 
1. Launch Android Studio and select _Open an existing Android Studio project_ then select _DocumentReader-sample_ project in file browser.
1. Download additional files proposed by Android Studio to build project (build tools, for example).
3. Change application ID to specified during registration of your license key at [licensing.regulaforensics.com](https://licensing.regulaforensics.com) (`com.regula.documentreader` by default).
1. Select appropriate build variant and run application.

## <a name="build_variants_description"></a> Build variants description
Depending on the selected build variant, appropriate CORE will be referenced. This causes changes in available operations and results. Please, choose carefully, as each additional functionality causes increasing size of the result application.

* mrz - capable to locate and recognize Machine Readable Zone on the image 
* bounds - capable to locate and crop document from the image
* barcode - capable to loacte and read data from Barcodes on the image
* barcodemrz - combines functionality of mrz and barcode
* ocrandmrz - combines functionality of mrz, bounds and locate
* full - combines functionality of mrz, bounds, barcode and locate (full document visual processing)
* mrzrfid - capable to locate and recognize Machine Readable Zone and read RFID chip using NFC
* fullrfid - same as "full" + capable of reading RFID chips data (ePassport / eDL)

## <a name="how_to_add_documentreader_library_to_your_project"></a> How to add DocumentReader library to your project

DocumentReader libraries are available in our [Maven repository](http://maven.regulaforensics.com/RegulaDocumentReader/com/regula/documentreader/). To install
them, simply add the following lines to your build.gradle:

```java
compile 'com.regula.documentreader:artifact_id:+@aar'	
//possible artifact_id: core, bounds, mrz, barcode, barcodemrz, ocrandmrz, full, fullrfid; Depends on received license
compile ('com.regula.documentreader:api:+aar'){
	transitive = true
}
```
## <a name="troubleshooting_license_issues"></a> Troubleshooting license issues
If you have issues with license verification when running the application, please verify that next is true:
1. OS you are using is the same as in the license you received (Android).
1. Application ID is the same that you specified for license.
1. Date and time on the device you are trying to run the application is correct and inside the license validity term.
1. You are using the latest release of the SDK.
1. You placed the license into the correct folder as described here [How to build demo application](#how_to_build_demo_application) (`DocumentReader-sample/app/src/main/res/raw`).

## <a name="additional_information"></a> Additional information
If you have any questions, feel free to contact us at support@regulaforensics.com
