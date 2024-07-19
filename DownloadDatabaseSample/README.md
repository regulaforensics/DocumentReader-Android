# Regula Document Reader (Android version)
`DownloadDatabaseSample` folder contains the sample project with database features.

# Content
* [How to build the demo application](#how-to-build-the-demo-application)
* [Troubleshooting license issues](#troubleshooting-license-issues)
* [Documentation](#documentation)
* [Additional information](#additional-information)

## How to build the demo application
1. Visit our [Client Portal](https://client.regulaforensics.com/) to get a trial license (`regula.license` file). The license creation wizard will guide you through the necessary steps.
2. Download or clone the current repository using the command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
3. Open the `DownloadDatabaseSample` project in an IDE.
4. Copy the license file to the project: `DownloadDatabaseSample/app/src/main/assets/`.
5. Optionaly: Copy the database file db.dat from [Client Portal](https://client.regulaforensics.com/customer/databases) to the project: `DownloadDatabaseSample/app/src/main/assets/Regula/`.
6. Change the application ID to the one you have specified during the registration at [Client Portal](https://client.regulaforensics.com/).
7. Run the project.
Note: Android Gradle plugin requires Java 11 to run

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
