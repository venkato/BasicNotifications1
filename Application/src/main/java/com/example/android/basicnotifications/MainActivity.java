package com.example.android.basicnotifications;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * The entry point to the BasicNotification sample.
 */
public class MainActivity extends Activity {
    /**
     * A numeric value that identifies the notification that we'll be sending.
     * This value needs to be unique within this app, but it doesn't need to be
     * unique system-wide.
     */
    public static final int NOTIFICATION_ID = 1;
    final Object monitor = new Object();
    volatile boolean linkOk = true;
    volatile String lastErrorMsg;
    volatile boolean start = false;
    volatile Thread thread;
    volatile Thread monitorThread;
    volatile Thread readInThread;
    volatile Thread readErrThread;
    volatile boolean inProgress = false;
    long sleepTime = 3000;
    long maxWaitTime = 3000;
    volatile ByteArrayOutputStream in;
    volatile ByteArrayOutputStream err;
    volatile Date lastRunTime;
    volatile Process process;
    private volatile InputStream inI, errI;

    /**
     * Read all bytes from input stream.
     *
     * @param in
     * @param bufferSize
     * @return read bytes
     * @throws IOException
     */
    public static byte[] readAllBytesFromInpustStream(final InputStream in,
                                                      final int bufferSize, ByteArrayOutputStream out) throws IOException {
        // TODO correct method name
        final byte[] buffer = new byte[bufferSize];
        // final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int readBytes;
        while ((readBytes = in.read(buffer)) > 0) {
            out.write(buffer, 0, readBytes);
        }
        return out.toByteArray();
    }

    /**
     * Read all bytes from input stream with buffer sise = 8192 bytes.
     */
    public static byte[] readAllBytesFromInpustStream(final InputStream in, ByteArrayOutputStream out)
            throws IOException {
        return readAllBytesFromInpustStream(in, 8192, out);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_layout);
        final Button refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {

                                             @Override
                                             public void onClick(View v) {
                                                 TextView lastRun = (TextView) findViewById(R.id.lastrun);
                                                 lastRun.setText("Last run = "+lastRunTime+ ", in progress = "+inProgress);
                                                 TextView textView = (TextView) findViewById(R.id.pingOut);
                                                 textView.setText(new String(in.toByteArray()) + " " + new String(err.toByteArray()));
                                             }
                                         }
        )        ;
        final Button stopStartButton = (Button) findViewById(R.id.stopStartButton);
        stopStartButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                start = "START".equals(stopStartButton.getText());
                stopStartButton.setText(start ? "STOP" : "START");
                if (start) {
                    startThreads();

                }
            }
        });


    }

    private void startThreads() {
        thread = new CustomThread("Nik ping thread") {

            @Override
            public boolean isEnable() {
                if (this != thread) {
                    return false;
                }
                return start;
            }

            @Override
            public void doJob() throws Exception {
                mainThread();
            }
        };
        thread.start();
        monitorThread = new CustomThread("Nik monitor thread") {

            @Override
            public boolean isEnable() {
                if (this != monitorThread) {
                    return false;
                }
                return start;
            }

            @Override
            public void doJob() throws Exception {
                monitorThread();

            }
        };
        monitorThread.start();
        readErrThread = new CustomThread("Nik error read thread") {

            @Override
            public boolean isEnable() {
                if (this != readErrThread) {
                    return false;
                }
                return start;
            }

            @Override
            public void doJob() throws Exception {
                readAllBytesFromInpustStream(errI, err);

            }
        };
        readErrThread.start();
        readInThread = new CustomThread("Nik error read thread") {

            @Override
            public boolean isEnable() {
                if (this != readInThread) {
                    return false;
                }
                return start;
            }

            @Override
            public void doJob() throws Exception {
                readAllBytesFromInpustStream(inI, in);

            }
        };
        readInThread.start();
    }

    private void monitorThread() {
        long timeDiff = lastRunTime.getTime() - System.currentTimeMillis();
        if (inProgress && timeDiff > maxWaitTime) {
            sendNotificationNik(false, "hangs " + lastRunTime + " " + process);
        }
    }

    private void readOutput(InputStream in, ByteArrayOutputStream out) throws Exception {
        if (in != null) {
            try {
                readAllBytesFromInpustStream(in, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        synchronized (monitor) {
            monitor.wait(3000);
        }
    }

    private void mainThread() throws Exception {
        //String[] ping = {"ping","-c","1","-W","1000","ya.ru"};
        inProgress = true;
        lastRunTime = new Date();
        String ping = "ping -c 1 -W 1000 ya.ru";
        in = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        process = Runtime.getRuntime().exec(ping);
        inI = process.getInputStream();
        errI = process.getErrorStream();

        // +" "+in+" "+err
        inProgress = false;
        process = null;
        int exitCode = process.waitFor();
        linkOk = exitCode == 0;
        synchronized (monitor) {
            monitor.notifyAll();
        }
        Thread.currentThread().sleep(1000);
        sendNotificationNik(linkOk, new String(in.toByteArray()) + " " + new String(err.toByteArray()) + " link ok = " + linkOk);
        //inI.close();
        //errI.close();
//                        sendNotificationNik(linkOk,new Date()+" link ok = "+linkOk);
        if (!start) {
            return;
        }
        Thread.sleep(sleepTime);

    }

    private String readInputStream(InputStream in) {
        return null;
    }


    public void sendNotificationNik(boolean statusOk, String msg) {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, MainActivity.class);
//        Intent intent = new Intent(Intent.ACTION_VIEW,
//                Uri.parse("http://developer.android.com/reference/android/app/Notification.html"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(statusOk ? R.drawable.connected : R.drawable.no_connection);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);

//        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        builder.setContentTitle(statusOk ? "connected" : msg);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Send a sample notification using the NotificationCompat API.
     */
    public void sendNotification(View view) {

        // BEGIN_INCLUDE(build_action)
        /** Create an intent that will be fired when the user clicks the notification.
         * The intent needs to be packaged into a {@link android.app.PendingIntent} so that the
         * notification service can fire it on our behalf.
         */
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://developer.android.com/reference/android/app/Notification.html"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // END_INCLUDE(build_action)

        // BEGIN_INCLUDE (build_notification)
        /**
         * Use NotificationCompat.Builder to set up our notification.
         */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        /** Set the icon that will appear in the notification bar. This icon also appears
         * in the lower right hand corner of the notification itself.
         *
         * Important note: although you can use any drawable as the small icon, Android
         * design guidelines state that the icon should be simple and monochrome. Full-color
         * bitmaps or busy images don't render well on smaller screens and can end up
         * confusing the user.
         */
        builder.setSmallIcon(R.drawable.ic_stat_notification);

        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);

        // Set the notification to auto-cancel. This means that the notification will disappear
        // after the user taps it, rather than remaining until it's explicitly dismissed.
        builder.setAutoCancel(true);

        /**
         *Build the notification's appearance.
         * Set the large icon, which appears on the left of the notification. In this
         * sample we'll set the large icon to be the same as our app icon. The app icon is a
         * reasonable default if you don't have anything more compelling to use as an icon.
         */
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        /**
         * Set the text of the notification. This sample sets the three most commononly used
         * text areas:
         * 1. The content title, which appears in large type at the top of the notification
         * 2. The content text, which appears in smaller text below the title
         * 3. The subtext, which appears under the text on newer devices. Devices running
         *    versions of Android prior to 4.2 will ignore this field, so don't use it for
         *    anything vital!
         */
        builder.setContentTitle("BasicNotifications Sample");
        builder.setContentText("Time to learn about notifications!");
        builder.setSubText("Tap to view documentation about notifications.");

        // END_INCLUDE (build_notification)

        // BEGIN_INCLUDE(send_notification)
        /**
         * Send the notification. This will immediately display the notification icon in the
         * notification bar.
         */
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        // END_INCLUDE(send_notification)
    }
}
