package com.shijingfeng.app;

import android.app.Application;
import android.util.Log;

/**
 * Function:
 * Date: 2020/5/14 9:52
 * Description:
 * Author: ShiJingFeng
 */
public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("测试", "App onCreate");
    }
}
