package com.example.caroline.airportnav;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;

import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.example.caroline.airportnav.utilities.NetworkUtils;
import com.example.caroline.airportnav.utilities.TextToSpeech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText flightNumber;
    private TextView flightDetails;
    private TextView mChatBox;
    private String number;
    private TextToSpeech TTS;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TTS.initTTS(this);

        flightNumber = (EditText) findViewById(R.id.flight_number);
        flightDetails = (TextView) findViewById(R.id.flight_details);
        mChatBox = (TextView) findViewById(R.id.bottom_text_box);
        btnSpeak = (ImageButton) findViewById(R.id.imageButton);
        flightNumber.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId==EditorInfo.IME_ACTION_DONE){
                    number = v.getText().toString();
                    getFlightDetails(number);
                    flightDetails.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });


    }
    public void getDetails(View view) {
        number = flightNumber.getText().toString();
        Log.d("hello","Button clicked "+number);
        URL timetableURL = NetworkUtils.buildUrlForTimeTable();
        new FetchTimeTableTask().execute(timetableURL);

    }
    public void SpeechToText(View view){
        promptSpeechInput();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if(result!=null & result.get(0).equalsIgnoreCase("yes")){
                        startNavigationActivity();
                    }
                }
                break;
            }
        }
    }

    private void startNavigationActivity() {
        Intent intent = new Intent(this, VoiceAssistantActivity.class);
        startActivity(intent);
    }

    private void getFlightDetails(String number) {

        URL timetableURL = NetworkUtils.buildUrlForTimeTable();
        new FetchTimeTableTask().execute(timetableURL);
    }

    public class FetchTimeTableTask extends AsyncTask<URL, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];
            String TimetableSearchResults = null;
            try {
                TimetableSearchResults = NetworkUtils.getResponseFromHttpUrl(searchUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return TimetableSearchResults;
        }

        @Override
        protected void onPostExecute(String timetableSearchResults) {
            Log.d("hello4","task completed");
            String flight_details = "";
            if (timetableSearchResults != null && !timetableSearchResults.equals("")) {
                try{
                    JSONArray timetable = new JSONArray(timetableSearchResults);
                    for(int i =0;i<timetable.length();i++){
                        Log.d("HEy!", "onPostExecute: timetable retrieves, our flight is"+number);
                        JSONObject table = timetable.getJSONObject(i);
                        JSONObject flight = table.getJSONObject("flight");
                        String flight_number = flight.getString("number");
                        Log.d("HEy2 !", "onPostExecute: flight retrieved"+flight_number);
                        if(flight_number.equals(number)){
                            Log.d("HEy 3!", "onPostExecute: flight matched");
                            JSONObject departure = table.getJSONObject("departure");

                            flight_details = "Terminal: "+departure.getString("terminal")+"\n Gate:"+departure.getString("gate")+"\n Time:"+departure.getString("scheduledTime");
                            break;
                        }else{
                            continue;
                        }

                    }
                }catch(JSONException e){
                    e.printStackTrace();
                }
                flightDetails.setText(flight_details);
            } else {
                flightDetails.setText("Error fetching results");
            }
            mChatBox.setText("Would you like to begin navigation?");
            TTS.speak("Would you like to begin navigation?");
        }

    }

}