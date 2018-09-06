package com.dgcheshang.cheji.Tools;

import java.io.File;

/**
 * Created by Administrator on 2018/8/31 0031.
 */

public class FileUtil {

    //判断文件是否存在
    public static boolean fileIsExists(String strFile)
    {
        try
        {
            File f=new File(strFile);
            if(!f.exists())
            {
                return false;
            }

        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    //文件长度
    public static long fileLength(String strFile)
    {
        try
        {
            File f=new File(strFile);
            if(!f.exists())
            {
                return 0;
            }else {
                return f.length();
            }

        }
        catch (Exception e)
        {
            return 0;
        }
    }
}
