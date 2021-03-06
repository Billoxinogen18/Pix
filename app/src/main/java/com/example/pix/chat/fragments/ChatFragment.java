package com.example.pix.chat.fragments;

import android.Manifest;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.transition.Explode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pix.R;
import com.example.pix.chat.adapters.MessageAdapter;
import com.example.pix.chat.utils.FetchPath;
import com.example.pix.home.fragments.ComposeFragment;
import com.example.pix.home.fragments.ProfileFragment;
import com.example.pix.home.models.Chat;
import com.example.pix.home.models.Message;
import com.example.pix.home.utils.EndlessRecyclerViewScrollListener;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.app.Activity.RESULT_OK;
import static com.example.pix.home.activities.HomeActivity.CHANNEL_ID;
import static com.example.pix.home.activities.HomeActivity.KEY_TEXT_REPLY;
import static com.example.pix.home.activities.HomeActivity.REPLY_CODE;
import static com.example.pix.home.models.Chat.CHAT;
import static com.example.pix.home.models.Chat.CREATED_AT;
import static com.example.pix.home.models.Chat.RECIPIENT;
import static com.example.pix.home.models.Chat.USER_PROFILE_CODE;

public class ChatFragment extends Fragment {

    public static final int RESULT_LOAD_IMG = 100;
    public static final int REQUEST_PERM = 101;
    private ImageView ivNewPic;
    private ParseFile newPic;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private EditText etText;
    private Chat chat;
    private Date lastMessage;
    private LinearLayoutManager manager;
    private ParseUser friend;

    public ChatFragment() {
    }

    public ChatFragment(ParseFile parseFile) {
        this.newPic = parseFile;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM);

        // This is what gives up the Bundle containing the reply the User has entered
        Bundle remoteInput = RemoteInput.getResultsFromIntent(getActivity().getIntent());

        // Use the objectId we passed to get this Chat
        String chatId = getActivity().getIntent().getStringExtra(CHAT);
        chat = Chat.getChat(chatId);

        friend = chat.getFriend(ParseUser.getCurrentUser());

        if (remoteInput != null) {
            // Get the reply that was entered
            String inputString = remoteInput.getCharSequence(KEY_TEXT_REPLY).toString();

            // Save as a new message
            Message newMessage = new Message();
            newMessage.setText(inputString);
            newMessage.setChat(chat);
            newMessage.setFrom(ParseUser.getCurrentUser());
            newMessage.setTo(friend);
            try {
                newMessage.save();
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Error replying", Toast.LENGTH_SHORT).show();
            }

            // Replace the current notification with one that just lets the User know the message was sent
            Notification repliedNotification =
                    new Notification.Builder(getContext(), CHANNEL_ID)
                            .setSmallIcon(
                                    android.R.drawable.ic_dialog_info)
                            .setContentText("Reply received")
                            .build();

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getContext());
            notificationManager.notify(REPLY_CODE, repliedNotification);
            // End this Activity since this is a reply from outside the app
            getActivity().finish();
        }

        RecyclerView rvMessages = view.findViewById(R.id.chat_rv);
        ImageView ivProfile = view.findViewById(R.id.chat_profile);
        TextView tvName = view.findViewById(R.id.chat_name);
        ImageView ivBack = view.findViewById(R.id.chat_back);
        ImageView ivCamera = view.findViewById(R.id.chat_camera);
        etText = view.findViewById(R.id.chat_text);
        ImageView ivPictures = view.findViewById(R.id.chat_pictures);
        ivNewPic = view.findViewById(R.id.chat_image);

        ComposeFragment composeFragment = new ComposeFragment();
        composeFragment.setSharedElementEnterTransition(new Explode());
        composeFragment.setEnterTransition(new Explode());
        setExitTransition(new Explode());
        composeFragment.setSharedElementReturnTransition(new Explode());

        // If we click the camera, go to the ComposeFragment
        ivCamera.setOnClickListener(unusedView -> {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.friend_container, composeFragment)
                    .commit();
        });

        view.findViewById(R.id.chat_send).setOnClickListener(unusedView -> {
            sendMessage();
        });

        ivBack.setOnClickListener(unusedView -> getActivity().finish());

        if (newPic != null) {
            newPic.saveInBackground((SaveCallback) e -> {
                if (e != null) {
                    Toast.makeText(getContext(), "Error sending snap", Toast.LENGTH_SHORT).show();
                    return;
                }
                Message m = new Message();
                m.setPic(newPic);
                m.setFrom(ParseUser.getCurrentUser());
                m.setTo(friend);
                m.setChat(chat);
                // If we took a new picture with Camera, this is a new SNap
                m.setIsSnap(true);
                saveMessage(m);
            });
        }

        // When we click the plus, go to add a pic
        ivPictures.setOnClickListener(unusedView -> {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            startActivityForResult(i, RESULT_LOAD_IMG);
        });

        // Get this friend's profile pic only if its not null
        ParseFile profilePic;
        try {
            profilePic = friend.fetchIfNeeded().getParseFile(USER_PROFILE_CODE);
            if (profilePic != null) {
                Glide.with(getActivity()).load(profilePic.getUrl()).circleCrop().into(ivProfile);
            }
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Error retrieving more chats", Toast.LENGTH_SHORT).show();
        }

        Fragment friendProfile = new ProfileFragment(friend);
        ivProfile.setOnClickListener(unusedView -> {
            // If we have already made a ProfileFragment for this friend, show it and hide the Chat
            if (getParentFragmentManager().getFragments().contains(friendProfile)) {
                getParentFragmentManager()
                        .beginTransaction()
                        .show(friendProfile)
                        .hide(this)
                        .commit();
                return;
            }
            // If not, add it, then show it and hide the Chat
            getParentFragmentManager()
                    .beginTransaction()
                    .add(R.id.friend_container, friendProfile)
                    .hide(this)
                    .commit();
        });

        tvName.setText(friend.getUsername());

        manager = new LinearLayoutManager(getContext());

        // Snapchat scrolls up instead of down, so reverse
        manager.setReverseLayout(true);

        try {
            // This large ImageView covers the entire ChatFragment and will be used to display Snaps
            // Same goes for the large VideoView
            ImageView imageContainer = view.findViewById(R.id.chat_snap_pic);
            VideoView videoContainer = view.findViewById(R.id.chat_snap_vid);
            messages = chat.getMessages(0, ParseUser.getCurrentUser());
            messageAdapter = new MessageAdapter(getContext(), messages, imageContainer, videoContainer);
            rvMessages.setAdapter(messageAdapter);
            rvMessages.setLayoutManager(manager);
            EndlessRecyclerViewScrollListener scroll = new EndlessRecyclerViewScrollListener(manager) {
                @Override
                public void onLoadMore(int page, int totalItemsCount, RecyclerView unusedView) {
                    try {
                        chat.getMessagesInBackground(page, ParseUser.getCurrentUser(), (objects, e) -> {
                            if (e != null) {
                                Toast.makeText(getContext(), "Error getting more messages", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            messages.addAll(objects);
                            messageAdapter.notifyDataSetChanged();
                        });

                    } catch (ParseException e) {
                        Log.e("Error", "Failed fetching more messages", e);
                        Toast.makeText(getContext(), "Error retrieving more messages", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            rvMessages.addOnScrollListener(scroll);
            manager.scrollToPosition(0);

            etText.setOnFocusChangeListener((unusedView, b) -> {
                if (b) {
                    manager.scrollToPosition(0);
                }
            });
            // When we press enter, save this text as a new Message within this Chat and scroll to the latest message
            etText.setOnKeyListener((unusedView, i, keyEvent) -> {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                    sendMessage();
                    return true;
                }
                return false;
            });

            // Record the last message we received's time
            if (messages.size() == 0) {
                lastMessage = null;
            } else {
                lastMessage = messages.get(0).getTime();
            }
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    ParseQuery<Message> q = ParseQuery.getQuery(Message.class);
                    q.whereEqualTo(CHAT, chat);
                    q.whereEqualTo(RECIPIENT, ParseUser.getCurrentUser());
                    // Check if there is a message in this Chat, to the current user who's time is greater
                    // than our latest message
                    if (lastMessage != null) {
                        q.whereGreaterThan(CREATED_AT, lastMessage);
                    }
                    q.orderByAscending(CREATED_AT);
                    q.findInBackground((newMessages, e) -> {
                        if (e != null) {
                            Toast.makeText(getContext(), "Error updating messages", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Case where there is no new messages
                        if (newMessages.size() == 0) return;

                        // Case where we have some messages to add
                        for (Message m : newMessages) {
                            messages.add(0, m);
                        }

                        // Alert our RecyclerView and Adapter
                        messageAdapter.notifyDataSetChanged();
                        if (messages.size() == 0) {
                            lastMessage = null;
                        } else {
                            lastMessage = messages.get(0).getTime();
                        }
                        manager.scrollToPosition(0);
                    });
                }
            }, 0, 500);

        } catch (ParseException e) {
            Toast.makeText(getContext(), "Error retrieving messages", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage() {
        if (etText.getText().toString().length() == 0 && newPic == null) {
            Toast.makeText(getContext(), "Empty Message", Toast.LENGTH_SHORT).show();
            return;
        }
        // Create new message
        Message newMessage = new Message();
        newMessage.setText(etText.getText().toString());
        newMessage.setFrom(ParseUser.getCurrentUser());
        newMessage.setTo(friend);
        newMessage.setChat(chat);
        if (newPic != null) {
            // Case where we need to save a picture to DB first
            newPic.saveInBackground((SaveCallback) e -> {
                if (e != null) {
                    Toast.makeText(getContext(), "Error saving picture", Toast.LENGTH_SHORT).show();
                    return;
                }
                newMessage.setPic(newPic);
                saveMessage(newMessage);
            });
        } else {
            // Case where there is no picture
            saveMessage(newMessage);
        }
    }

    private void saveMessage(Message newMessage) {
        newMessage.saveInBackground(e -> {
            if (e != null) {
                Log.e("Error", "Failed saving message", e);
                Toast.makeText(getContext(), "Error sending message", Toast.LENGTH_SHORT).show();
                return;
            }
            messages.add(0, newMessage);
            messageAdapter.notifyDataSetChanged();
            manager.scrollToPosition(0);
            etText.setText("");
            chat.setStatus(1);
            ivNewPic.setImageResource(0);
            ivNewPic.setVisibility(View.GONE);
            newPic = null;
            chat.saveInBackground(e1 -> {
                if (e1 != null) {
                    Toast.makeText(getContext(), "Error sending read receipt", Toast.LENGTH_SHORT).show();
                    Log.e("Error", "Failed saving chat", e1);
                }
            });
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == RESULT_LOAD_IMG) {
                try {
                    final Uri imageUri = data.getData();
                    InputStream imageStream = getActivity().getContentResolver().openInputStream(imageUri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                    ivNewPic.setImageBitmap(rotateBitmap(selectedImage, 90));
                    ivNewPic.setVisibility(View.VISIBLE);

                    newPic = new ParseFile(new File(FetchPath.getPath(getContext(), imageUri)));
                } catch (IOException e) {
                    Toast.makeText(getContext(), "Error retrieving image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
