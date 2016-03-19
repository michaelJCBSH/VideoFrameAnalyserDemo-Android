package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

/**
 * Created by JCBSH on 14/03/2016.
 */
public class AnalyserActivity extends SingleFragmentActivity{


    @Override
    protected Fragment createFragment() {
        String path = getIntent().getStringExtra(AnalyserFragment.EXTRA_VIDEO_FILE_PATH);

        Fragment fragment = AnalyserMediaCodecFragment.getInstance(path);
        return fragment;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    @Override
    protected int getLayoutResId() {
        return R.layout.activity_fragment_video_record;
    }
}
