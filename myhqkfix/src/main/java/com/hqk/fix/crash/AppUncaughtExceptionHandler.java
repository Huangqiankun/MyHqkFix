package com.hqk.fix.crash;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 应用异常捕获类
 * Created by hqk
 * on 2019/2/13.
 */

public class AppUncaughtExceptionHandler implements UncaughtExceptionHandler {

    private static final String TAG = "TEST";
    //程序的Context对象
    private Context applicationContext;

    private volatile boolean crashing;
    // 用来显示Toast中的信息
    private static String error = "程序错误，额，不对，我应该说，服务器正在维护中，请稍后再试";
    private static final Map<String, String> regexMap = new HashMap<String, String>();
    /**
     * 日期格式器
     */
    private DateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    /**
     * 系统默认的UncaughtException处理类
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    /**
     * 单例
     */
    private static AppUncaughtExceptionHandler sAppUncaughtExceptionHandler;

    public static synchronized AppUncaughtExceptionHandler getInstance() {
        if (sAppUncaughtExceptionHandler == null) {
            synchronized (AppUncaughtExceptionHandler.class) {
                if (sAppUncaughtExceptionHandler == null) {
                    initMap();
                    sAppUncaughtExceptionHandler = new AppUncaughtExceptionHandler();
                }
            }
        }
        return sAppUncaughtExceptionHandler;
    }

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        applicationContext =context;
        crashing = false;
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }



    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (crashing) {
            return;
        }
        crashing = true;

        Log.e(TAG,"uncaughtException"+ex.toString());
        // 打印异常信息
        ex.printStackTrace();
        // 我们没有处理异常 并且默认异常处理不为空 则交给系统处理
        if (!handlelException(ex) && mDefaultHandler != null) {
            // 系统处理
            mDefaultHandler.uncaughtException(thread, ex);
        }
        byebye();
    }

    private void byebye() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private boolean handlelException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        try {
            // 异常信息
            String crashReport = getCrashReport(ex);
            // TODO: 上传日志到服务器
            // 保存到sd卡
            saveExceptionToSdcard(crashReport);
            // 提示对话框
            showPatchDialog();
        } catch (Exception e) {
            return false;
        }
        return true;
    }


    private void showPatchDialog() {
        Intent intent = PatchDialogActivity.newIntent(applicationContext, getApplicationName(applicationContext), null);
        applicationContext.startActivity(intent);
    }

    private String getApplicationName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ApplicationInfo applicationInfo = null;
        String name = null;
        try {
            applicationInfo = packageManager.getApplicationInfo(
                    context.getApplicationInfo().packageName, 0);
            name = (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (final PackageManager.NameNotFoundException e) {
            String[] packages = context.getPackageName().split(".");
            name = packages[packages.length - 1];
        }
        return name;
    }

    /**
     * 获取异常信息
     * @param ex
     * @return
     */
    private String getCrashReport(Throwable ex) {
        StringBuffer exceptionStr = new StringBuffer();
        PackageInfo pinfo =getPackageInfo();
        if (pinfo != null) {
            if (ex != null) {
                //app版本信息
                exceptionStr.append("App Version：" + pinfo.versionName);
                exceptionStr.append("_" + pinfo.versionCode + "\n");

                //手机系统信息
                exceptionStr.append("OS Version：" + Build.VERSION.RELEASE);
                exceptionStr.append("_");
                exceptionStr.append(Build.VERSION.SDK_INT + "\n");

                //手机制造商
                exceptionStr.append("Vendor: " + Build.MANUFACTURER+ "\n");

                //手机型号
                exceptionStr.append("Model: " + Build.MODEL+ "\n");

                String errorStr = ex.getLocalizedMessage();
                if (TextUtils.isEmpty(errorStr)) {
                    errorStr = ex.getMessage();
                }
                if (TextUtils.isEmpty(errorStr)) {
                    errorStr = ex.toString();
                }

                Log.e(TAG,">>>>>errorStr<<<<"+ex.toString());
                exceptionStr.append("Exception: " + errorStr + "\n");
                StackTraceElement[] elements = ex.getStackTrace();
                if (elements != null) {
                    for (int i = 0; i < elements.length; i++) {
                        if (i == 0) {
                            setError(ex.toString());
                        }
                        exceptionStr.append(elements[i].toString() + "\n");
                    }
                }
            } else {
                exceptionStr.append("no exception. Throwable is null\n");
            }
            return exceptionStr.toString();
        } else {
            return "";
        }
    }


    /**
     * 设置错误的提示语
     *
     * @param e
     */
    public static void setError(String e) {
        Pattern pattern;
        Matcher matcher;
        for (Map.Entry<String, String> m : regexMap.entrySet()) {
            Log.d(TAG, e + "key:" + m.getKey() + "; value:" + m.getValue());
            pattern = Pattern.compile(m.getKey());
            matcher = pattern.matcher(e);
            if (matcher.matches()) {
                error = m.getValue();
                break;
            }
        }
    }

    /**
     * 获取App安装包信息
     *
     * @return
     */
    public PackageInfo getPackageInfo() {
        PackageInfo info = null;
        try {
            info = applicationContext.getPackageManager().getPackageInfo(applicationContext.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return info;
    }

    /**
     * 保存错误报告到sd卡
     * @param errorReason
     */
    private void saveExceptionToSdcard(String errorReason) {
        try {
            Log.e("CrashDemo", "AppUncaughtExceptionHandler执行了一次"+errorReason);
            String time = mFormatter.format(new Date());
            String fileName = "Crash-" + time + ".log";
            if (SdcardConfig.getInstance().hasSDCard()) {
                String path = SdcardConfig.LOG_FOLDER;
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                Log.e(TAG,"path   crash "+path + fileName);
                fos.write(errorReason.getBytes());
                fos.close();
            }
        } catch (Exception e) {
            Log.e("CrashDemo", "an error occured while writing file..." + e.getMessage());
        }
    }



    /**
     * 初始化错误的提示语
     */
    private static void initMap() {
        // Java.lang.NullPointerException
        // java.lang.ClassNotFoundException
        // java.lang.ArithmeticException
        // java.lang.ArrayIndexOutOfBoundsException
        // java.lang.IllegalArgumentException
        // java.lang.IllegalAccessException
        // SecturityException
        // NumberFormatException
        // OutOfMemoryError
        // StackOverflowError
        // RuntimeException
        regexMap.put(".*NullPointerException.*", "嘿，无中生有~Boom!");
        regexMap.put(".*ClassNotFoundException.*", "你确定你能找得到它？");
        regexMap.put(".*ArithmeticException.*", "我猜你的数学是体育老师教的，对吧？");
        regexMap.put(".*ArrayIndexOutOfBoundsException.*", "恩，无下限=无节操，请不要跟我搭话");
        regexMap.put(".*IllegalArgumentException.*", "你的出生就是一场错误。");
        regexMap.put(".*IllegalAccessException.*", "很遗憾，你的信用卡账号被冻结了，无权支付");
        regexMap.put(".*SecturityException.*", "死神马上降临");
        regexMap.put(".*NumberFormatException.*", "想要改变一下自己形象？去泰国吧，包你满意");
        regexMap.put(".*OutOfMemoryError.*", "或许你该减减肥了");
        regexMap.put(".*StackOverflowError.*", "啊，啊，憋不住了！");
        regexMap.put(".*RuntimeException.*", "你的人生走错了方向，重来吧");
    }
}

