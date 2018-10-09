package com.sunxy.suntesseract;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.sunxiaoyu.utils.UtilsCore;
import com.sunxiaoyu.utils.core.utils.ToastUtils;
import com.sunxy.suntesseract.utils.ORCApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * Created by sunxy on 2018/10/9 0009.
 */
public class WelcomeActivity extends Activity {

    private Disposable disposable;

    private String[] arrays = {"eng","chi_sim", "num"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_layout);

        requestPermissions();

    }

    private void requestPermissions(){
        disposable = UtilsCore.manager().requestPermissions(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean it) throws Exception {
                        if (it){
                            dealFile();
                        }else{
                            ToastUtils.showToast(WelcomeActivity.this, "缺少必要的权限，无法注册运行！");
                            finish();
                        }
                    }
                });
    }

    /**
     * 处理文件
     */
    private void dealFile(){
        disposable = Observable.just("tessdata")
                .subscribeOn(Schedulers.io())
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String type) throws Exception {
                        setInfo(type);
                        return type;
                    }
                })
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                    }
                });
    }

    /**
     * 设置信息
     */
    private void setInfo(String typeName){
        File file = getExternalFilesDir(typeName);
        if (!file.exists()){
            file.mkdirs();
        }

        Map<Integer, String> typeMap = new HashMap<>();
        for (int i = 0; i < arrays.length; i++) {
            String name = arrays[i];
            String fileName = name + ".traineddata";
            String filePath = file.getAbsolutePath() + "/" + fileName;
            if (!new File(filePath).exists()){
                if ( copyFile(fileName, file) ){
                    typeMap.put(i, name);
                }
            }else{
                typeMap.put(i, name);
            }
        }
        ORCApi.getApi().setTypeMap(typeMap);
        ORCApi.getApi().setRootPath( file.getParentFile().getAbsolutePath() );

    }

    /**
     * 拷贝文件
     */
    private boolean copyFile(String assertFileName, File outFile){
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = getAssets().open(assertFileName);
            fos = new FileOutputStream(new File(outFile, assertFileName));
            byte[] buff = new byte[1024 * 8];
            int len = -1;
            while ((len = is.read(buff)) != -1) {
                fos.write(buff, 0, len);
            }
            return true;
        } catch (IOException e) {
            return false;
        }finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
    }
}
