# Document Reader SDK Certificate Pinning Sample Project (Android)

* [Overview](#overview)
* [Configuration of Certificate Pinning](#configuration-of-certificate-pinning)
* [Documentation](#documentation)
* [Demo Application](#demo-application)
* [Technical Support](#technical-support)
* [Business Enquiries](#business-enquiries)

## Overview

Sample project in Kotlin, demonstrating how to set up and use the <a target="_blank" href="https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile/security/certificate-pinning/">Certificate Pinning</a> feature.

## Configuration of Certificate Pinning

To generate the key for the Android app, you can follow the <a target="_blank" href="https://nikunj-joshi.medium.com/ssl-pinning-increase-server-identity-trust-656a2fc7e22b">example instructions</a>.

1. In the mobile app you need to create `xml` file (`main/res/xml/network_security_config`)
2. In the `AndroidManifest.xml` you need to set up config above in the application area:
   `android:networkSecurityConfig="@xml/network_security_config"`
3. In the `network-security-config` you need to replace `domain` with your value and replace `SHA-256`.

**Note:** Android Gradle plugin requires Java 11 to run.

## Documentation

<a target="_blank" href="https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile/">Document Reader SDK Mobile Documentation</a>

## Demo Application

<a target="_blank" href="https://play.google.com/store/apps/details?id=com.regula.documentreader">Regula Document Reader Android Demo Application on Google Play</a>

## Technical Support

To submit a request to the Support Team, visit <a target="_blank" href="https://support.regulaforensics.com/hc/en-us/requests/new?utm_source=github">Regula Help Center</a>.

## Business Enquiries

To discuss business opportunities, fill the <a target="_blank" href="https://explore.regula.app/docs-support-request">Enquiry Form</a> and specify your scenarios, applications, and technical requirements.