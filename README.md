# Speed Test library for Java / Android  #

http://akinaru.github.io/speed-test-lib/

<i>Last update 26/05/2015</i>

Speed Test library featuring :

* speed test download with transfer rate output
* speed test upload with transfer rate output
* download and upload progress monitoring
* speed test server / port / uri can be configured easily

<b>Last release ></b>
https://github.com/akinaru/speed-test-lib/releases

* For download process, library will download file from given speed test server parameters and calculate transfer rate
* For upload process, library will generate a random file with a given size and will upload this file to a server calculating transfer rate

No external file are required and no file are stored in Hard Disk.

<hr/>

<h3>How to use ?</h3>

<b>Instanciate SpeedTest class</b>

```
SpeedTestSocket speedTestSocket = new SpeedTestSocket();
```
<b>Add a listener to monitor</b>

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

<b>Start Download speed test</b>

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

<b>Start Upload speed test</b>

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
<hr/>

<h3>Quick test command line syntax</h3> 

``java -jar speed-test-lib-1.0.jar``

in folder ./http-endec-java/release

<hr/>

* Project is JRE 1.7 compliant
* You can build it with ant => build.xml
* Development on Eclipse 

<b>Tested with</b>

* https://testdebit.info/

<h3>TODO</h3>

manage unexpected socket disconnection for downloading and uploading
