package com.sunxy.suntesseract.utils;

import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * --
 * <p>
 * Created by sunxy on 2018/10/8 0008.
 */
public class ORCApi {
    private static final ORCApi ourInstance = new ORCApi();

    public static ORCApi getApi() {
        return ourInstance;
    }

    private String rootPath;
    private Map<Integer, String> typeMap;
    private Map<String, TessBaseAPI> apiMap = new HashMap<>();
    private boolean working;

    private ORCApi() {
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public void setTypeMap(Map<Integer, String> typeMap) {
        this.typeMap = typeMap;
    }

    public void getImageContent(final String path, final int type, final OnORCListener listener){
        if (isWorking()){
            return;
        }
        setWorking(true);
        Disposable subscribe = Observable.just(type)
                .subscribeOn(Schedulers.io())
                .map(new Function<Integer, String>() {
                    @Override
                    public String apply(Integer integer) throws Exception {
                        return getContent(path, type);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        listener.onResultSuccess(s);
                        setWorking(false);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        listener.onResultFile(throwable.getMessage());
                        setWorking(false);
                    }
                });
    }


    public boolean isWorking() {
        return working;
    }

    private void setWorking(boolean working) {
        this.working = working;
    }

    private TessBaseAPI getTessBaseAPIFromType(int type){
        String name = typeMap.get(type);
        if (TextUtils.isEmpty(name)){
            return null;
        }
        TessBaseAPI tessBaseAPI = apiMap.get(name);
        if (tessBaseAPI == null){
            tessBaseAPI = new TessBaseAPI();
            tessBaseAPI.init(rootPath, name);
            apiMap.put(name, tessBaseAPI);
        }
        return tessBaseAPI;
    }


    private String getContent(String path, int type){
        Bitmap bitmap = null;
        try {
            TessBaseAPI tessBaseAPIFromType = getTessBaseAPIFromType(type);
            if (tessBaseAPIFromType == null){
                return null;
            }
            bitmap = ImageUtils.path2Bitmap(path);
            bitmap = ImageUtils.convertToBMW(bitmap, bitmap.getWidth()/2, bitmap.getHeight()/2, 100);
            tessBaseAPIFromType.setImage(bitmap);
            return tessBaseAPIFromType.getUTF8Text();
        }finally {
            if (bitmap != null && !bitmap.isRecycled()){
                bitmap.recycle();
            }
        }
    }

    public interface OnORCListener{
        void onResultSuccess(String result);
        void onResultFile(String error);
    }
}
