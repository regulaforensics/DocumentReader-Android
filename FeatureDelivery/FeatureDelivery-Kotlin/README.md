# Document Reader SDK Sample Project with Feature Delivery (Android)

* [Overview](#overview)
* [Installation](#installation)
* [Troubleshooting](#troubleshooting)
* [Documentation](#documentation)
* [Demo Application](#demo-application)
* [Technical Support](#technical-support)
* [Business Enquiries](#business-enquiries)

## Overview

Sample project in Kotlin, demonstrating how to optimize the application size by <a traget="_blank" href="https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile/optimization/android/feature-delivery/">loading Regula Core framework in runtime</a>, using Google's <a target="_blank" href="https://developer.android.com/guide/playcore/feature-delivery">Play Feature Delivery</a>.

## Installation

1. Download or clone the current repository using the command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`.
2. Open the `FeatureDelivery-Kotlin` project in an IDE.
3. Add license and database files to the project:
   - Visit [Regula Client Portal](https://client.regulaforensics.com) to get a trial license (`regula.license` file). The license creation wizard will guide you through the necessary steps. 
   - Copy the license file to the project: `FeatureDelivery/FeatureDelivery-Kotlin/app/src/main/res/raw/`
   - Copy the database file `db.dat` from [Regula Client Portal](https://client.regulaforensics.com/customer/databases) to the project: `FeatureDelivery/FeatureDelivery-Kotlin/app/src/main/assets/Regula/`
4. Change the application ID to the one you have specified during the registration at [Regula Client Portal](https://client.regulaforensics.com).
5. Run the project.

**Note:** Android Gradle plugin requires Java 11 to run.

## Troubleshooting

If you have issues with the license verification when running the application, check the following:

1. The OS, which you use, is specified in the license (Android).
2. The application ID, which you use, is specified in the license.
3. The license is valid (not expired).
4. The date and time on the device, where you run the application, are valid.
5. You use the latest release version of the Document Reader SDK.
6. You placed the license into the project as described in the [Installation](#installation) section.

## Documentation

<a target="_blank" href="https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile/">Document Reader SDK Mobile Documentation</a>

## Demo Application

<a target="_blank" href="https://play.google.com/store/apps/details?id=com.regula.documentreader">Regula Document Reader Android Demo Application on Google Play</a>

## Technical Support

To submit a request to the Support Team, visit <a target="_blank" href="https://support.regulaforensics.com/hc/en-us/requests/new?utm_source=github">Regula Help Center</a>.

## Business Enquiries

To discuss business opportunities, fill the <a target="_blank" href="https://explore.regula.app/docs-support-request">Enquiry Form</a> and specify your scenarios, applications, and technical requirements.