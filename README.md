# DocumentReader-Android

## <a name="run_sample"></a>Run Sample

1. Download repository
2. Get trial license for demo application at [licensing.regulaforensics.com](https://licensing.regulaforensics.com) (`regula.license` file).
3. Change bundle ID in demo application, specified during registration your license key.
4. Put your trial license key (`regula.license` file) in DocumentReader-sample/app/src/main/res/raw folder
5. Open DocumentReader-sample using Android Studio Open an existing Android Studio project
6. Select appropriate build variant and run application.

## <a name="instal_reader"></a> Install DocumentReader in custom applications

DocumentReader libraries are [available on our Maven repository](http://maven.regulaforensics.com/RegulaDocumentReader/com/regula/documentreader/). To install
them, simply add the following lines to your build.gradle:

```java
compile 'com.regula.documentreader:artifact_id:+@aar'	//possible artifact_id: core, bounds; Depends on received license
compile ('com.regula.documentreader:api:+aar'){
	transitive = true
}
```

## <a name="additional_information"></a> Additional information
If you have any questions, feel free to contact us at support@regulaforensics.com
