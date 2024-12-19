# BLE Device without Hardware License Sample Project (Android)

* [Overview](#overview)
* [Installation](#installation)
* [Documentation](#documentation)
* [Demo Application](#demo-application)
* [Technical Support](#technical-support)
* [Business Enquiries](#business-enquiries)

## Overview

Sample project in Kotlin, demonstrating the Document Reader SDK integration with the device <a target="_blank" href="https://docs.regulaforensics.com/develop/1120/">Mobile Document Authenticator Regula 1120</a> without the hardware license inside it.

In this case, to run the project, it needs to be initialized with a proper software license in the application.

## Installation

1. Download or clone the current repository using the command `git clone https://github.com/regulaforensics/DocumentReader-Android.git`
2. Open the `1120-Without License` project in an IDE.
3. Add license and database files to the project:
   - Visit [Regula Client Portal](https://client.regulaforensics.com/) to get a trial license (`regula.license` file). The license creation wizard will guide you through the necessary steps.
   - Copy the license file to the project: `1120-Without License/app/src/main/res/raw/`
   - Copy the database file `db.dat` from [Client Portal](https://client.regulaforensics.com/customer/databases) to the project: `1120-Without License/app/src/main/assets/Regula/`
4. Change the application ID to the one you have specified during the registration at [Client Portal](https://client.regulaforensics.com/).
5. Run the project.
   
**Note:** Android Gradle plugin requires Java 11 to run.

## Documentation

<a target="_blank" href="https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile/">Document Reader SDK Mobile Documentation</a>

## Demo Application

<a target="_blank" href="https://play.google.com/store/apps/details?id=com.regula.documentreader">Regula Document Reader Android Demo Application on Google Play</a>

## Technical Support

To submit a request to the Support Team, visit <a target="_blank" href="https://support.regulaforensics.com/hc/en-us/requests/new?utm_source=github">Regula Help Center</a>.

## Business Enquiries

To discuss business opportunities, fill the <a target="_blank" href="https://explore.regula.app/docs-support-request">Enquiry Form</a> and specify your scenarios, applications, and technical requirements.