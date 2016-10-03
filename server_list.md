# Non exhaustive list of speed test server

## speedtest.net

Complete server list can be found here : http://www.speedtest.net/speedtest-servers.php

* download : http://speedo.eltele.no/speedtest/random2000x2000.jpg
* upload   : http://speedo.eltele.no/speedtest/upload.php

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
* ftp download : ftp://speedtest.serverius.net/10mb.bin

## ovh.net

http://ovh.net/files

* http download : http://ovh.net/files/1Mb.dat

## Test configuration

```
[
    {
        "host": "speedo.eltele.no",
        "download": [
            {
                "protocol": "http",
                "uri": "/speedtest/random2000x2000.jpg"
            }
        ],
        "upload": [
            {
                "protocol": "http",
                "uri": "/speedtest/upload.php"
            }
        ]
    },
    {
        "host": "speedtest.tele2.net",
        "download": [
            {
                "protocol": "http",
                "uri": "/100MB.zip"
            },
            {
                "protocol": "ftp",
                "uri": "/1MB.zip"
            }
        ],
        "upload": [
            {
                "protocol": "ftp",
                "uri": "/speedtest"
            }
        ]
    },
    {
        "host": "2.testdebit.info",
        "download": [
            {
                "protocol": "http",
                "uri": "/fichiers/1Mo.dat"
            }
        ],
        "upload": [
            {
                "protocol": "http",
                "uri": "/"
            }
        ]
    },
    {
        "host": "mirror.internode.on.net",
        "download": [
            {
                "protocol": "http",
                "uri": "/pub/speed/SpeedTest_16MB"
            }
        ]
    },
    {
        "host": "test.talia.net",
        "download": [
            {
                "protocol": "http",
                "uri": "/dl/1mb.pak"
            }
        ]
    },
    {
        "host": "speedtest.ftp.otenet.gr",
        "download": [
            {
                "protocol": "http",
                "uri": "/files/test1Mb.db"
            }
        ]
    },
    {
        "host": "ftp.otenet.gr",
        "download": [
            {
                "protocol": "ftp",
                "uri": "/test1Mb.db",
                "username": "speedtest",
                "password": "speedtest"
            }
        ]
    },
    {
        "host": "speedtest.serverius.net",
        "download": [
            {
                "protocol": "http",
                "uri": "/files/10mb.bin"
            },
            {
                "protocol": "ftp",
                "uri": "/10mb.bin"
            }
        ]
    },
    {
        "host": "ovh.net",
        "download": [
            {
                "protocol": "http",
                "uri": "/files/1Mb.dat"
            }
        ]
    }
]
```