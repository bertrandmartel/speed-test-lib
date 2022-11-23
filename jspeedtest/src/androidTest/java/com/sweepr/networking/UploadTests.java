package com.sweepr.networking;

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.bmartel.speedtest.SpeedTestSocket;

@RunWith(AndroidJUnit4.class)
public class UploadTests {

    private Context appContext;

    @Before
    public void setup() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void run_upload_test() {
        final TestAwaiter awaiter = new TestAwaiter();
        final SpeedTestSocket socket = new SpeedTestSocket();
        socket.addSpeedTestListener(awaiter);
        socket.startFixedUpload("http://speedtest.sweepr.com:8080/speedtest/upload.php",
                16 * 1024, 20000);

        final boolean completed = awaiter.acquire();
        assertTrue(completed);
    }
}
