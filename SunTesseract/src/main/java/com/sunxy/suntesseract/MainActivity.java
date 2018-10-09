package com.sunxy.suntesseract;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.sunxiaoyu.utils.UtilsCore;
import com.sunxiaoyu.utils.core.PhotoConfig;
import com.sunxiaoyu.utils.core.model.ActivityResultInfo;
import com.sunxiaoyu.utils.core.utils.DialogUtils;
import com.sunxiaoyu.utils.core.utils.StringUtils;
import com.sunxiaoyu.utils.core.utils.ToastUtils;
import com.sunxy.suntesseract.utils.ORCApi;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private String imagePath;

    private EditText textView;
    private ImageView imageView;
    private RadioGroup radioGroup;

    private long startTemp;

    private String result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

    }

    private void init(){
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        imageView.setOnClickListener(this);
        findViewById(R.id.start_btn).setOnClickListener(this);
        findViewById(R.id.take_photo).setOnClickListener(this);
        findViewById(R.id.album_photo).setOnClickListener(this);
        findViewById(R.id.copy_btn).setOnClickListener(this);
        findViewById(R.id.clear_btn).setOnClickListener(this);
        radioGroup = findViewById(R.id.radioGroup);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imageView:
                UtilsCore.manager().seePhoto(this, imagePath);
                break;
            case R.id.start_btn:
                startGet();
                break;
            case R.id.album_photo:
                UtilsCore.manager().selectPicture(this, 0, false, null)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<ActivityResultInfo>() {
                            @Override
                            public void accept(ActivityResultInfo activityResultInfo) throws Exception {
                                if (activityResultInfo.success()){
                                    textView.setText( result = "" );
                                    imagePath = activityResultInfo.getData().getStringExtra(PhotoConfig.RESULT_PHOTO_PATH);
                                    UtilsCore.manager().loadImage(MainActivity.this, imagePath, imageView);
//                                    Bitmap bitmap = ImageUtils.path2Bitmap(imagePath);
//                                    bitmap = ImageUtils.convertToBMW(bitmap, bitmap.getWidth()/2, bitmap.getHeight()/2, 100);
//                                    imageView.setImageBitmap(bitmap);
                                }
                            }
                        });
                break;
            case R.id.take_photo:
                final String savePath = getExternalFilesDir("image").getAbsolutePath() + "/"+ System.currentTimeMillis() +".jpg";
                UtilsCore.manager().takePicture(this, 0, false, savePath)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<ActivityResultInfo>() {
                            @Override
                            public void accept(ActivityResultInfo activityResultInfo) throws Exception {
                                if (activityResultInfo.success()){
                                    textView.setText( result = "" );
                                    imagePath = activityResultInfo.getData().getStringExtra(PhotoConfig.RESULT_PHOTO_PATH);
                                    UtilsCore.manager().loadImage(MainActivity.this, imagePath, imageView);
                                }
                            }
                        });
                break;
            case R.id.copy_btn:
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null){
                    ClipData mClipData = ClipData.newPlainText("Label", result);
                    cm.setPrimaryClip(mClipData);
                    ToastUtils.showToast(this, "复制成功！");
                }
                break;
            case R.id.clear_btn:
                textView.setText( result = "" );
                break;

        }
    }

    private void startGet(){
        if (StringUtils.isNull(imagePath)){
            ToastUtils.showToast(this, "请先选择照片！");
            return;
        }
        if (ORCApi.getApi().isWorking()){
            return;
        }
        DialogUtils.dismiss();
        DialogUtils.showLoadDialog(this, "识别中...", false);
        startTemp = System.currentTimeMillis();
        ORCApi.getApi().getImageContent(imagePath, getTypeFromRG(), new ORCApi.OnORCListener() {
            @Override
            public void onResultSuccess(String str) {
                result = str;
                long temp = System.currentTimeMillis() - startTemp;
                textView.setText("识别成功, 耗时 " + temp/1000 + " 秒, 识别内容：\n\n" + result);
                DialogUtils.dismiss();
            }

            @Override
            public void onResultFile(String error) {
                textView.setText("识别时发生错误：：\n\n" + error);
                DialogUtils.dismiss();
            }
        });
    }

    private int getTypeFromRG(){
        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        switch (checkedRadioButtonId){
            case R.id.radioBtn_0:
                return 0;
            case R.id.radioBtn_1:
                return 1;
            case R.id.radioBtn_2:
                return 2;
            default:
                return 1;
        }
    }
}
