# JSpeedTest

[![Build Status](https://travis-ci.org/bertrandmartel/speed-test-lib.svg?branch=master)](https://travis-ci.org/bertrandmartel/speed-test-lib)
[![Download](https://api.bintray.com/packages/bertrandmartel/maven/speedtest/images/download.svg) ](https://bintray.com/bertrandmartel/maven/speedtest/_latestVersion)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/fr.bmartel/jspeedtest/badge.svg)](https://maven-badges.herokuapp.com/maven-central/fr.bmartel/jspeedtest)
[![Coverage Status](https://coveralls.io/repos/github/bertrandmartel/speed-test-lib/badge.svg?branch=master)](https://coveralls.io/github/bertrandmartel/speed-test-lib?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/55e8e347e0d24566b37fe43799665e40)](https://www.codacy.com/app/bertrandmartel/speed-test-lib?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=bertrandmartel/speed-test-lib&amp;utm_campaign=Badge_Grade)
[![Javadoc](http://javadoc-badge.appspot.com/fr.bmartel/jspeedtest.svg?label=javadoc)](http://javadoc-badge.appspot.com/fr.bmartel/jspeedtest)
[![License](http://img.shields.io/:license-mit-blue.svg)](LICENSE.md)

Speed Test client library for Java/Android with HTTP & FTP support

* speed test download
* speed test upload
* download / upload progress monitoring
* configurable hostname / port / uri (username & password for FTP)
* configurable socket timeout and chunk size
* configure upload file storage

Check a [non-exhaustive list](./server_list.md) of compatible speed test server.

## Include in your project

* with Gradle, from jcenter or mavenCentral :

```gradle
compile 'fr.bmartel:jspeedtest:1.32.1'
```

## Usage

* setup a speed test listener to monitor progress, completion and error catch :

```java
SpeedTestSocket speedTestSocket = new SpeedTestSocket();

// add a listener to wait for speedtest completion and progress
speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

    @Override
    public void onCompletion(SpeedTestReport report) {
        // called when download/upload is complete
        System.out.println("[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
        System.out.println("[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
    }

    @Override
    public void onError(SpeedTestError speedTestError, String errorMessage) {
         // called when a download/upload error occur
    }

    @Override
    public void onProgress(float percent, SpeedTestReport report) {
        // called to notify download/upload progress
        System.out.println("[PROGRESS] progress : " + percent + "%");
        System.out.println("[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
        System.out.println("[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
    }
});
```

### Download

* HTTP download 1Mo from `http://ipv4.ikoula.testdebit.info`

```java
speedTestSocket.startDownload("http://ipv4.ikoula.testdebit.info/1M.iso");
```

* FTP download 1Mo from `speedtest.tele2.net`

```java
speedTestSocket.startDownload("ftp://speedtest.tele2.net/1MB.zip");
```

* FTP download 1Mo from `ftp.otenet.gr` with credentials (username/password), default is anonymous/no password

```java
speedTestSocket.startDownload("ftp://speedtest:speedtest@ftp.otenet.gr/test1Mb.db");
```

### Upload

* HTTP upload 1Mo to `http://ipv4.ikoula.testdebit.info`

```java
speedTestSocket.startUpload("http://ipv4.ikoula.testdebit.info/", 1000000);
```

* FTP upload a 1Mo file to `speedtest.tele2.net`

```java
String fileName = SpeedTestUtils.generateFileName() + ".txt";
speedTestSocket.startUpload("ftp://speedtest.tele2.net/upload/" + fileName, 1000000);
```

### Fixed duration download

Download during a fixed duration. Download will be stopped when the max duration is reached.

* HTTP download for 10s max, a 100 Mo file from `http://ipv4.ikoula.testdebit.info`

```java
speedTestSocket.startFixedDownload("http://ipv4.ikoula.testdebit.info/100M.iso", 10000);
```

* FTP download for 10s max, a 100 Mo file from `speedtest.tele2.net`

```java
speedTestSocket.startFixedDownload("ftp://speedtest.tele2.net/100MB.zip");
```

### Fixed duration Upload

Upload during a fixed duration. Upload will be stopped when the max duration is reached

* HTTP upload for 10s max, a 10Mo file to `http://ipv4.ikoula.testdebit.info`

```java
speedTestSocket.startFixedUpload("http://ipv4.ikoula.testdebit.info/", 10000000, 10000);
```

* FTP upload for 10s max, a 10Mo file to `speedtest.tele2.net`

```java
String fileName = SpeedTestUtils.generateFileName() + ".txt";
speedTestSocket.startFixedUpload("ftp://speedtest.tele2.net/upload/" + fileName, 10000000, 10000);
```

### Define report interval

You can define your own report interval (interval between each `onDownloadProgress` & `onUploadProgress`) in milliseconds.

* HTTP download with download reports each 1.5 seconds

```java
speedTestSocket.startDownload("http://ipv4.ikoula.testdebit.info/1M.iso", 1500);
```

* FTP download with download reports each 1.5 seconds

```java
speedTestSocket.startDownload("ftp://speedtest.tele2.net/1MB.zip", 1500);
```

* HTTP upload with upload reports each 1.5 seconds

```java
speedTestSocket.startUpload("http://ipv4.ikoula.testdebit.info/", 10000000, 1500);
```

* FTP upload with upload reports each 1.5 seconds

```java
String fileName = SpeedTestUtils.generateFileName() + ".txt";
speedTestSocket.startUpload("ftp://speedtest.tele2.net/upload/" + fileName, 10000000, 1500);
```

### Use proxy server

```java
speedTestSocket.setProxyServer("http://216.56.48.118:9000");
```

default proxy server port is 8080

### Chain download/upload requests

You can chain multiple download/upload requests during a fixed duration. This way, there will be as much download/upload request until the end of the period

* download repeat

The following will download regularly for 20 seconds a file of 1Mo with download report each 2 seconds. Download reports will appear in `onReport` callback of `IRepeatListener` instead of `onDownloadProgress` :

```java
speedTestSocket.startDownloadRepeat("http://ipv4.ikoula.testdebit.info/1M.iso",
    20000, 2000, new
            IRepeatListener() {
                @Override
                public void onCompletion(final SpeedTestReport report) {
                    // called when repeat task is finished
                }

                @Override
                public void onReport(final SpeedTestReport report) {
                    // called when a download report is dispatched
                }
            });
```

* upload repeat

The following will upload regularly for 20 seconds a file of 1Mo with download report each 2 seconds. Upload reports will appear in `onReport` callback of `IRepeatListener` instead of `onUploadProgress` :

```java
speedTestSocket.startUploadRepeat("http://ipv4.ikoula.testdebit.info/", 1000000
    20000, 2000, new
            IRepeatListener() {
                @Override
                public void onCompletion(final SpeedTestReport report) {
                    // called when repeat task is finished
                }

                @Override
                public void onReport(final SpeedTestReport report) {
                    // called when an upload report is dispatched
                }
            });
```

### Get live download & upload

* retrieve current download report : 

```java
SpeedTestReport getLiveReport()
```

* retrieve current upload report : 

```java
SpeedTestReport getLiveReport()
```

### Set setup time

Setup time is the amount of time in milliseconds from which speed test will be calculated :

The following will set the setup time to 5 seconds which mean, the speed rate will begin to be computed 5 seconds after the speed test start :

* download
```java
speedTestSocket.setDownloadSetupTime(5000);
```
* upload
```java
speedTestSocket.setUploadSetupTime(5000);
```

### Set upload file storage type

By default, data to be uploaded is stored in RAM, for large data it is recommended to used file storage : 

```java
speedTestSocket.setUploadStorageType(UploadStorageType.FILE_STORAGE);
```

It will create a temporary file containing random data. File will be deleted automatically at the end of the upload.

### Set size of each packet sent to upload server

```java
speedTestSocket.setUploadChunkSize(65535);
```

### Set socket timeout value

You can set download/upload socket timeout in milliseconds :

```java
speedTestSocket.setSocketTimeout(5000);
```

### Set transfer rate precision

These settings are used to alter transfer rate float rounding / scale :

* set RoundingMode :

```java
speedTestSocket.setDefaultRoundingMode(RoundingMode.HALF_EVEN);
```
Default `RoundingMode` used for transfer rate calculation is `HALF_EVEN`. It can be override with : 

* set Scale :

```java
speedTestSocket.setDefaultScale(4);
```
Default scale used for transfer rate calculation is 4

### FTP mode

Set passive/active mode with :

```java
speedTestSocket.setFtpMode(FtpMode.ACTIVE);
```

default is `FtpMode.PASSIVE`

## Android Integration

* add Internet permission to manifest : 

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

* use an `AsyncTask` to run your speed test :

```java
public class SpeedTestTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... params) {

        SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onCompletion(SpeedTestReport report) {
                // called when download/upload is finished
                Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                // called when a download/upload error occur
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                // called to notify download/upload progress
                Log.v("speedtest", "[PROGRESS] progress : " + percent + "%");
                Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            }
        });

        speedTestSocket.startDownload("http://ipv4.ikoula.testdebit.info/1M.iso");

        return null;
    }
}
```

Execute it with : `new SpeedTestTask().execute();`

## Features examples

All following examples use speed test server `http://ipv4.ikoula.testdebit.info` for HTTP and `speedtest.tele2.net` for FTP

* HTTP download (1Mo)

```bash
./gradlew downloadFile
```

* HTTP upload (1Mo)

```bash
./gradlew uploadFile
```

* FTP download (1Mo)

```bash
./gradlew downloadFTP
```

* FTP upload (1Mo)

```bash
./gradlew uploadFTP
```

* HTTP download (1Mo) through proxy server 216.56.48.118:9000 

```bash
./gradlew downloadFileProxy
```

* download during a fixed duration (size: 100Mo, duration: 15s, report interval: 1s)

```bash
./gradlew fixedDownload
```

* upload during a fixed duration (size: 100Mo, duration: 15s, report interval: 1s)

```bash
./gradlew fixedUpload
```

* download repeatedly a file during a fixed duration (size:10Mo, duration 11s, report interval: 1s)

```bash
./gradlew repeatDownload
```

* upload repeatedly a file during a fixed duration (size:1Mo, duration 11s, report interval: 1s)

```bash
./gradlew repeatUpload
```

* successive 2 x (download + upload) repeatedly a file during a fixed duration (1 download size:1Mo, duration 3s, report interval: 1s following by 1 upload size:1Mo, duration 3s, report interval: 1s)

```bash
./gradlew repeatChain
```

## Speed Test issues

It's important to choose an adequate speed test server depending on latency/jitter. This library is **not** responsible for the speed test server choice.

Note that this library :
* doesn't adjust the chunk size depending on the connection speed either
* doesn't provide pre-estimation of the connection speed based on small chunk sent to/from server
* doesn't detect anomaly either (for instance taking away X% slowest chunk and X% fastest chunk downloaded)

This library does provide an average of transfer rate for all individual chunks read/written for download/upload.

The 2 following links describe the process of speedtest.net :
* http://www.ookla.com/support/a21110547/what-is-the-test-flow-and-methodology-for-the-speedtest
* https://support.speedtest.net/hc/en-us/articles/203845400-How-does-the-test-itself-work-How-is-the-result-calculated-

## Compatibility

JRE 1.7 compliant

## Build & test

* build without test :

```bash
./gradlew clean build -x test
```

* build with test :

```bash
./gradlew clean build
```

* run specific test

```bash
./gradlew test --tests "fr.bmartel.speedtest.test.SpeedTestFunctionalTest"
```

## External libraries

* [http-endec](https://github.com/bertrandmartel/http-endec)
* [Apache Commons Net](https://commons.apache.org/proper/commons-net/)

## License

The MIT License (MIT) Copyright (c) 2016-2018 Bertrand Martel
