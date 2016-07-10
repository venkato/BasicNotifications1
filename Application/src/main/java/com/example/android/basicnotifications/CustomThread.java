package com.example.android.basicnotifications;


public abstract class CustomThread extends Thread {

    public CustomThread(String threadName) {
        super(threadName);
    }


    @Override
    public final void run() {
        try {
            while (true) {
                if (!isEnable()) {
                    break;
                }
                doJob();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract boolean isEnable();

    public abstract void doJob() throws Exception;

}

