# JSpeedTest

[![Build Status](https://travis-ci.org/akinaru/speed-test-lib.svg?branch=master)](https://travis-ci.org/akinaru/speed-test-lib)
[![Download](https://api.bintray.com/packages/akinaru/maven/speedtest/images/download.svg) ](https://bintray.com/akinaru/maven/speedtest/_latestVersion)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.akinaru/speedtest/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.akinaru/speedtest)
[![Coverage Status](https://coveralls.io/repos/github/akinaru/speed-test-lib/badge.svg?branch=master)](https://coveralls.io/github/akinaru/speed-test-lib?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/55e8e347e0d24566b37fe43799665e40)](https://www.codacy.com/app/bmartel.fr/speed-test-lib?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=akinaru/speed-test-lib&amp;utm_campaign=Badge_Grade)
[![Javadoc](http://javadoc-badge.appspot.com/com.github.akinaru/speedtest.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.github.akinaru/speedtest)
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

```
compile 'com.github.akinaru:speedtest:1.23'
```

## Usage

* setup a speed test listener to monitor progress, completion and error catch :

```
SpeedTestSocket speedTestSocket = new SpeedTestSocket();

// add a listener to wait for speedtest completion and progress
speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

    @Override
    public void onDownloadFinished(SpeedTestReport report) {
        // called when download is finished
        System.out.println("[DL FINISHED] rate in octet/s : " + report.getTransferRateOctet());
        System.out.println("[DL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
    }

    @Override
    public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
         // called when a download error occur
    }

    @Override
    public void onUploadFinished(SpeedTestReport report) {
        // called when an upload is finished
        System.out.println("[UL FINISHED] rate in octet/s : " + report.getTransferRateOctet());
        System.out.println("[UL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
    }

    @Override
    public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
        // called when an upload error occur
    }

    @Override
    public void onDownloadProgress(float percent, SpeedTestReport report) {
        // called to notify download progress
        System.out.println("[DL PROGRESS] progress : " + percent + "%");
        System.out.println("[DL PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
        System.out.println("[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
    }

    @Override
    public void onUploadProgress(float percent, SpeedTestReport report) {
        // called to notify upload progress
        System.out.println("[UL PROGRESS] progress : " + percent + "%");
        System.out.println("[UL PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
        System.out.println("[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
    }

    @Override
    public void onInterruption() {
        // triggered when forceStopTask is called
    }
});
```

### Download

* HTTP download 1Mo from `2.testdebit.info`

```
speedTestSocket.startDownload("2.testdebit.info", "/fichiers/1Mo.dat");
```

* FTP download 1Mo from `speedtest.tele2.net`

```
speedTestSocket.startFtpDownload("speedtest.tele2.net", "/1MB.zip");
```

### Upload

* HTTP upload 1Mo to `2.testdebit.info`

```
speedTestSocket.startUpload("2.testdebit.info", "/", 1000000);
```

* FTP upload a 1Mo file to `speedtest.tele2.net`

```
String fileName = SpeedTestUtils.generateFileName() + ".txt";
speedTestSocket.startFtpUpload("speedtest.tele2.net", "/upload/" + fileName, 1000000);
```

### Fixed duration download

Download during a fixed duration. Download will be stopped when the max duration is reached.
At the end of the max duration, `onInterruption` is called if download has not be fully completed

* HTTP download for 10s max, a 100 Mo file from `2.testdebit.info`

```
speedTestSocket.startFixedDownload("2.testdebit.info", "/fichiers/100Mo.dat", 10000);
```

* FTP download for 10s max, a 100 Mo file from `speedtest.tele2.net`

```
speedTestSocket.startFtpFixedDownload("speedtest.tele2.net", "/100MB.zip");
```

### Fixed duration Upload

Upload during a fixed duration. Upload will be stopped when the max duration is reached
At the end of the max duration, `onInterruption` is called if upload has not be fully completed

* HTTP upload for 10s max, a 10Mo file to `2.testdebit.info`

```
speedTestSocket.startFixedUpload("2.testdebit.info", "/", 10000000, 10000);
```

* FTP upload for 10s max, a 10Mo file to `speedtest.tele2.net`

```
String fileName = SpeedTestUtils.generateFileName() + ".txt";
speedTestSocket.startFtpFixedUpload("speedtest.tele2.net", "/upload/" + fileName, 10000000, 10000);
```

### Define report interval

You can define your own report interval (interval between each `onDownloadProgress` & `onUploadProgress`) in milliseconds.

* HTTP download with download reports each 1.5 seconds

```
speedTestSocket.startDownload("2.testdebit.info", "/fichiers/1Mo.dat", 1500);
```

* FTP download with download reports each 1.5 seconds

```
speedTestSocket.startFtpDownload("speedtest.tele2.net", "/1MB.zip", 1500);
```

* HTTP upload with upload reports each 1.5 seconds

```
speedTestSocket.startUpload("2.testdebit.info", "/", 10000000, 1500);
```

* FTP upload with upload reports each 1.5 seconds

```
String fileName = SpeedTestUtils.generateFileName() + ".txt";
speedTestSocket.startFtpUpload("speedtest.tele2.net", "/upload/" + fileName, 10000000, 1500);
```

### Chain download/upload requests

You can chain multiple download/upload requests during a fixed duration. This way, there will be as much download/upload request until the end of the period

* download repeat

The following will download regularly for 20 seconds a file of 1Mo with download report each 2 seconds. Download reports will appear in `onReport` callback of `IRepeatListener` instead of `onDownloadProgress` :

```
speedTestSocket.startDownloadRepeat("2.testdebit.info", "/fichiers/1Mo.dat",
    20000, 2000, new
            IRepeatListener() {
                @Override
                public void onFinish(final SpeedTestReport report) {
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

```
speedTestSocket.startUploadRepeat("2.testdebit.info", "/", 1000000
    20000, 2000, new
            IRepeatListener() {
                @Override
                public void onFinish(final SpeedTestReport report) {
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
```
SpeedTestReport getLiveDownloadReport()
```

* retrieve current upload report : 
```
SpeedTestReport getLiveUploadReport()
```

### Set upload file storage type

By default, data to be uploaded is stored in RAM, for large data it is recommended to used file storage : 

```
speedTestSocket.setUploadStorageType(UploadStorageType.FILE_STORAGE);
```

It will create a temporary file containing random data. File will be deleted automatically at the end of the upload.

### Set size of each packet sent to upload server

```
speedTestSocket.setUploadChunkSize(65535);
```

### Set socket timeout value

You can set download/upload socket timeout in milliseconds :

```
speedTestSocket.setSocketTimeout(5000);
```

### Set transfer rate precision

These settings are used to alter transfer rate float rounding / scale :

* set RoundingMode :

```
speedTestSocket.setDefaultRoundingMode(RoundingMode.HALF_EVEN);
```
Default `RoundingMode` used for transfer rate calculation is `HALF_EVEN`. It can be override with : 

* set Scale :

```
speedTestSocket.setDefaultScale(4);
```
Default scale used for transfer rate calculation is 4

## Android Integration

* add Internet permission to manifest : 
```
<uses-permission android:name="android.permission.INTERNET" />
```

* use an `AsyncTask` to run your speed test :

```
public class SpeedTestTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... params) {

        SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadFinished(SpeedTestReport report) {
                // called when download is finished
                Log.v("speedtest", "[DL FINISHED] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[DL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
                // called when a download error occur
            }

            @Override
            public void onUploadFinished(SpeedTestReport report) {
                // called when an upload is finished
                Log.v("speedtest", "[UL FINISHED] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[UL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
                // called when an upload error occur
            }

            @Override
            public void onDownloadProgress(float percent, SpeedTestReport report) {
                // called to notify download progress
                Log.v("speedtest", "[DL PROGRESS] progress : " + percent + "%");
                Log.v("speedtest", "[DL PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onUploadProgress(float percent, SpeedTestReport report) {
                // called to notify upload progress
                Log.v("speedtest", "[UL PROGRESS] progress : " + percent + "%");
                Log.v("speedtest", "[UL PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onInterruption() {
                // triggered when forceStopTask is called
            }
        });

        speedTestSocket.startDownload("2.testdebit.info", "/fichiers/1Mo.dat");

        return null;
    }
}
```

Execute it with : `new SpeedTestTask().execute();`

## Features examples

All following examples use speed test server `1.testdebit.info` for HTTP and `speedtest.tele2.net` for FTP

* HTTP download (1Mo)

```
./gradlew downloadFile
```

* HTTP upload (1Mo)

```
./gradlew uploadFile
```

* FTP download (1Mo)

```
./gradlew downloadFTP
```

* FTP upload (1Mo)

```
./gradlew uploadFTP
```

* download during a fixed duration (size: 100Mo, duration: 15s, report interval: 1s)

```
./gradlew fixedDownload
```

* upload during a fixed duration (size: 100Mo, duration: 15s, report interval: 1s)

```
./gradlew fixedUpload
```

* download repeatedly a file during a fixed duration (size:10Mo, duration 11s, report interval: 1s)

```
./gradlew repeatDownload
```

* upload repeatedly a file during a fixed duration (size:1Mo, duration 11s, report interval: 1s)

```
./gradlew repeatUpload
```

* successive 2 x (download + upload) repeatedly a file during a fixed duration (1 download size:1Mo, duration 3s, report interval: 1s following by 1 upload size:1Mo, duration 3s, report interval: 1s)

```
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

```
./gradlew clean build -x test
```

* build with test :

```
./gradlew clean build
```

* run specific test

```
./gradlew test --tests "fr.bmartel.speedtest.test.SpeedTestFunctionalTest"
```

## External libraries

* [http-endec](https://github.com/akinaru/http-endec)
* [Apache Commons Net](https://commons.apache.org/proper/commons-net/)

## License

The MIT License (MIT) Copyright (c) 2016 Bertrand Martel
