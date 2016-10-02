# Speed Test library

http://akinaru.github.io/speed-test-lib

[![Build Status](https://travis-ci.org/akinaru/speed-test-lib.svg?branch=master)](https://travis-ci.org/akinaru/speed-test-lib)
[![Download](https://api.bintray.com/packages/akinaru/maven/speedtest/images/download.svg) ](https://bintray.com/akinaru/maven/speedtest/_latestVersion)
[![Coverage Status](https://coveralls.io/repos/github/akinaru/speed-test-lib/badge.svg?branch=master)](https://coveralls.io/github/akinaru/speed-test-lib?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/55e8e347e0d24566b37fe43799665e40)](https://www.codacy.com/app/bmartel.fr/speed-test-lib?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=akinaru/speed-test-lib&amp;utm_campaign=Badge_Grade)
[![Javadoc](http://javadoc-badge.appspot.com/com.github.akinaru/speedtest.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.github.akinaru/speedtest)
[![License](http://img.shields.io/:license-mit-blue.svg)](LICENSE.md)

Speed Test library for Java / Android :

* speed test download with transfer rate output
* speed test upload with transfer rate output
* download and upload progress monitoring
* speed test server / port / uri can be configured easily

<hr/>

* For download process, library will download file from given speed test server parameters and calculate transfer rate
* For upload process, library will generate a random file with a given size and will upload this file to a server calculating transfer rate

No external file are required and no file are stored in Hard Disk.

## Include in your project

* with Gradle, from jcenter :

```
compile 'com.github.akinaru:speedtest:1.16'
```

## How to use ?

#### Instanciate SpeedTest class

```
SpeedTestSocket speedTestSocket = new SpeedTestSocket();
```
#### Add a listener to monitor

* download process result with ``onDownloadPacketsReceived`` callback
* upload process result with ``onUploadPacketsReceived`` callback
* download progress with ``onDownloadProgress`` callback
* upload progress with ``onUploadProgress`` callback
* download error catch with ``onDownloadError``
* upload error catch with ``onUploadError``

```
speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

	@Override
	public void onDownloadPacketsReceived(long packetSize, float transferRateBitPerSeconds,
                                          float transferRateOctetPerSeconds) {
		System.out.println("download transfer rate  : " + transferRateBitPerSeconds + " bps");
		System.out.println("download transfer rate  : " + transferRateOctetPerSeconds + "Bps");
	}

	@Override
	public void onDownloadError(SpeedTestError errorCode, String message) {
		System.out.println("Download error " + errorCode + " occured with message : " + message);
	}

	@Override
	public void onUploadPacketsReceived(int packetSize, float transferRateBitPerSeconds, 
                                        float transferRateOctetPerSeconds) {
		System.out.println("download transfer rate  : " + transferRateBitPerSeconds + " bps");
		System.out.println("download transfer rate  : " + transferRateOctetPerSeconds + "Bps");
	}

	@Override
	public void onUploadError(SpeedTestError errorCode, String message) {
		System.out.println("Upload error " + errorCode + " occured with message : " + message);
	}

	@Override
	public void onDownloadProgress(float percent, SpeedTestReport downloadReport) {
	}

	@Override
	public void onUploadProgress(float percent, SpeedTestReport uploadReport) {
	}

});

```

#### Start Download speed test

Download a single file from a server : 

``void startDownload(String hostname, int port, String uri)``

| params     |  type     |       description                    |
|------------|-----------|--------------------------------------|
| `hostname` |  String   | server hostname                      |  
| `port`     |  int      | server port                          |
| `uri`      |  String   | uri to fetch your file from server   |  

```
speedTestSocket.startDownload("ipv4.intuxication.testdebit.info", 80,"/fichiers/10Mo.dat");
```
You can wait for test completion with ``closeSocketJoinRead()`` which is prefered to ``closeSocket()`` since it join reading thread before resuming application.

```
speedTestSocket.closeSocketJoinRead();
```

#### Start Upload speed test

Upload a single file with specified size to a server :

```
void startUpload(String hostname, int port, String uri, int fileSizeOctet)
```

| params     |  type     |       description                    |
|------------|-----------|--------------------------------------|
| `hostname` |  String   | server hostname                      |  
| `port`     |  int      | server port                          |
| `uri`      |  String   | uri to fetch your file from server   |  
| `fileSizeOctet`     |  int      | the file size to be uploaded to server (file will be generated randomly and sent to speed test server)                          |

Here is an example for a file of 10Moctet :
```
speedTestSocket.startUpload("1.testdebit.info", 80, "/", 10000000);
```
### Download/Upload during a fix amount of time

If you want to download/upload during a fix value, you can begin download/upload and then invoke : 

```
speedTestSocket.forceStopTask();
```

The following will start downloading a file of 100Mo but will stop downloading 15 seconds later : 

```
final Timer timer = new Timer();

/* instanciate speed test */
final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

/* add a listener to wait for speed test completion and progress */
speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

    @Override
    public void onDownloadPacketsReceived(long packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {
    }

    @Override
    public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
        if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
            if (timer != null) {
                timer.purge();
                timer.cancel();
            }
        }
    }

    @Override
    public void onUploadPacketsReceived(long packetSize, float transferRateBitPerSeconds, 
                                        float transferRateOctetPerSeconds) {
    }

    @Override
    public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
        if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
            if (timer != null) {
                timer.purge();
                timer.cancel();
            }
        }
    }

    @Override
    public void onDownloadProgress(float percent, SpeedTestReport downloadReport) {
    }

    @Override
    public void onUploadProgress(float percent, SpeedTestReport uploadReport) {
    }
});

TimerTask stopTask = new TimerTask() {
    @Override
    public void run() {
        System.out.println("--------------- FINISH REPORT -----------------------------");
        SpeedTestReport downloadReport = speedTestSocket.getLiveDownloadReport();
        System.out.println("---------------current download report--------------------");
        System.out.println("progress             : " + downloadReport.getProgressPercent() + "%");
        System.out.println("transfer rate bit    : " + downloadReport.getTransferRateBit() + "b/s");
        System.out.println("transfer rate octet  : " + downloadReport.getTransferRateOctet() + "B/s");
        System.out.println("downloaded for now   : " + downloadReport.getTemporaryPacketSize() + "/" + downloadReport.getTotalPacketSize());
        if (downloadReport.getStartTime() > 0) {
            System.out.println("amount of time   : " + 
                ((downloadReport.getReportTime() - downloadReport.getStartTime()) / 1000) + "s");
        }
        speedTestSocket.forceStopTask();
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }
};
timer.schedule(stopTask, 15000);
speedTestSocket.startDownload("1.testdebit.info", 80, "/fichiers/100Mo.dat");

```

### Default report interval

By default, progress report is achieved as following :

* for upload, a progress report is sent and trigger `onUploadProgress` each time `uploadChunkSize` number of packet is sent. The default value for `uploadChunkSize` is 65535 but you can change with `speedTestSocket.setUploadChunkSize(int chunkSize)`

* for download, a progress report is sent and trigger `onDownloadProgress` each time a chunk of data is read from the downlink socket

### Set your own report interval

If you want to set a custom report interval, you can use a task scheduled at fixed rate to retrieve report with `speedTestSocket.getLiveDownloadReport()` or `speedTestSocket.getLiveUploadReport()` depending if you want download or upload report.

The following will start uploading a file of 10Mo and request reports every 400ms :

```
final Timer timer = new Timer();

/* instanciate speed test */
final SpeedTestSocket speedTestSocket = new SpeedTestSocket();

/* add a listener to wait for speed test completion and progress */
speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

    @Override
    public void onDownloadPacketsReceived(long packetSize, float transferRateBitPerSeconds, 
                                          float transferRateOctetPerSeconds) {
    }

    @Override
    public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
        if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
            if (timer != null) {
                timer.purge();
                timer.cancel();
            }
        }
    }

    @Override
    public void onUploadPacketsReceived(long packetSize, float transferRateBitPerSeconds, 
                                        float transferRateOctetPerSeconds) {
    }

    @Override
    public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
        if (speedTestError != SpeedTestError.FORCE_CLOSE_SOCKET) {
            if (timer != null) {
                timer.purge();
                timer.cancel();
            }
        }
    }

    @Override
    public void onDownloadProgress(float percent, SpeedTestReport downloadReport) {
    }

    @Override
    public void onUploadProgress(float percent, SpeedTestReport uploadReport) {
    }
});

TimerTask task = new TimerTask() {

    @Override
    public void run() {

        if (speedTestSocket.getSpeedTestMode() == SpeedTestMode.UPLOAD) {

            SpeedTestReport uploadReport = speedTestSocket.getLiveUploadReport();
            System.out.println("---------------current upload report--------------------");
            System.out.println("progress             : " + uploadReport.getProgressPercent() + "%");
            System.out.println("transfer rate bit    : " + uploadReport.getTransferRateBit() + "b/s");
            System.out.println("transfer rate octet  : " + uploadReport.getTransferRateOctet() + "B/s");
            System.out.println("uploaded for now     : " + uploadReport.getTemporaryPacketSize() 
                + "/" + uploadReport.getTotalPacketSize());
            System.out.println("amount of time       : "  
                + ((uploadReport.getReportTime() - uploadReport.getStartTime()) / 1000) + "s");
            System.out.println("--------------------------------------------------------");

        } else if (speedTestSocket.getSpeedTestMode() == SpeedTestMode.DOWNLOAD) {

            SpeedTestReport downloadReport = speedTestSocket.getLiveDownloadReport();
            System.out.println("---------------current download report--------------------");
            System.out.println("progress             : " + downloadReport.getProgressPercent() + "%");
            System.out.println("transfer rate bit    : " + downloadReport.getTransferRateBit() + "b/s");
            System.out.println("transfer rate octet  : " + downloadReport.getTransferRateOctet() + "B/s");
            System.out.println("downloaded for now   : " + downloadReport.getTemporaryPacketSize() 
                + "/" + downloadReport.getTotalPacketSize());
            System.out.println("amount of time       : "
                + ((downloadReport.getReportTime() - downloadReport.getStartTime()) / 1000) + "s");
        }
    }
};

// scheduling the task at interval
timer.scheduleAtFixedRate(task, 0, 400);

speedTestSocket.startUpload("1.testdebit.info", 80, "/", 10000000);
```

### Chain download/upload requests

It is possible to chain download/upload requests with `startDownloadRepeat` & `startUploadRepeat` API during a fixed time called `repeatWindow` with a report interval named `reportPeriodMillis`

*  chain download requests

```
/**
 * Start repeat download task.
 *
 * @param hostname           server hostname
 * @param port               server port
 * @param uri                uri to fetch to download file
 * @param repeatWindow       time window for the repeated download in milliseconds
 * @param reportPeriodMillis time interval between each report in milliseconds
 * @param repeatListener     listener for download repeat task completion & reports
 */
public void startDownloadRepeat(final String hostname,
                                final int port,
                                final String uri,
                                final int repeatWindow,
                                final int reportPeriodMillis,
                                final IRepeatListener repeatListener)
```

* chain upload requests

```
/**
 * Start repeat upload task.
 *
 * @param hostname           server hostname
 * @param port               server port
 * @param uri                uri to fetch to download file
 * @param repeatWindow       time window for the repeated upload in milliseconds
 * @param reportPeriodMillis time interval between each report in milliseconds
 * @param repeatListener     listener for upload repeat task completion & reports
 */
public void startUploadRepeat(final String hostname,
                              final int port,
                              final String uri,
                              final int repeatWindow,
                              final int reportPeriodMillis,
                              final int fileSizeOctet,
                              final IRepeatListener repeatListener)
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
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadPacketsReceived(int packetSize, 
            									float transferRateBitPerSeconds, 
            									float transferRateOctetPerSeconds) {
                Log.i("speed-test-app","download transfer rate  : " + transferRateOctetPerSeconds + "Bps");
            }

            @Override
            public void onDownloadError(SpeedTestError errorCode, String message) {
                Log.i("speed-test-app","Download error " + errorCode + " occured with message : " + message);
            }

            @Override
            public void onUploadPacketsReceived(int packetSize, 
            									float transferRateBitPerSeconds, 
            									float transferRateOctetPerSeconds) {
                Log.i("speed-test-app","download transfer rate  : " + transferRateOctetPerSeconds + "Bps");
            }

            @Override
            public void onUploadError(SpeedTestError errorCode, String message) {
                Log.i("speed-test-app","Upload error " + errorCode + " occured with message : " + message);
            }

            @Override
            public void onDownloadProgress(float percent,SpeedTestReport downloadReport) {
            }

            @Override
            public void onUploadProgress(float percent,SpeedTestReport uploadReport) {
            }

        });

        speedTestSocket.startUpload("1.testdebit.info", 
        							80, "/", 10000000); //will block until upload is finished

        return null;
    }
}
```

Execute it with : `new SpeedTestTask().execute();`

## JavaDoc

<a href="https://oss.sonatype.org/service/local/repositories/releases/archive/com/github/akinaru/speedtest/1.11/speedtest-1.11-javadoc.jar/!/index.html">javadoc can be found here</a>

## Features examples

All following examples use speed test server `1.testdebit.info`

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

## Compatibility

JRE 1.7 compliant

## Build & test

```
./gradlew clean build -x test
```

## Run specific test

for example :
```
./gradlew test --tests "fr.bmartel.speedtest.examples.SpeedTestFunctionalTest"
```

## External libraries

* https://github.com/akinaru/http-endec

## SpeedTest Server tested

* https://testdebit.info/

## Tutorial 

* [Tutorial on how to integrate Speed Test library into your project](http://www.hirunawijesinghe.com/integrating-akinarus-speed-test-lib-into-android/) by Hiruna Wijesinghe

## License

The MIT License (MIT) Copyright (c) 2016 Bertrand Martel
