package org.tensorflow.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FragmentPage1 extends Fragment {
    private View v;
    private String checked;

    //권한
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1001;

    FragmentManager manager;
    FragmentTransaction fragmentTransaction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        v = inflater.inflate(R.layout.fragment1, container, false);

        Button handBtn = (Button) v.findViewById(R.id.handRecogBtn);
        Button stuffBtn = (Button) v.findViewById(R.id.stuffRecogBtn);

        manager = getChildFragmentManager();
        fragmentTransaction = manager.beginTransaction();

        handBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), holistic_activity.class);
                startActivity(intent);
            }
        });
        stuffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), DetectorActivity.class);
                startActivity(intent);
            }
        });
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

}
