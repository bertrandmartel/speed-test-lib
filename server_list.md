# List of compatible speed test server

This list is non-exhaustive

## speedtest.net

Complete server list can be found here : http://www.speedtest.net/speedtest-servers.php

* download : http://rbx-fr.verelox.com/speedtest/random2000x2000.jpg
* upload   : http://rbx-fr.verelox.com/speedtest/upload.php

## speedtest mini server

http://www.speedtest.net/mini.php

* download : http://127.0.0.1/mini/speedtest/random2000x2000.jpg
* upload   : http://127.0.0.1/mini/speedtest/upload.php

## Tele2 server

http://speedtest.tele2.net

* http download : http://speedtest.tele2.net/100MB.zip
* ftp download  : ftp://speedtest.tele2.net/10MB.zip
* ftp upload : ftp://speedtest.tele2.net/upload

## testdebit.info

https://testdebit.info/

* http download : http://2.testdebit.info/fichiers/1Mo.dat
* http upload : http://2.testdebit.info/

## mirror.internode.on.net

http://mirror.internode.on.net/pub/speed

* http download : http://mirror.internode.on.net/pub/speed/SpeedTest_16MB

## test.talia.net

http://test.talia.net

* http download : http://test.talia.net/dl/1mb.pak

## speedtest.ftp.otenet.gr

http://speedtest.ftp.otenet.gr

* http download : http://speedtest.ftp.otenet.gr/files/test1Mb.db
* ftp download : ftp://speedtest:speedtest@ftp.otenet.gr/test1Mb.db

## serverius.net

http://speedtest.serverius.net

* http download : http://speedtest.serverius.net/files/10mb.bin

## ovh.net

http://ovh.net/files

* http download : http://ovh.net/files/1Mb.dat

## Test configuration

```
[{
    "mode": "download",
    "uri": "http://2.testdebit.info/fichiers/1Mo.dat"
}, {
    "mode": "upload",
    "uri": "http://2.testdebit.info/"
}, {
    "mode": "download",
    "uri": "http://mirror.internode.on.net/pub/speed/SpeedTest_16MB"
}, {
    "mode": "download",
    "uri": "http://test.talia.net/dl/1mb.pak"
}, {
    "mode": "download",
    "uri": "http://speedtest.ftp.otenet.gr/files/test1Mb.db"
}, {
    "mode": "download",
    "uri": "ftp://speedtest:speedtest@ftp.otenet.gr/test1Mb.db"
}, {
    "mode": "download",
    "uri": "http://speedtest.serverius.net/files/10mb.bin"
}, {
    "mode": "download",
    "uri": "http://ovh.net/files/1Mb.dat"
}]
```