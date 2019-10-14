package com.hqk.fix;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

import static android.content.ContentValues.TAG;

/**
 * Created by hqk on 2019/2/12.
 */

public class FixManager {
    private static final FixManager ourInstance = new FixManager();

    public static FixManager getInstance() {
        return ourInstance;
    }

    private HashSet<File> loaded = new HashSet<>();
    private FixManager() {
    }

    public void loadDex(Context context) {
        if (context == null) {
            return;
        }
        File filesDir = context.getDir("odex", Context.MODE_PRIVATE);
        File[]  listFiles=filesDir.listFiles();
        for (File file : listFiles) {
//            如果是classes开到   。dex
            if(file.getName().endsWith(".dex")){
                loaded.add(file);
            }
        }

//        修复
        String optimizeDir = filesDir.getAbsolutePath() + File.separator + "opt_dex";
        File fopt = new File(optimizeDir);
        if (!fopt.exists()) {
            fopt.mkdirs();
        }
        for (File dex : loaded) {
//              private final Element[] dexElements;

//            //        对象成员变量      对象 还原     ----数组   反射调用
//    ------------------------------我们的--------------------------------------------------------
            DexClassLoader dexClassloader = new DexClassLoader(dex.getAbsolutePath(), optimizeDir,
                    null, context.getClassLoader());
//         BaseClassLoader  ----dexPathList   ---->dexElements;
            try {
                Class myDexClazzLoader=Class.forName("dalvik.system.BaseDexClassLoader");
                Field myPathListFiled=myDexClazzLoader.getDeclaredField("pathList");
                myPathListFiled.setAccessible(true);
//dexPathList
                Object myPathListObject =myPathListFiled.get(dexClassloader);

                Class  myPathClazz=myPathListObject.getClass();
                Field  myElementsField = myPathClazz.getDeclaredField("dexElements");//dexElements

                myElementsField.setAccessible(true);
                Object myElements = myElementsField.get(myPathListObject);// Element[]   dexElements
//                myElements  -----》  private final Element[] dexElements;
//                天之道里面去  1号房间

//----------------------------------系统的--------------------------------------------------------
//              PathClassLoader   PathList Element

                PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();
                Class baseDexClazzLoader=Class.forName("dalvik.system.BaseDexClassLoader");
                Field  pathListFiled=baseDexClazzLoader.getDeclaredField("pathList");
                pathListFiled.setAccessible(true);
                Object pathListObject = pathListFiled.get(pathClassLoader);

                Class  systemPathClazz=pathListObject.getClass();
                Field  systemElementsField = systemPathClazz.getDeclaredField("dexElements");
                systemElementsField.setAccessible(true);
                Object systemElements=systemElementsField.get(pathListObject);
//-----------------systemElements-- 所有的房间   5   ----------1+5  -----------------------------------------------
// 其他地方  新建留个房间
//                dalvik.system.Element[] dexElements;
                Class elemnt = systemElements.getClass().getComponentType();
                int systemLength=Array.getLength(systemElements);
                int myLength = Array.getLength(myElements);
                //                生成一个新的 数组   类型为Element类型
               Object newElementArray= Array.newInstance(elemnt, systemLength + myLength);

              for(int i=0;i<myLength+systemLength;i++) {
                  if (i < myLength) {
//                      先插入修复包的dex yes  1
                      Array.set(newElementArray, i, Array.get(myElements,i));
                  }else {
//                      系统的            no  2 索引错了  systemElements  5
                      Array.set(newElementArray, i, Array.get(systemElements,i-myLength));
                  }
              }



                Field  elementsField=pathListObject.getClass().getDeclaredField("dexElements");;
                elementsField.setAccessible(true);

                elementsField.set(pathListObject, newElementArray);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    /**
     * 获取cache路径
     * @param context
     * @return
     */
    public  String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

    /**
     * 调用该方法，必须保证有修复包，并且修复包在自己定义的文件夹内
     * @param context
     * @param name
     */
    public void fix(Context context,String name){
        this.fix(context,name,"");
    }

    public void fix(Context context,String name,String pathDir){
        File filesDir = context.getDir("odex", Context.MODE_PRIVATE);
//        String name = "fix.dex";
        String filePath = new File(filesDir, name).getAbsolutePath();
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        InputStream is = null;
        FileOutputStream os = null;
        if("".equals(pathDir) || pathDir==null){
            pathDir=getDiskCachePath(context);
        }
        try {
            Log.i(TAG, filePath+"      fixBug: " + new File(pathDir, name).getAbsolutePath()+">>>>>"+new File(pathDir, name).exists());
            if(!new File(pathDir, name).exists()){
                throw new RuntimeException("\n\t\t该文件夹路径有问题或文件不存在   \n\t\t文件夹路径为："+pathDir+" \n\t\t文件路径为："+new File(pathDir, name).getAbsolutePath());
            }
            is = new FileInputStream(new File(pathDir, name));

            os = new FileOutputStream(filePath);
            int len = 0;
            byte[] buffer = new byte[1024];

            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
//            getResources()
            File f = new File(filePath);
            FixManager.getInstance().loadDex(context);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("该文件夹路径有问题        路径为："+pathDir);
        } finally {
            try {
                is.close();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
