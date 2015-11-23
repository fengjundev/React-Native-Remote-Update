package com.doubanmovie;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;

import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.JSBundleLoader;
import com.facebook.react.bridge.JSCJavaScriptExecutor;
import com.facebook.react.bridge.JavaScriptExecutor;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.shell.MainReactPackage;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Base react native which can load js bundle file from remote server to update
 *
 * @author fengjun
 */
public class BaseReactActivity extends Activity implements DefaultHardwareBackBtnHandler {

    private static final String TAG = "BaseReactActivity";

    public static final String JS_BUNDLE_REMOTE_URL = "http://172.18.16.43:8080/download/index.android.bundle";
    public static final String JS_BUNDLE_LOCAL_FILE = "index.android.bundle";
    public static final String JS_BUNDLE_LOCAL_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + JS_BUNDLE_LOCAL_FILE;

    private ReactInstanceManager mReactInstanceManager;
    private ReactRootView mReactRootView;
    private CompleteReceiver mDownloadCompleteReceiver;
    private long mDownloadId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        iniReactRootView();
        setContentView(mReactRootView);
        initDownloadManager();
        updateBundle();
    }

    private void initDownloadManager() {
        mDownloadCompleteReceiver = new CompleteReceiver();
        registerReceiver(mDownloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void iniReactRootView() {
        ReactInstanceManager.Builder builder = ReactInstanceManager.builder()
                .setApplication(getApplication())
                .setJSMainModuleName("index.android")
                .addPackage(new MainReactPackage())
                .setInitialLifecycleState(LifecycleState.RESUMED);

        File file = new File(JS_BUNDLE_LOCAL_PATH);
        if(file != null && file.exists()){
            builder.setJSBundleFile(JS_BUNDLE_LOCAL_PATH);
            Log.i(TAG, "load bundle from local cache");
        }else{
            builder.setBundleAssetName(JS_BUNDLE_LOCAL_FILE);
            Log.i(TAG, "load bundle from asset");
        }

        mReactRootView = new ReactRootView(this);
        mReactInstanceManager = builder.build();
        mReactRootView.startReactApplication(mReactInstanceManager, "DoubanMovie", null);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mDownloadCompleteReceiver);
    }

    private void updateBundle() {

        // Should add version check here, if bundle file
        // is the newest , we do not need to update

        File file = new File(JS_BUNDLE_LOCAL_PATH);
        if(file != null && file.exists()){
            Log.i(TAG, "newest bundle exists !");
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(JS_BUNDLE_REMOTE_URL));
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        request.setDestinationUri(Uri.parse("file://" + JS_BUNDLE_LOCAL_PATH));
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadId = dm.enqueue(request);

        Log.i(TAG, "start download remote bundle");
    }

    private class CompleteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if(completeDownloadId == mDownloadId){
                onJSBundleLoadedFromServer();
            }
        }
    };

    private void onJSBundleLoadedFromServer() {
        File file = new File(JS_BUNDLE_LOCAL_PATH);
        if(file == null || !file.exists()){
            Log.i(TAG, "download error, check URL or network state");
            return;
        }

        Log.i(TAG, "download success, reload js bundle");
        try {
            Class<?> RIManagerClazz = mReactInstanceManager.getClass();
            Method method = RIManagerClazz.getDeclaredMethod("recreateReactContextInBackground",
                    JavaScriptExecutor.class, JSBundleLoader.class);
            method.setAccessible(true);
            method.invoke(mReactInstanceManager,
                    new JSCJavaScriptExecutor(),
                    JSBundleLoader.createFileLoader(getApplicationContext(), JS_BUNDLE_LOCAL_PATH));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && mReactInstanceManager != null) {
            mReactInstanceManager.showDevOptionsDialog();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onResume(this, new DefaultHardwareBackBtnHandler() {
                @Override
                public void invokeDefaultOnBackPressed() {
                    finish();
                }
            });
        }
    }
}
