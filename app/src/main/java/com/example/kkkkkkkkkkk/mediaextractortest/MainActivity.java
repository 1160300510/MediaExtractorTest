package com.example.kkkkkkkkkkk.mediaextractortest;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.SyncStateContract;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
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
    private String mVideoPath;
    private String pcmPath;
    Button myButton;

    String outputFileString = null;
    private String wavOutputFileString;
    private AudioRecord recorder = null;
    private static String fileName = null;
    private static int fileCounter = 0;
    private static File outputFile;
    File wavOutputFile = null;
    private static byte[] buffer;
    int bufferSize;
    private static boolean recording = false;
    private String selectedFile;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Setting up the recognizer, one moment...");

        //Initial setup of the PocketSphinx Decoder for use within the application
        setupTask();
        //selectedFile = "2018_01_25_19_55_13.wav";//选择的视频地址

        mVideoPath = Constants.getPath("video/outputVideo/", "0.blv");


        /*final String pcmPath = Constants.getPath("audio/outputAudio/", "audio_"+"1.pcm");//视频音乐
        AudioCodec.getPCMFromAudio(mVideoPath, pcmPath, new AudioCodec.AudioDecodeListener() {
            @Override
            public void decodeOver() {
                Toast.makeText(MainActivity.this,"分离完毕 PCM保存路径为----  "+pcmPath,Toast.LENGTH_SHORT).show();
                //endLoading();
            }

            @Override
            public void decodeFail() {
                Toast.makeText(MainActivity.this,"分离失败 maybe same Exception ，please look at logcat ",Toast.LENGTH_SHORT).show();
                //endLoading();
            }
        });*/

        //先从视频中分离出音频
        selectedFile = Constants.getPath("audio/outputAudio/", "audio_"+"1.wav");
        AudioCodec.getAudioFromVideo(mVideoPath, selectedFile, new AudioCodec.AudioDecodeListener() {
            @Override
            public void decodeOver() {
                Toast.makeText(MainActivity.this,"分离完毕 音频保存路径为----  "+selectedFile,Toast.LENGTH_SHORT).show();
                //endLoading();
                final String pcmPath = Constants.getPath("audio/outputAudio/", "audio_"+"1.pcm");
                AudioCodec.getPCMFromAudio(selectedFile, pcmPath, new AudioCodec.AudioDecodeListener() {
                    @Override
                    public void decodeOver() {
                        Toast.makeText(MainActivity.this,"解码完毕 音频保存路径为----  "+pcmPath,Toast.LENGTH_SHORT).show();
                        String selectedFile1 = Constants.getPath("audio/outputAudio/", "audio_2.wav");

                        //rawToWav(pcmPath, selectedFile);
                        //new PcmToWavUtil().pcmToWav(pcmPath,selectedFile1);
                        /*AudioCodec.PCM2Audio(pcmPath, selectedFile, new AudioCodec.AudioDecodeListener() {
                            @Override
                            public void decodeOver() {
                                Toast.makeText(MainActivity.this,"解码完毕 音频保存路径为----  "+selectedFile,Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void decodeFail() {
                                Toast.makeText(MainActivity.this,"解码失败 maybe same Exception ，please look at logcat ",Toast.LENGTH_SHORT).show();
                            }
                        });*/
                    }

                    @Override
                    public void decodeFail() {
                        Toast.makeText(MainActivity.this,"解码失败 maybe same Exception ，please look at logcat ",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void decodeFail() {
                Toast.makeText(MainActivity.this,"分离失败 maybe same Exception ，please look at logcat ",Toast.LENGTH_SHORT).show();
                //endLoading();
            }
        });

        //再将音频解码成pcm数据




        //最后将pcm数据编码成目标wav格式的音频文件


        //file path to the directory in which the recordings will be stored
        //fileName = "/storage/emulated/0/Android/data/edu.cmu.sphinx.pocketsphinx/files/sync"+File.separator;


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
                    File wavFile = new File(selectedFile);
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

    /**
     * 获取默认的文件路径
     *
     * @return
     */
    public static String getDefaultFilePath(String path) {
        String filepath = "";
        File file = new File(Environment.getExternalStorageDirectory(),
                path);
        if (file.exists()) {
            filepath = file.getAbsolutePath();
        } else {
            //路径不存在，创建
            File dir = new File(path);
            if (!dir.exists()) {
                try {
                    //在指定的文件夹中创建文件
                    dir.createNewFile();
                } catch (Exception e) {
                }
            }
        }
        return filepath;
    }

    //This method writes a wav header to a file. This is used to iniitate our wav file, before filling it up with
    //data from the raw file. The specifications for the header are obtained through the method parameters.
    private void WriteWaveFileHeader(
            FileOutputStream out, long audioLength,
            long dataLength, long longSampleRate, int channels,
            long byteRate) throws IOException {

        //The byte array that will temporarily hold the header
        byte[] header = new byte[44];

        header[0] = 'R'; //ChunkID
        header[1] = 'I'; //ChunkID
        header[2] = 'F'; //ChunkID
        header[3] = 'F'; //ChunkID
        header[4] = (byte) (dataLength & 0xff); //ChunkSize
        header[5] = (byte) ((dataLength >> 8) & 0xff); //ChunkSize
        header[6] = (byte) ((dataLength >> 16) & 0xff); //ChunkSize
        header[7] = (byte) ((dataLength >> 24) & 0xff); //ChunkSize
        header[8] = 'W'; //Format
        header[9] = 'A'; //Format
        header[10] = 'V'; //Format
        header[11] = 'E'; //Format
        header[12] = 'f'; //Subchunk1ID
        header[13] = 'm'; //Subchunk1ID
        header[14] = 't'; //Subchunk1ID
        header[15] = ' '; //Subchunk1ID
        header[16] = 16; //Subchunk1Size
        header[17] = 0; //Subchunk1Size
        header[18] = 0; //Subchunk1Size
        header[19] = 0; //Subchunk1Size
        header[20] = 1; //AudioFormat
        header[21] = 0; ////AudioFormat
        header[22] = (byte) channels; //NumChannels
        header[23] = 0; //NumChannels
        header[24] = (byte) (longSampleRate & 0xff); //SampleRate
        header[25] = (byte) ((longSampleRate >> 8) & 0xff); //SampleRate
        header[26] = (byte) ((longSampleRate >> 16) & 0xff); //SampleRate
        header[27] = (byte) ((longSampleRate >> 24) & 0xff); //SampleRate
        header[28] = (byte) (byteRate & 0xff); //ByteRate
        header[29] = (byte) ((byteRate >> 8) & 0xff); //ByteRate
        header[30] = (byte) ((byteRate >> 16) & 0xff); //ByteRate
        header[31] = (byte) ((byteRate >> 24) & 0xff); //ByteRate
        header[32] = (byte) (2 * 16 / 8); //BlockAlign
        header[33] = 0; //BlockAlign
        header[34] = 16; //BitsPerSamples
        header[35] = 0; //BitsPerSamples
        header[36] = 'd'; //Subchunk2ID
        header[37] = 'a'; //Subchunk2ID
        header[38] = 't'; //Subchunk2ID
        header[39] = 'a'; //Subchunk2ID
        header[40] = (byte) (audioLength & 0xff); //Subchunk2Size
        header[41] = (byte) ((audioLength >> 8) & 0xff); //Subchunk2Size
        header[42] = (byte) ((audioLength >> 16) & 0xff); //Subchunk2Size
        header[43] = (byte) ((audioLength >> 24) & 0xff); //Subchunk2Size

        //Writes our recently created header to the output file.
        out.write(header, 0, 44);
    }


    /**
     * 将音乐文件解码
     *
     * @param musicFileUrl 源文件路径
     * @param decodeFileUrl 解码文件路径
     * @param startMicroseconds 开始时间 微秒
     * @param endMicroseconds 结束时间 微秒
     * @param decodeOperateInterface 解码过程回调
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean decodeMusicFile(String musicFileUrl, String decodeFileUrl,
                                    long startMicroseconds, long endMicroseconds, DecodeOperateInterface decodeOperateInterface) {

        //采样率，声道数，时长，音频文件类型
        int sampleRate = 0;
        int channelCount = 0;
        long duration = 0;
        String mime = null;

        //MediaExtractor, MediaFormat, MediaCodec
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaFormat mediaFormat = null;
        MediaCodec mediaCodec = null;

        //给媒体信息提取器设置源音频文件路径
        try {
            mediaExtractor.setDataSource(musicFileUrl);
        }catch (Exception ex){
            ex.printStackTrace();
            try {
                mediaExtractor.setDataSource(new FileInputStream(musicFileUrl).getFD());
            } catch (Exception e) {
                e.printStackTrace();
                LogUtil.e("设置解码音频文件路径错误");
            }
        }

        //获取音频格式轨信息
        mediaFormat = mediaExtractor.getTrackFormat(0);

        //从音频格式轨信息中读取 采样率，声道数，时长，音频文件类型
        sampleRate = mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? mediaFormat.getInteger(
                MediaFormat.KEY_SAMPLE_RATE) : 44100;
        channelCount = mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? mediaFormat.getInteger(
                MediaFormat.KEY_CHANNEL_COUNT) : 1;
        duration = mediaFormat.containsKey(MediaFormat.KEY_DURATION) ? mediaFormat.getLong(
                MediaFormat.KEY_DURATION) : 0;
        mime = mediaFormat.containsKey(MediaFormat.KEY_MIME) ? mediaFormat.getString(MediaFormat.KEY_MIME)
                : "";

        LogUtil.i("歌曲信息Track info: mime:"
                + mime
                + " 采样率sampleRate:"
                + sampleRate
                + " channels:"
                + channelCount
                + " duration:"
                + duration);

        if (!mime.startsWith("audio/")) {
            LogUtil.e("解码文件不是音频文件mime:" + mime);
            return false;
        }

        if (mime.equals("audio/ffmpeg")) {
            mime = "audio/mpeg";
            mediaFormat.setString(MediaFormat.KEY_MIME, mime);
        }

        if (duration <= 0) {
            LogUtil.e("音频文件duration为" + duration);
            return false;
        }

        //解码的开始时间和结束时间
        startMicroseconds = Math.max(startMicroseconds, 0);
        endMicroseconds = endMicroseconds < 0 ? duration : endMicroseconds;
        endMicroseconds = Math.min(endMicroseconds, duration);

        if (startMicroseconds >= endMicroseconds) {
            return false;
        }

        //创建一个解码器
        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);

            mediaCodec.configure(mediaFormat, null, null, 0);
        } catch (Exception e) {
            LogUtil.e("解码器configure出错");
            return false;
        }

        //得到输出PCM文件的路径
        decodeFileUrl = decodeFileUrl.substring(0, decodeFileUrl.lastIndexOf("."));
        String pcmFilePath = decodeFileUrl + ".pcm";

        //后续解码操作
        getDecodeData(mediaExtractor, mediaCodec, pcmFilePath, sampleRate, channelCount,
                startMicroseconds, endMicroseconds, decodeOperateInterface);

        return true;
    }

    /**
     * 解码数据
     */

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void getDecodeData(MediaExtractor mediaExtractor, MediaCodec mediaCodec,
                               String decodeFileUrl, int sampleRate, int channelCount, final long startMicroseconds,
                               final long endMicroseconds, final DecodeOperateInterface decodeOperateInterface) {

        //初始化解码状态，未解析完成
        boolean decodeInputEnd = false;
        boolean decodeOutputEnd = false;

        //当前读取采样数据的大小
        int sampleDataSize;
        //当前输入数据的ByteBuffer序号，当前输出数据的ByteBuffer序号
        int inputBufferIndex;
        int outputBufferIndex;
        //音频文件的采样位数字节数，= 采样位数/8
        int byteNumber;

        //上一次的解码操作时间，当前解码操作时间，用于通知回调接口
        long decodeNoticeTime = System.currentTimeMillis();
        long decodeTime;

        //当前采样的音频时间，比如在当前音频的第40秒的时候
        long presentationTimeUs = 0;

        //定义编解码的超时时间
        final long timeOutUs = 100;

        //存储输入数据的ByteBuffer数组，输出数据的ByteBuffer数组
        ByteBuffer[] inputBuffers;
        ByteBuffer[] outputBuffers;

        //当前编解码器操作的 输入数据ByteBuffer 和 输出数据ByteBuffer，可以从targetBuffer中获取解码后的PCM数据
        ByteBuffer sourceBuffer;
        ByteBuffer targetBuffer;

        //获取输出音频的媒体格式信息
        MediaFormat outputFormat = mediaCodec.getOutputFormat();

        MediaCodec.BufferInfo bufferInfo;

        byteNumber = (outputFormat.containsKey("bit-width") ? outputFormat.getInteger("bit-width") : 0) / 8;

        //开始解码操作
        mediaCodec.start();

        //获取存储输入数据的ByteBuffer数组，输出数据的ByteBuffer数组
        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();

        mediaExtractor.selectTrack(0);

        //当前解码的缓存信息，里面的有效数据在offset和offset+size之间
        bufferInfo = new MediaCodec.BufferInfo();

        //获取解码后文件的输出流
        BufferedOutputStream bufferedOutputStream =
                FileFunction.getBufferedOutputStreamFromFile(decodeFileUrl);

        //开始进入循环解码操作，判断读入源音频数据是否完成，输出解码音频数据是否完成
        while (!decodeOutputEnd) {
            if (decodeInputEnd) {
                return;
            }

            decodeTime = System.currentTimeMillis();

            //间隔1秒通知解码进度
            if (decodeTime - decodeNoticeTime > Constants.OneSecond) {
                final int decodeProgress =
                        (int) ((presentationTimeUs - startMicroseconds) * Constants.NormalMaxProgress
                                / endMicroseconds);

                if (decodeProgress > 0) {
                    notifyProgress(decodeOperateInterface, decodeProgress);
                }

                decodeNoticeTime = decodeTime;
            }

            try {

                //操作解码输入数据

                //从队列中获取当前解码器处理输入数据的ByteBuffer序号
                inputBufferIndex = mediaCodec.dequeueInputBuffer(timeOutUs);

                if (inputBufferIndex >= 0) {
                    //取得当前解码器处理输入数据的ByteBuffer
                    sourceBuffer = inputBuffers[inputBufferIndex];
                    //获取当前ByteBuffer，编解码器读取了多少采样数据
                    sampleDataSize = mediaExtractor.readSampleData(sourceBuffer, 0);

                    //如果当前读取的采样数据<0，说明已经完成了读取操作
                    if (sampleDataSize < 0) {
                        decodeInputEnd = true;
                        sampleDataSize = 0;
                    } else {
                        presentationTimeUs = mediaExtractor.getSampleTime();
                    }

                    //然后将当前ByteBuffer重新加入到队列中交给编解码器做下一步读取操作
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleDataSize, presentationTimeUs,
                            decodeInputEnd ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    //前进到下一段采样数据
                    if (!decodeInputEnd) {
                        mediaExtractor.advance();
                    }

                } else {
                    //LogUtil.e("inputBufferIndex" + inputBufferIndex);
                }

                //操作解码输出数据

                //从队列中获取当前解码器处理输出数据的ByteBuffer序号
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOutUs);

                if (outputBufferIndex < 0) {
                    //输出ByteBuffer序号<0，可能是输出缓存变化了，输出格式信息变化了
                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            outputBuffers = mediaCodec.getOutputBuffers();
                            LogUtil.e(
                                    "MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED [AudioDecoder]output buffers have changed.");
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            outputFormat = mediaCodec.getOutputFormat();

                            sampleRate =
                                    outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? outputFormat.getInteger(
                                            MediaFormat.KEY_SAMPLE_RATE) : sampleRate;
                            channelCount =
                                    outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? outputFormat.getInteger(
                                            MediaFormat.KEY_CHANNEL_COUNT) : channelCount;
                            byteNumber =
                                    (outputFormat.containsKey("bit-width") ? outputFormat.getInteger("bit-width") : 0)
                                            / 8;

                            LogUtil.e(
                                    "MediaCodec.INFO_OUTPUT_FORMAT_CHANGED [AudioDecoder]output format has changed to "
                                            + mediaCodec.getOutputFormat());
                            break;
                        default:
                            //LogUtil.e("error [AudioDecoder] dequeueOutputBuffer returned " + outputBufferIndex);
                            break;
                    }
                    continue;
                }

                //取得当前解码器处理输出数据的ByteBuffer
                targetBuffer = outputBuffers[outputBufferIndex];

                byte[] sourceByteArray = new byte[bufferInfo.size];

                //将解码后的targetBuffer中的数据复制到sourceByteArray中
                targetBuffer.get(sourceByteArray);
                targetBuffer.clear();

                //释放当前的输出缓存
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);

                //判断当前是否解码数据全部结束了
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    decodeOutputEnd = true;
                }

                //sourceByteArray就是最终解码后的采样数据
                //接下来可以对这些数据进行采样位数，声道的转换，但这是可选的，默认是和源音频一样的声道和采样位数
                if (sourceByteArray.length > 0 && bufferedOutputStream != null) {
                    if (presentationTimeUs < startMicroseconds) {
                        continue;
                    }

                    //采样位数转换，按自己需要是否实现
                    byte[] convertByteNumberByteArray =
                            convertByteNumber(byteNumber, Constants.ExportByteNumber, sourceByteArray);

                    //声道转换，按自己需要是否实现
                    byte[] resultByteArray = convertChannelNumber(channelCount, Constants.ExportChannelNumber,
                            Constants.ExportByteNumber, convertByteNumberByteArray);

                    //将解码后的PCM数据写入到PCM文件
                    try {
                        bufferedOutputStream.write(resultByteArray);
                    } catch (Exception e) {
                        LogUtil.e("输出解压音频数据异常" + e);
                    }
                }

                if (presentationTimeUs > endMicroseconds) {
                    break;
                }
            } catch (Exception e) {
                LogUtil.e("getDecodeData异常" + e);
            }
        }

        if (bufferedOutputStream != null) {
            try {
                bufferedOutputStream.close();
            } catch (IOException e) {
                LogUtil.e("关闭bufferedOutputStream异常" + e);
            }
        }

        //重置采样率，按自己需要是否实现
        if (sampleRate != Constants.ExportSampleRate) {
            Resample(sampleRate, decodeFileUrl);
        }

        notifyProgress(decodeOperateInterface, 100);

        //释放mediaCodec 和 mediaExtractor
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
        }
    }

    /**
     * 重置采样率
     */
    private static void Resample(int sampleRate, String decodeFileUrl) {
        String newDecodeFileUrl = decodeFileUrl + "new";

        try {
            FileInputStream fileInputStream = new FileInputStream(new File(decodeFileUrl));
            FileOutputStream fileOutputStream = new FileOutputStream(new File(newDecodeFileUrl));

            new SSRC(fileInputStream, fileOutputStream, sampleRate, Constants.ExportSampleRate,
                    Constants.ExportByteNumber, Constants.ExportByteNumber, 1, Integer.MAX_VALUE, 0, 0, true);

            fileInputStream.close();
            fileOutputStream.close();

            FileFunction.renameFile(newDecodeFileUrl, decodeFileUrl);
        } catch (IOException e) {
            LogUtil.e("关闭bufferedOutputStream异常" + e);
        }
    }

    /**
     * 重置采样点字节数
     */
    public static byte[] convertByteNumber(int sourceByteNumber, int outputByteNumber,
                                           byte[] sourceByteArray) {
        if (sourceByteNumber == outputByteNumber) {
            return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceByteNumber) {
            case 1:
                switch (outputByteNumber) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte resultByte[];

                        for (int index = 0; index < sourceByteArrayLength; index += 1) {
                            resultByte = CommonFunction.GetBytes((short) (sourceByteArray[index] * 256),
                                    Constants.isBigEnding);

                            byteArray[2 * index] = resultByte[0];
                            byteArray[2 * index + 1] = resultByte[1];
                        }

                        return byteArray;
                    default:
                        break;
                }
                break;
            case 2:
                switch (outputByteNumber) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        for (int index = 0; index < outputByteArrayLength; index += 1) {
                            byteArray[index] = (byte) (CommonFunction.GetShort(sourceByteArray[2 * index],
                                    sourceByteArray[2 * index + 1], Constants.isBigEnding) / 256);
                        }

                        return byteArray;
                    default:
                        break;
                }
                break;
            default:
                break;
        }

        return sourceByteArray;
    }

    /**
     * 重置声道数
     */
    public static byte[] convertChannelNumber(int sourceChannelCount, int outputChannelCount,
                                              int byteNumber, byte[] sourceByteArray) {
        if (sourceChannelCount == outputChannelCount) {
            return sourceByteArray;
        }

        switch (byteNumber) {
            case 1:
            case 2:
                break;
            default:
                return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceChannelCount) {
            case 1:
                switch (outputChannelCount) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte firstByte;
                        byte secondByte;

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < sourceByteArrayLength; index += 1) {
                                    firstByte = sourceByteArray[index];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = firstByte;
                                }
                                break;
                            case 2:
                                for (int index = 0; index < sourceByteArrayLength; index += 2) {
                                    firstByte = sourceByteArray[index];
                                    secondByte = sourceByteArray[index + 1];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = secondByte;
                                    byteArray[2 * index + 2] = firstByte;
                                    byteArray[2 * index + 3] = secondByte;
                                }
                                break;
                            default:
                                break;
                        }

                        return byteArray;
                    default:
                        break;
                }
                break;
            case 2:
                switch (outputChannelCount) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    short averageNumber =
                                            (short) ((short) sourceByteArray[2 * index] + (short) sourceByteArray[2
                                                    * index + 1]);
                                    byteArray[index] = (byte) (averageNumber >> 1);
                                }
                                break;
                            case 2:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    byte resultByte[] =
                                            CommonFunction.AverageShortByteArray(sourceByteArray[2 * index],
                                                    sourceByteArray[2 * index + 1], sourceByteArray[2 * index + 2],
                                                    sourceByteArray[2 * index + 3], Constants.isBigEnding);

                                    byteArray[index] = resultByte[0];
                                    byteArray[index + 1] = resultByte[1];
                                }
                                break;
                            default:
                                break;
                        }

                        return byteArray;
                    default:
                        break;
                }
                break;
            default:
                break;
        }

        return sourceByteArray;
    }

    private void notifyProgress(final DecodeOperateInterface decodeOperateInterface, final int progress){
        App.getInstance().mHandler.post(new Runnable() {
            @Override public void run() {
                if (decodeOperateInterface != null) {
                    decodeOperateInterface.updateDecodeProgress(progress);
                }
            }
        });
    }


}
