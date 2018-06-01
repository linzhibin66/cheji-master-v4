package com.dgcheshang.cheji.netty.util;

import android.content.Context;
import android.provider.MediaStore;

import com.dgcheshang.cheji.CjApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 *人脸识别工具
 */

public class RlsbUtil {
    static List<Timer> timers=new ArrayList<>();
    /**
     * 判断文件是否存在
     * */
    public static boolean isFileExist(String path) {
        File file = new File(path);
        if (file.isFile()) {
            return (file.length() > 0);
        }
        return false;
    }
    /**
     * 判断文件夹是否存在,不存在则创建
     * */
    public static void isexistAndBuild(String path){
        //判断文件夹是否存在
        File f=new File(path);
        if(!f.exists()){
            //不存在
            f.mkdirs();
        }
    }
    /**
     * 删除比对失败的照片
     * */

    public static boolean delete(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        } else {
            if (file.isFile()) {

                return  file.delete();


            }else {
                return false;
            }
        }
    }

    public static void addtimer (Timer timer){
        timers.add(timer);
    }

    public static void deltimers (){
        if(timers.size()>0){
            for(int i=0;i<timers.size();i++){
                timers.get(i).cancel();
            }
        }

    }

    public static String getAssetsCacheFile(Context context, String fileName) {
        File cacheFile = new File(context.getFilesDir(), fileName);
        if (!cacheFile.exists()) {
            //printLog("*******cacheFile not exists");
            try {
                InputStream inputStream = context.getAssets().open(fileName);
                try {
                    FileOutputStream outputStream = new FileOutputStream(cacheFile);
                    try {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = inputStream.read(buf)) > 0) {
                            outputStream.write(buf, 0, len);
                        }
                    } finally {
                        outputStream.close();
                    }
                } finally {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //printLog("*******cacheFile=" + cacheFile.getAbsolutePath());
        }
        return cacheFile.getAbsolutePath();
    }
}
