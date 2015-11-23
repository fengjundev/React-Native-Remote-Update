package com.doubanmovie;

/**
 * Created by fengjun on 15/11/20.
 */
public class RemoteSupportManager {

    private static final String JS_LOCAL_CACHE_FILE = "/mnt/sdcard/index.android.bundle";

    private static final String JS_SERVER_URL = "http://172.18.16.43:8080/download/index.android.bundle";


    public static String getSourceUrl() {
        return JS_SERVER_URL;
    }

    public static String getDownloadedJSBundleFile(){
        return JS_LOCAL_CACHE_FILE;
    }
}
