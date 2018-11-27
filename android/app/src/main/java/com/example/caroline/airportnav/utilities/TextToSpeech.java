package com.example.caroline.airportnav.utilities;

import android.content.Context;

import java.util.Locale;

public class TextToSpeech {

    private static android.speech.tts.TextToSpeech textToSpeech;

    public static void initTTS(final Context context) {
        if (textToSpeech == null) {
            textToSpeech = new android.speech.tts.TextToSpeech(context, new android.speech.tts.TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setSpeechRate((float)0.95);
                    String welcome = "Welcome to Airport Navigation, please enter your flight number";

                    speak(welcome);
                }
            });
        }
    }

    public static void speak(final String text) {
        textToSpeech.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null);
    }
}
