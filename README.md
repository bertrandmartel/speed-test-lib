# Speed Test library

[![Build Status](https://travis-ci.org/akinaru/speed-test-lib.svg?branch=master)](https://travis-ci.org/akinaru/speed-test-lib)
[![Download](https://api.bintray.com/packages/akinaru/maven/speedtest/images/download.svg) ](https://bintray.com/akinaru/maven/speedtest/_latestVersion)
[![Coverage Status](https://coveralls.io/repos/github/akinaru/speed-test-lib/badge.svg?branch=master)](https://coveralls.io/github/akinaru/speed-test-lib?branch=master)
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
compile 'akinaru:speedtest:1.02'
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
	public void onDownloadPacketsReceived(int packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {
		System.out.println("download transfer rate  : " + transferRateBitPerSeconds + " bps");
		System.out.println("download transfer rate  : " + transferRateOctetPerSeconds + "Bps");
	}

	@Override
	public void onDownloadError(int errorCode, String message) {
		System.out.println("Download error " + errorCode + " occured with message : " + message);
	}

	@Override
	public void onUploadPacketsReceived(int packetSize, float transferRateBitPerSeconds, float transferRateOctetPerSeconds) {
		System.out.println("download transfer rate  : " + transferRateBitPerSeconds + " bps");
		System.out.println("download transfer rate  : " + transferRateOctetPerSeconds + "Bps");
	}

	@Override
	public void onUploadError(int errorCode, String message) {
		System.out.println("Upload error " + errorCode + " occured with message : " + message);
	}

	@Override
	public void onDownloadProgress(int percent) {
	}

	@Override
	public void onUploadProgress(int percent) {
	}

});

```

#### Start Download speed test

``void startDownload(String hostname, int port, String uri)``

* `hostname` : server hostname
* `port` : server port
* `uri` : uri to fetch your file from server

```
speedTestSocket.startDownload("ipv4.intuxication.testdebit.info", 80,"/fichiers/10Mo.dat");
```
You can wait for test completion with ``closeSocketJoinRead()`` which is prefered to ``closeSocket()`` since it join reading thread before resuming application.

```
speedTestSocket.closeSocketJoinRead();
```

#### Start Upload speed test

```
void startUpload(String hostname, int port, String uri, int fileSizeOctet)
```

* `hostname` : server hostname
* `port` : server port
* `uri` : uri to fetch your file from server
* `fileSizeOctet` : the file size to be uploaded to server (file will be generated randomly and sent to speed test server)

Here is an example for a file of 10Moctet :
```
speedTestSocket.startUpload("1.testdebit.info", 80, "/", 10000000);
```

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
            public void onDownloadError(int errorCode, String message) {
                Log.i("speed-test-app","Download error " + errorCode + " occured with message : " + message);
            }

            @Override
            public void onUploadPacketsReceived(int packetSize, 
            									float transferRateBitPerSeconds, 
            									float transferRateOctetPerSeconds) {
                Log.i("speed-test-app","download transfer rate  : " + transferRateOctetPerSeconds + "Bps");
            }

            @Override
            public void onUploadError(int errorCode, String message) {
                Log.i("speed-test-app","Upload error " + errorCode + " occured with message : " + message);
            }

            @Override
            public void onDownloadProgress(int percent) {
            }

            @Override
            public void onUploadProgress(int percent) {
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

http://akinaru.github.io/speed-test-lib

## Quick test

``./gradlew quickTest``

## Compatibility

JRE 1.7 compliant

## Build

Gradle using IntelliJ IDEA or Eclipse

## External libraries

* https://github.com/akinaru/http-endec

## SpeedTest Server tested

* https://testdebit.info/

## License

The MIT License (MIT) Copyright (c) 2016 Bertrand Martel
