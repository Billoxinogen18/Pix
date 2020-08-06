package com.example.pix.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.pix.R;
import com.example.pix.home.activities.HomeActivity;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class LoginActivity extends AppCompatActivity {

    private static final String REDIRECT_URI = "yourcustomprotocol://callback";
    private boolean loggedIn = false;
    private boolean authenticated = false;
    private static SpotifyAppRemote mSpotifyAppRemote;

    // We can access the Spotify Remote in later Activities
    public static SpotifyAppRemote getmSpotifyAppRemote() {
        return mSpotifyAppRemote;
    }

    // When one of (spotify, parse) is done, check if both are done
    private void checkIfDone() {
        if (loggedIn && authenticated) {
            Intent i = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        final EditText etUsername = findViewById(R.id.entered_username);
        final EditText etPassword = findViewById(R.id.entered_password);

        Button btnSignup = findViewById(R.id.parse_signup);
        Button btnSpotify = findViewById(R.id.auth_spotify);

        // If we hit enter from the username, go to the password...
        etUsername.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                return etPassword.requestFocus();
            }
            return false;
        });

        // If we hit enter from the password, focus on the login/signup buttons
        etPassword.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                return btnSignup.requestFocus();
            }
            return false;
        });

        // If we already have a User logged in, reflect that
        if (ParseUser.getCurrentUser() != null) {
            loggedIn = true;
            (findViewById(R.id.parse_container)).setVisibility(View.GONE);
            checkIfDone();
        }

        // If we signed out, we have already made a Spotify Remote
        if (mSpotifyAppRemote != null) {
            // We are done Authenticating Spotify
            authenticated = true;
            (findViewById(R.id.auth_spotify)).setVisibility(View.GONE);
            checkIfDone();
        }


        // Attempt to login the User
        (findViewById(R.id.parse_login)).setOnClickListener(unusedView -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            ParseUser.logInInBackground(username, password, new LogInCallback() {
                @Override
                public void done(ParseUser user, ParseException e) {
                    if (e != null) {
                        Toast.makeText(LoginActivity.this, "Incorrect credentials", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(LoginActivity.this, "Successfully logged in!", Toast.LENGTH_SHORT).show();
                    loggedIn = true;
                    (findViewById(R.id.parse_container)).setVisibility(View.GONE);
                    checkIfDone();
                    btnSpotify.requestFocus();
                }
            });
        });

        // When user clicks register, we set up their account then go to MainActivity
        btnSignup.setOnClickListener(unusedView -> {
            ParseUser newUser = new ParseUser();
            newUser.setUsername(etUsername.getText().toString());
            newUser.setPassword(etPassword.getText().toString());
            try {
                newUser.signUp();
                Toast.makeText(LoginActivity.this, "Successfully signed up!", Toast.LENGTH_SHORT).show();
                loggedIn = true;
                (findViewById(R.id.parse_container)).setVisibility(View.GONE);
                checkIfDone();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });

        // When button is hit, create an AuthenticationRequest and jump to the Spotify-provided LoginActivity
        btnSpotify.setOnClickListener(unusedView -> {

            // Set the connection parameters
            ConnectionParams connectionParams =
                    new ConnectionParams.Builder("cf5e6393a07f442ab4f22d05650071ec") // Client ID
                            .setRedirectUri(REDIRECT_URI)
                            .showAuthView(true)
                            .build();

            SpotifyAppRemote.connect(LoginActivity.this, connectionParams,
                    new Connector.ConnectionListener() {

                        @Override
                        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                            mSpotifyAppRemote = spotifyAppRemote;

                            // We are done Authenticating Spotify
                            (findViewById(R.id.auth_spotify)).setVisibility(View.GONE);
                            checkIfDone();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e("MainActivity", throwable.getMessage(), throwable);
                        }
                    });
            authenticated = true;
        });
    }

}