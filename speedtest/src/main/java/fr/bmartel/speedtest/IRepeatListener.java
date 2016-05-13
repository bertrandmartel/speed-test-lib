package fr.bmartel.speedtest;

/**
 * Created by akinaru on 13/05/16.
 */
public interface IRepeatListener {

    void onFinish(SpeedTestReport report);

    void onReport(SpeedTestReport report);
}
