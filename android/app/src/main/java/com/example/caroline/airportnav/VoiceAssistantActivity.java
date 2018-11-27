package com.example.caroline.airportnav;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.caroline.airportnav.Bean.Message;
import com.example.caroline.airportnav.configuration.Config;
import com.example.caroline.airportnav.utilities.TextToSpeech;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.api.android.AIConfiguration;
import ai.api.android.GsonFactory;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import ai.api.ui.AIButton;

public class VoiceAssistantActivity extends AppCompatActivity implements AIButton.AIButtonListener {

    public static final String TAG = VoiceAssistantActivity.class.getName();

    private AIButton aiButton;
    private Gson gson = GsonFactory.getGson();
    private TextView resultTextView;
    private int MY_RECORD_AUDIO_PERMISSION = 5;
    private static TextToSpeech textToSpeech;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private List<Message> messageList = new ArrayList<>();
    protected LinearLayoutManager mLayoutManager;
    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String AIRLINES = "airlines";
    public static final String GATE = "gate";
    public static final String DEPARTURE_TIME = "departTime";
    public static final String TERMINAL = "terminal";
    public static final String FLIGHT_NUMBER = "flightNumber";
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_assistant);

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        aiButton = (AIButton) findViewById(R.id.imageButton);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED) {
                // put your code for Version>=Marshmallow
            } else {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(this,
                            "App required access to audio", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO
                },MY_RECORD_AUDIO_PERMISSION);
            }

        } else {

        }

        final AIConfiguration config = new AIConfiguration(Config.ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiButton.initialize(config);
        aiButton.setResultsListener(this);

        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, messageList);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mMessageRecycler.setLayoutManager(mLayoutManager);
        mMessageRecycler.setAdapter(mMessageAdapter);
        
        String sessionData = sharedpreferences.getString(GATE,"defaultGate");
        resultTextView.setText(sessionData);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_RECORD_AUDIO_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Application will not have audio on record", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResult(AIResponse response) {
        Result result = response.getResult();
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        // Show results in TextView.
        //String text = "Query:" + result.getResolvedQuery() + "\n Action: " + result.getAction() + "\n Parameters: " + parameterString;
        String Query = result.getResolvedQuery();
        String action = "";
        if(result!= null && result.getContexts() != null && result.getContexts().get(0)!=null) {
            Map<String, JsonElement> context = result.getContexts().get(0).getParameters();
            if (context.containsKey("action")) {
                action = context.get("action").toString();
            }
            resultTextView.setText(action);
            //change this code to display the map based on action
        }

        String speech = response.getResult().getFulfillment().getSpeech();
        messageList.add(new Message(Query,"User"));
        messageList.add(new Message(speech,"Agent"));
        mMessageAdapter.notifyDataSetChanged();
        mMessageRecycler.scrollToPosition(mMessageAdapter.getItemCount() - 1);
        TextToSpeech.speak(speech);


    }

    @Override
    public void onError(final AIError error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onError");
                resultTextView.setText(error.toString());
            }
        });

    }

    @Override
    public void onCancelled() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCancelled");
                resultTextView.setText("");
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        aiButton.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        aiButton.resume();
    }
    public class MessageListAdapter extends RecyclerView.Adapter {
        private Context mContext;
        private List<Message> mMessageList;
        private static final int VIEW_TYPE_MESSAGE_SENT = 1;
        private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

        public MessageListAdapter(Context context, List<Message> messageList) {
            mContext = context;
            mMessageList = messageList;
        }

        @Override
        public int getItemViewType(int position) {
            Message message = (Message) mMessageList.get(position);

            if (message!= null && message.getSender()!=null && !message.getSender().equals("Agent")){
                // If the current user is the sender of the message
                return VIEW_TYPE_MESSAGE_SENT;
            } else {
                // If some other user sent the message
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view;

            if (viewType == VIEW_TYPE_MESSAGE_SENT) {
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.item_message_sent,viewGroup,false);
                return new SentMessageHolder(view);
            } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
                view = LayoutInflater.from(VoiceAssistantActivity.this)
                        .inflate(R.layout.item_message_received, viewGroup, false);
                return new ReceivedMessageHolder(view);
            }

            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Message message = mMessageList.get(position);

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MESSAGE_SENT:
                    ((SentMessageHolder) holder).bind(message);
                    break;
                case VIEW_TYPE_MESSAGE_RECEIVED:
                    ((ReceivedMessageHolder) holder).bind(message);
            }
        }

        @Override
        public int getItemCount() {
            return mMessageList.size();
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.text_message_body);

        }
        void bind(Message message) {
            messageText.setText(message.getMessage());
        }
    }
    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.text_message_body);

        }
        void bind(Message message) {
            messageText.setText(message.getMessage());
        }
    }
}
