# Regula Document Reader (Android version)
`1120` folder contains the sample project for working with the [Mobile document authenticator Regula 1120](https://regulaforensics.com/en/products/machine_verification/1120/).

# Content
* [How to build the demo application](#how-to-build-the-demo-application)
* [Documentation](#documentation)
* [Additional information](#additional-information)

## How to build the demo application
1. Visit our [Client Portal](https://client.regulaforensics.com/) to get a trial license (`regula.license` file). The license creation wizard will guide you through the necessary steps.
2. Download or clone the current repository using the command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
3. Open the `1120-Without License` project in an IDE.
4. Copy the license file to the project: `1120-Without License/app/src/main/res/raw/`.
5. Copy the database file db.dat from [Client Portal](https://client.regulaforensics.com/customer/databases) to the project: `1120-Without License/app/src/main/assets/Regula/`.
6. Change the application ID to the one you have specified during the registration at [Client Portal](https://client.regulaforensics.com/).
7. Run the project.
   Note: Android Gradle plugin requires Java 11 to run

## Documentation
The documentation on the SDK can be found [here](https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile?utm_source=github).

## Additional information
If you have any technical questions or suggestions, feel free to [contact](mailto:android.support@regulaforensics.com) us or create an issue [here](https://github.com/regulaforensics/DocumentReader-Android/issues).
