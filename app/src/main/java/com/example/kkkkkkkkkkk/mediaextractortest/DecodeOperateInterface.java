package com.example.kkkkkkkkkkk.mediaextractortest;

/**
 * Created by fundamental on 2018/4/21.
 */

/**
 * 音频解码监听器
 */
public interface DecodeOperateInterface {
    public void updateDecodeProgress(int decodeProgress);

    public void decodeSuccess();

    public void decodeFail();
}
