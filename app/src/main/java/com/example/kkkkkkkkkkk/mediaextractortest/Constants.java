package com.example.kkkkkkkkkkk.mediaextractortest;

import android.os.Environment;

import java.io.File;

/**
 * Created by fundamental on 2018/4/21.
 */

public class Constants {

    public static final boolean Debug = BuildConfig.DEBUG;
    public static final String NAME = "AudioEdit";

    public static final int ExportChannelNumber = 1;  // 输出声道为单声道
    public static final int ExportByteNumber = 2; //输出采样精度字节数
    public static final int ExportSampleRate = 16000; //输出采样率

    public static final int OneSecond = 1000;

    public static final int NormalMaxProgress = 100;

    public static boolean isBigEnding = false;

    public static String SUFFIX_WAV = ".wav";
    public static String SUFFIX_PCM = ".pcm";

    public static String getBaseFolder() {
        String baseFolder = Environment.getExternalStorageDirectory() + "/Codec/";
        File f = new File(baseFolder);
        if (!f.exists()) {
            boolean b = f.mkdirs();
            if (!b) {
                baseFolder = MyApplication.getContext().getExternalFilesDir(null).getAbsolutePath() + "/";
            }
        }
        return baseFolder;
    }
    //获取VideoPath
    public static String getPath(String path, String fileName) {
        String p = getBaseFolder() + path;
        File f = new File(p);
        if (!f.exists() && !f.mkdirs()) {
            return getBaseFolder() + fileName;
        }
        return p + fileName;
    }


}
