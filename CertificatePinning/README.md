# Regula Document Reader (Android version)
The `CertificatePinning` sample project shows how configure Certificate Pinning in the Android app

# Content
* [How to configure Certificate Pinning](#how-to-configure-Certificate-Pinning)
* [Documentation](#documentation)
* [Additional information](#additional-information)

## How to configure Certificate Pinning

Here you can find how to generate key for the android app
https://nikunj-joshi.medium.com/ssl-pinning-increase-server-identity-trust-656a2fc7e22b

1. In the mobile app you need to create xml file (main/res/xml/network_security_config)
2. In the AndroidManifest.xml you need to set up config above in the application area:
   android:networkSecurityConfig="@xml/network_security_config"
3. In the network-security-config you need to replace 'domain' to your and replace 'SHA-256'

Note: Android Gradle plugin requires Java 11 to run

## Documentation
The documentation on the SDK can be found [here](https://docs.regulaforensics.com/develop/doc-reader-sdk/mobile?utm_source=github).

## Additional information
If you have any technical questions or suggestions, feel free to [contact](mailto:android.support@regulaforensics.com) us or create an issue [here](https://github.com/regulaforensics/DocumentReader-Android/issues).

To use our SDK in your own app you have to [purchase](https://pipedrivewebforms.com/form/5f1d771cbe4f844a1f78f8a06fbf94361841159) a commercial license.
