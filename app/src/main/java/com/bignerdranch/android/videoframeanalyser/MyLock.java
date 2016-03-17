package com.bignerdranch.android.videoframeanalyser;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by JCBSH on 17/03/2016.
 */
public class MyLock {
    private ReentrantLock mReentrantLock;
    public MyLock () {
        mReentrantLock = new ReentrantLock();
    }




    public void aquireLock()  {

        while(true ) {
            boolean getFirstLockfirstLock = false;
            try {
                getFirstLockfirstLock = mReentrantLock.tryLock();
            } finally {
                if (getFirstLockfirstLock) {
                    return;
                }
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void unLock()  {
        mReentrantLock.unlock();
    }
}
