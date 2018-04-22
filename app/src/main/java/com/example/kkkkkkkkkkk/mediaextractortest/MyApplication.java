package com.example.kkkkkkkkkkk.mediaextractortest;

import android.app.Application;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by fundamental on 2018/4/21.
 */

public class MyApplication extends Application {
    private static Context mContext;
    public static int screenWidth;
    public static int screenHeight;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        DisplayMetrics mDisplayMetrics = getApplicationContext().getResources()
                .getDisplayMetrics();
        screenWidth = mDisplayMetrics.widthPixels;
        screenHeight = mDisplayMetrics.heightPixels;
    }

    public static Context getContext() {
        return mContext;
    }
}
