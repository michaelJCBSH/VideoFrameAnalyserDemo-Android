package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public static Fragment getInstance() {

        Fragment fragment = new MainActivityFragment();
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        Button mRecordVideoButton = (Button) v.findViewById(R.id.recordVideo_button);
        mRecordVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), RecordVideoActivity.class);
                startActivity(intent);
            }
        });

        Button mListVideoButton = (Button) v.findViewById(R.id.analysisFrames_button);
        mListVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ListVideoActivity.class);
                startActivity(intent);
            }
        });
        return v;

    }

}
