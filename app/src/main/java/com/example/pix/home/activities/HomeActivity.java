package com.example.pix.home.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.pix.R;
import com.example.pix.home.fragments.HomeFragment;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class HomeActivity extends AppCompatActivity {

    SpotifyAppRemote mSpotifyAppRemote;
    public static final int RESULT_LOAD_IMG = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // The home activity first contains the HomeFragment, which can potentially replace itself with a ProfileFragment
        getSupportFragmentManager().beginTransaction().replace(R.id.home_profile, new HomeFragment()).commit();


        /*LinearLayout homeContainer = findViewById(R.id.home_container);

        View.OnTouchListener onTouchListener = (view, motionEvent) -> {
            switch(motionEvent.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    x1 = motionEvent.getX();
                    return true;
                case MotionEvent.ACTION_UP:
                    x2 = motionEvent.getX();
                    float deltaX = x2 - x1;
                    if (deltaX > 0)
                    {
                        Toast.makeText(HomeActivity.this, "left2right swipe", Toast.LENGTH_SHORT).show ();
                    }
                    else
                    {
                        Toast.makeText(HomeActivity.this, "right2left swipe", Toast.LENGTH_SHORT).show ();
                    }
                    return true;
            }
            return super.onTouchEvent(motionEvent);
        };

        homeContainer.setOnTouchListener(onTouchListener); */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}