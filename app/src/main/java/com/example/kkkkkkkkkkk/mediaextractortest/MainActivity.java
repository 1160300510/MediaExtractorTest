package com.example.kkkkkkkkkkk.mediaextractortest;

import android.media.AudioRecord;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static junit.framework.Assert.fail;

/**
 * 此函数用pocketsSphinx将wav文件进行语音识别
 */

public class MainActivity extends AppCompatActivity {

    protected static final String TAG = SpeechRecognizer.class.getSimpleName();
    protected static Decoder d;
    protected static File assetsDir;
    Button myButton;

    String outputFileString = null;
    String wavOutputFileString = null;
    private AudioRecord recorder = null;
    private static String fileName = null;
    private static int fileCounter = 0;
    private static File outputFile;
    File wavOutputFile = null;
    private static byte[] buffer;
    int bufferSize;
    private static boolean recording = false;
    private String selectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Setting up the recognizer, one moment...");

        //Initial setup of the PocketSphinx Decoder for use within the application
        setupTask();

        //file path to the directory in which the recordings will be stored
        //fileName = "/storage/emulated/0/Android/data/edu.cmu.sphinx.pocketsphinx/files/sync"+File.separator;
        selectedFile = "2018_01_25_19_55_13.wav";

        ////disables button when app boots up
        //findViewById(R.id.button_send).setEnabled(false);
        myButton = (Button) findViewById(R.id.button_send);
        //add click listener to handle user interaction
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //changeButton("Processing File");
                //passes the user selectied file into text extraction method
                convertFileToSpeech(v, d, assetsDir);
            }
        });
    }

    //convert an inputstream to text
    static {
        System.loadLibrary("pocketsphinx_jni");
    }

    private void setupTask() {
        //Setup is done as an asynchronous task to allow user to pick recoding/file option as the
        //decoder is being setup
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Assets assets = null;
                try {
                    assets = new Assets(MainActivity.this);
                    assetsDir = assets.syncAssets();//复制资源文件
                    Config c = Decoder.defaultConfig();
                    c.setString("-hmm", new File(assetsDir, "en-us-ptm").getPath());
                    c.setString("-dict", new File(assetsDir, "cmudict-en-us.dict").getPath());
                    c.setBoolean("-allphone_ci", true);
                    c.setFloat("-kws_threshold", 1e-45f);
                    c.setString("-lm", new File(assetsDir, "en-us.lm.dmp").getPath());
                    d = new Decoder(c);
                    Log.i(TAG, "Done.....");
                    return "Recognizer Setup Complete.";

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            //once setup is complete inform the user
            protected void onPostExecute(String result) {
                updateView(result);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //changes the audio button to display appropriate text
    private void updateView(String phrase) {
        ((TextView) findViewById(R.id.caption_text))
                .setText(phrase);
        //findViewById(R.id.audio_button).setEnabled(true);
    }

    //disables main button in home view
    /*private void changeButton(String action) {
        ((Button) findViewById(R.id.button_send)).setText(action);
        findViewById(R.id.button_send).setEnabled(false);
    }*/

    //method that tales a pre-recorded voice file and extracts the text. This file format must
    //be in .wav format.
    private void convertFileToSpeech(View v, final Decoder d, final File assetsDir) {

        //Parsing the file is done asynchronously and delegated to a new thread to keep the load
        // on the main thread minimal and speed up the extraction process.
        new AsyncTask<Void, Void, String>() {

            @Override
            //This task will be performed in the background
            protected String doInBackground(Void... params) {
                String output = null;
                try {
                    //user selected file is added to to an inputStream from which it will be read.
                    File wavFile = new File(assetsDir, selectedFile);
                    InputStream stream = new FileInputStream(wavFile);
                    /////////////////////////////////////////////////
                    d.startUtt();//Decoder set to start

                    //A buffer that will be used to hold chunks of our input file stream
                    byte[] b = new byte[4096];
                    try {
                        int nbytes;
                        while ((nbytes = stream.read(b)) >= 0) {
                            ByteBuffer bb = ByteBuffer.wrap(b, 0, nbytes);
                            // Not needed on desktop but required on android
                            bb.order(ByteOrder.LITTLE_ENDIAN);
                            short[] s = new short[nbytes / 2];
                            bb.asShortBuffer().get(s);
                            d.processRaw(s, nbytes / 2, false, false);
                        }
                    } catch (IOException e) {
                        fail("Error when reading inputstream" + e.getMessage());
                    }
                    d.endUtt();//Decoder ends processing the input stream
                    ///////////////////////////////////////////////////////
                    //Hypothesis string is the text that the decoder extracted from the file
                    String text = d.hyp().getHypstr();
                    Log.i(TAG, text);
                    return d.hyp().getHypstr();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            //Task performed after execution on main activity.
            //Method updated the View to display the resulting string from the file.
            protected void onPostExecute(String result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Extracted Text:");
                    ((TextView) findViewById(R.id.result_text)).setText(result);
                    findViewById(R.id.button_send).setEnabled(true);
                    String str1 = "Process File";
                    ((Button) findViewById(R.id.button_send)).setText(str1);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}
