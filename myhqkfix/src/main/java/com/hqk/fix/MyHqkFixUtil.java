package com.hqk.fix;

import android.content.Context;

public class MyHqkFixUtil {

    private FixManager fixManager;

    private MyHqkFixUtil() {
        fixManager = FixManager.getInstance();
    }

    private static MyHqkFixUtil myHqkFixUtil = new MyHqkFixUtil();

    public static MyHqkFixUtil getInstance() {
        return myHqkFixUtil;
    }


    public void loadDex(Context context) {
        fixManager.loadDex(context);
    }

    public void fix(Context context,String name){
        fixManager.fix(context,name);
    }
    public void fix(Context context,String name,String pathDir){
        fixManager.fix(context,name,pathDir);
    }


}
