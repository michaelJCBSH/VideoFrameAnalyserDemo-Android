package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by JCBSH on 9/03/2016.
 */
public class RecordVideoFragment extends Fragment {


    public static Fragment getInstance() {

        Fragment fragment = new RecordVideoFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record_video, container, false);
    }

}


