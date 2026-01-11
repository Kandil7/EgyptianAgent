package com.egyptian.agent.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PocketSphinx wake word detector for efficient, low-power keyword spotting
 * Optimized for Egyptian dialect wake words like "يا حكيم" and "يا كبير"
 */
public class PocketSphinxWakeWordDetector implements RecognitionListener {
    private static final String TAG = "PocketSphinxDetector";
    
    private static final String WAKE_WORD_SEARCH_NAME = "wake_word";
    private static final String GRAMMAR_FILE = "grammar.jsgf";
    private static final String DICTIONARY_FILE = "egyptian.dic";
    
    private Context context;
    private SpeechRecognizer recognizer;
    private WakeWordCallback callback;
    private boolean isListening = false;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private ExecutorService executor;
    
    // Wake word variations for Egyptian dialect
    private static final String[] EGYPTIAN_WAKE_WORDS = {
        "ya sa7bi",    // يا صاحبي
        "ya kabir",    // يا كبير
        "yakbir",      // يا كبير (different pronunciation)
        "yas7bi",      // يا صاحبي (different pronunciation)
        "ya3am",       // يا عمي
        "ya 3am"       // يا عمي (separated)
    };
    
    public interface WakeWordCallback {
        void onWakeWordDetected();
    }
    
    public PocketSphinxWakeWordDetector(Context context, WakeWordCallback callback) {
        this.context = context;
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Initializes the PocketSphinx recognizer with Egyptian dialect wake words
     */
    public void initialize() throws IOException {
        Log.i(TAG, "Initializing PocketSphinx wake word detector");
        
        Assets assets = new Assets(context);
        File assetDir = assets.syncAssets();
        
        // Build the recognizer
        recognizer = defaultSetup()
                .setAcousticModel(new File(assetDir, "en-us-ptm"))
                .setDictionary(new File(assetDir, DICTIONARY_FILE))
                .setRawLogDir(assetDir)
                .getRecognizer();
        
        // Add searches for wake words
        setupWakeWordSearch();
        
        // Register this class as listener
        recognizer.addListener(this);
        
        Log.i(TAG, "PocketSphinx wake word detector initialized successfully");
    }
    
    /**
     * Sets up the search for wake words
     */
    private void setupWakeWordSearch() {
        StringBuilder grammarBuilder = new StringBuilder();
        grammarBuilder.append("#JSGF V1.0;\n");
        grammarBuilder.append("grammar wake;\n");
        grammarBuilder.append("public <wake> = ");
        
        for (int i = 0; i < EGYPTIAN_WAKE_WORDS.length; i++) {
            grammarBuilder.append(EGYPTIAN_WAKE_WORDS[i]);
            if (i < EGYPTIAN_WAKE_WORDS.length - 1) {
                grammarBuilder.append(" | ");
            }
        }
        grammarBuilder.append(";\n");
        
        // Create temporary grammar file
        try {
            File tempGrammarFile = new File(context.getCacheDir(), "egyptian_wake_words.gram");
            java.io.FileWriter writer = new java.io.FileWriter(tempGrammarFile);
            writer.write(grammarBuilder.toString());
            writer.close();
            
            recognizer.addGrammarSearch(WAKE_WORD_SEARCH_NAME, tempGrammarFile.getPath());
            Log.i(TAG, "Wake word grammar added: " + grammarBuilder.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error creating grammar file", e);
            // Fallback to default grammar
            recognizer.addGrammarSearch(WAKE_WORD_SEARCH_NAME, new File(context.getAssets().toString(), GRAMMAR_FILE).getPath());
        }
    }
    
    /**
     * Starts listening for wake words
     */
    public void startListening() {
        if (recognizer == null) {
            Log.e(TAG, "Recognizer not initialized");
            return;
        }
        
        Log.i(TAG, "Starting PocketSphinx wake word detection");
        isListening = true;
        
        // Start recognition
        recognizer.startListening(WAKE_WORD_SEARCH_NAME);
    }
    
    /**
     * Stops listening for wake words
     */
    public void stopListening() {
        if (recognizer != null) {
            recognizer.cancel();
            isListening = false;
            Log.i(TAG, "Stopped PocketSphinx wake word detection");
        }
    }
    
    /**
     * Called when PocketSphinx detects a hypothesis
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            String text = hypothesis.getHypstr().toLowerCase();
            Log.d(TAG, "PocketSphinx recognized: " + text);
            
            // Check if the recognized text matches any of our wake words
            for (String wakeWord : EGYPTIAN_WAKE_WORDS) {
                if (text.contains(wakeWord)) {
                    Log.i(TAG, "Wake word detected: " + wakeWord);
                    if (callback != null) {
                        callback.onWakeWordDetected();
                    }
                    break;
                }
            }
        }
        
        // Restart listening if still active
        if (isListening) {
            recognizer.startListening(WAKE_WORD_SEARCH_NAME);
        }
    }
    
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        // Not needed for wake word detection
    }
    
    @Override
    public void onTimeout() {
        Log.d(TAG, "PocketSphinx timeout - restarting listening");
        if (isListening) {
            recognizer.startListening(WAKE_WORD_SEARCH_NAME);
        }
    }
    
    /**
     * Destroys the recognizer and cleans up resources
     */
    public void destroy() {
        if (recognizer != null) {
            recognizer.removeListener(this);
            recognizer.cancel();
            recognizer.shutdown();
            recognizer = null;
        }
        
        if (executor != null) {
            executor.shutdown();
        }
        
        Log.i(TAG, "PocketSphinx wake word detector destroyed");
    }
}