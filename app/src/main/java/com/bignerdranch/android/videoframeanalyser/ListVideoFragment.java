package com.bignerdranch.android.videoframeanalyser;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;

/**
 * Created by JCBSH on 9/03/2016.
 */
public class ListVideoFragment extends Fragment{

    private static final String TAG = ListVideoFragment.class.getSimpleName();
    private ListView listView;

    public static Fragment getInstance() {
        Fragment fragment = new ListVideoFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_video, container, false);
        listView = (ListView) v.findViewById(R.id.listView);

        File[] videoFiles = findVideos();
        listView.setAdapter(new VideoFilesAdapter(getActivity(), videoFiles));
        return v;
    }

    private File[] findVideos() {
        File[] files = getActivity().getExternalFilesDir(null).listFiles();
        for (File video: files) {
            Log.d(TAG, "files: " +  video.getName());
        }
        return files;
    }

    private class VideoFilesAdapter extends ArrayAdapter<File> {
        public VideoFilesAdapter(Context context, File[] videos) {
            super(context, android.R.layout.simple_expandable_list_item_1, videos);
        }

    }
}
