package com.hqk.my.fix;

import android.app.Application;
import android.content.Context;

import com.hqk.fix.MyHqkFixUtil;

/**
 * @创建者 HQK
 * @创建时间 2019/10/14 17:00
 * @描述
 */
public class MyApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        MyHqkFixUtil.getInstance().loadDex(base);
        super.attachBaseContext(base);
    }
}
