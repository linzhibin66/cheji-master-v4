/*
 * FaceDetJniJni.java
 * FaceDetJniJni
 * 
 * Github: https://github.com/daniulive/SmarterStreaming
 * 
 * Created by DaniuLive on 2015/09/20.
 * Copyright © 2014~2016 DaniuLive. All rights reserved.
 */

package com.chainway.facedet;

//import java.nio.ByteBuffer;
import android.util.Log;

import java.util.List;

public class FaceDetJni {	
	
	/**
	 * Initialized publisher.
	 *
	 * @param ctx: get by this.getApplicationContext()
	 * 
	 * @param audio_opt: if with 0: it does not publish audio; if with 1, it publish audio; if with 2, it publish external encoded audio, only support aac.
	 * 
	 * @param video_opt: if with 0: it does not publish video; if with 1, it publish video; if with 2, it publish external encoded video, only support h264, data:0000000167....
	 * 
	 * @param width: capture width; height: capture height.
	 *
	 * <pre>This function must be called firstly.</pre>
	 *
	 * @return {0} if successful
	 */
	 private static boolean isLoaded;
	 private final String TAG = this.getClass().toString();
	 private FD_FSDKFace[] mFaces = new FD_FSDKFace[16];
	 private int mFaceCount = 0;

	 private FD_FSDKFace[] obtainFaceArray(int size) {
		if(mFaceCount < size) {
			if(mFaces.length < size) {
				mFaces = new FD_FSDKFace[(size / 16 + 1) * 16];
			}

			for(int i = mFaceCount; i < size; ++i) {
				mFaces[i] = new FD_FSDKFace();
			}
			mFaceCount = size;
		}
		return mFaces;
	 }

	 public boolean FD_FSDK_FaceDetection(String path, List<FD_FSDKFace> list) {
		if(list != null && path != null) {
			int count = FD_Process(path);
			Log.d(TAG,"********get FaceCnt="+count);
			//this.error.mCode = this.FD_GetErrorCode(this.handle.intValue());
			if(count > 0) {
				FD_FSDKFace[] result = obtainFaceArray(count);
				FD_GetResult(result[0]);
				//result[0].SetRect();
				for(int i = 0; i < count; ++i) {
					list.add(result[i]);
				}
                return true;
			}
		}
        return false;
	 }

	 static {
		if (!isLoaded) {			
			System.loadLibrary("face_jni");
			isLoaded = true;
		}
	 }
	/**
	 * FaceDetInit
	 *
	 * @param path1: landmarks path
	 * @param path2: facenet path
	 * @param path2: train path
	 * @return {0} if successful
	 */

	  public native boolean FaceDetInit(String path1, String path2, String path3);

    
	 /**
	  * CaptureFace
	  * 
	  * @param path: face image path
	  * @return {0} if successful
	  */
	   public native boolean CaptureFace(String path);

    /**
     * CaptureFace
     *
     * @param path: face image path
     * @param name: people name
     * @return {0} if successful
     */
    public native boolean CaptureFaceMuti(String path, String name);

	 /**
	 * Set Video HW Encoder, if support HW encoder, it will return 0
	 *
	 * @param path: face image path
	 *
	 * @return {0} if successful
	 */
	 public native int FD_Process(String path);
	/**
	 * Set Video HW Encoder, if support HW encoder, it will return 0
	 *
	 * @param FD_FSDKFaceVal: FD_FSDKFaceVal
	 *
	 * @return {0} if successful
	 */
	 public native int FD_GetResult(FD_FSDKFace FD_FSDKFaceVal);
   
	 /**
	 * Stop publish stream
	 *
	 * @param path: face image path
	 *
	 * @return {0} if successful
	 */
	 public native float FaceDetect(String path);

    /**
     * Stop publish stream
     *
     * @param path: face image path
     *
     * @return {0} if successful
     */
	 public native String FaceDetectMuti(String path, float thd);
       
    
	/**
	* Stop recorder
	*
	* @return {0} if successful
	*/
	  public native void FaceDetDeInit();
    
   
    /*********增加新的接口  -- ***************/      
}
