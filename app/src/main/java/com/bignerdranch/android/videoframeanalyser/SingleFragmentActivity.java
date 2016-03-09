package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by JCBSH on 19/01/2016.
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {
    protected static final String LIFE_TAG = "life_SingleFragmentActivity";


    protected String getLifeTag () {
        return LIFE_TAG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(getLifeTag(), "onCreate() ");
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment =  createFragment();

            //Log.d(LIFE_TAG, "before transaction ");
            fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();
            //Log.d(LIFE_TAG, "after transaction ");
        }


    }

    protected int getLayoutResId() {
        return R.layout.activity_fragment;
    }

    @Override
    protected void onStart() {
        Log.d(getLifeTag(), "onStart() ");
        super.onStart();


        Log.d(getLifeTag(), "end of onStart() ");
    }

    @Override
    protected void onResume() {
        Log.d(getLifeTag(), "onResume() ");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(getLifeTag(), "onPause() ");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(getLifeTag(), "onStop() ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(getLifeTag(), "onDestroy() ");
        super.onDestroy();
    }


    protected abstract Fragment createFragment();


}
