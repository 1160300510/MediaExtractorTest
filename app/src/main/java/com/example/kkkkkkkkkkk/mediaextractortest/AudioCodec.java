package com.example.kkkkkkkkkkk.mediaextractortest;

/**
 * Created by kkkkkkkkkkk on 2018/3/9.
 */

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class AudioCodec {
    final static int TIMEOUT_USEC = 0;
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static AudioDecodeListener listener;
    private static int mSampleRate;
    private static int channel;

    /**
     * 从视频文件中分离出音频，并保存在本地
     */


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static void getAudioFromVideo(String videoPath, final String audioSavePath, final AudioDecodeListener listener){
        final MediaExtractor extractor = new MediaExtractor();
        int audioTrack = -1;
        boolean hasAudio = false;
        try {
            extractor.setDataSource(videoPath);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioTrack = i;
                    hasAudio = true;
                    mSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channel = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                    Log.e("notice","---读取音频数据，采样率-----：：："+mSampleRate);
                    Log.e("notice","---读取音频数据，声道数-----：：："+channel);

                    break;
                }
            }
            if (hasAudio) {

                extractor.selectTrack(audioTrack);
                final int finalAudioTrack = audioTrack;
                new Thread(new Runnable(){
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                    @Override
                    public void run() {
                        try{
                            MediaMuxer mediaMuxer = new MediaMuxer(audioSavePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                            MediaFormat trackFormat = extractor.getTrackFormat(finalAudioTrack);
                            int writeAudioIndex = mediaMuxer.addTrack(trackFormat);
                            mediaMuxer.start();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE));
                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                            extractor.readSampleData(byteBuffer, 0);
                            if (extractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                                extractor.advance();
                            }

                            while (true) {
                                int readSampleSize = extractor.readSampleData(byteBuffer, 0);
                                //Log.e("notice","---读取音频数据，采样率-----：：："+mSampleRate);
                                //Log.e("notice","---读取音频数据，声道数-----：：："+channel);
                                Log.e("hero","---读取音频数据，当前读取到的大小-----：：："+readSampleSize);
                                if (readSampleSize < 0) {
                                    break;
                                }

                                bufferInfo.size = readSampleSize;
                                bufferInfo.flags = extractor.getSampleFlags();
                                bufferInfo.offset = 0;
                                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                                Log.e("hero","----写入音频数据---当前的时间戳：：："+extractor.getSampleTime());

                                mediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo);
                                extractor.advance();//移动到下一帧
                            }
                            mediaMuxer.release();
                            extractor.release();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null){
                                        listener.decodeOver();
                                    }
                                }
                            });
                        }catch (Exception e){
                            e.printStackTrace();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (listener != null){
                                        listener.decodeFail();
                                    }
                                }
                            });
                        }
                    }
                }).start();
            }else {
                Log.e("hero", " extractor failed !!!! 没有音频信道");
                if (listener != null){
                    listener.decodeFail();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e("hero", " extractor failed !!!!");
            if (listener != null){
                listener.decodeFail();
            }
        }
    }

    /**
     * 将音频文件解码成原始的PCM数据
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static void getPCMFromAudio(String audioPath, String audioSavePath, final AudioDecodeListener listener) {
        AudioCodec.listener = listener;
        MediaExtractor extractor = new MediaExtractor();
        int audioTrack = -1;
        boolean hasAudio = false;
        try {
            extractor.setDataSource(audioPath);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioTrack = i;
                    hasAudio = true;
                    break;
                }
            }
            if (hasAudio) {
                extractor.selectTrack(audioTrack);

                //原始音频解码
                new Thread(new AudioDecodeRunnable(extractor, audioTrack, audioSavePath, new DecodeOverListener() {
                    @Override
                    public void decodeIsOver() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null){
                                    listener.decodeOver();
                                }
                            }
                        });
                    }
                    @Override
                    public void decodeFail() {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null){
                                    listener.decodeFail();
                                }
                            }
                        });
                    }
                })).start();

            } else {
                Log.e("hero", " select audio file has no auido track");
                if (listener != null){
                    listener.decodeFail();
                }
            }
        } catch (IOException  e) {
            e.printStackTrace();
            Log.e("hero", " decode failed !!!!");
            if (listener != null){
                listener.decodeFail();
            }
        }


    }


    interface DecodeOverListener {
        void decodeIsOver();

        void decodeFail();
    }

    public interface AudioDecodeListener{
        void decodeOver();
        void decodeFail();
    }
    /**
     * PCM文件转音频
     * 从pcm文件中读取byte数据
     * byte数据放进编码器
     * 把编码后的数据放进文件
     * */
    public static void PCM2Audio(String pcmPath,String audioPath,final AudioDecodeListener listener){
        new Thread(new AudioEncodeRunnable(pcmPath, audioPath, new AudioDecodeListener() {
            @Override
            public void decodeOver() {
                if (listener != null){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.decodeOver();
                        }
                    });
                }
            }

            @Override
            public void decodeFail() {
                if (listener != null){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.decodeFail();
                        }
                    });
                }
            }
        })).start();
    }

    /**
     * 写入ADTS头部数据
     * */
    public static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


}
