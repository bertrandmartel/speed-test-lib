package fr.bmartel.speedtest.model;

/**
 * FTP local mode
 * @author Bertrand Martel
 */
public enum FtpMode {
    //https://commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/ftp/FTPClient.html#enterLocalActiveMode()
    ACTIVE,
    //https://commons.apache.org/proper/commons-net/apidocs/org/apache/commons/net/ftp/FTPClient.html#enterLocalPassiveMode()
    PASSIVE
}
