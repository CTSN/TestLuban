package com.roy.testluban;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;
import uk.co.senab.photoview.PhotoView;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int TAKE_PHOTO = 100;
    private static final int PHOTO = 200;
    private static final int CAMERA = 1;

    @BindView(R.id.btn_take_photo)
    Button btnTakePhoto;
    @BindView(R.id.btn_open)
    Button btnOpen;
    @BindView(R.id.tv_yuan)
    TextView tvYuan;
    @BindView(R.id.tv_change)
    TextView tvChange;
    @BindView(R.id.iv_show_real)
    PhotoView ivShow;
    @BindView(R.id.iv_show_compress)
    PhotoView ivShowCompress;

    private File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initPersission();
    }

    @OnClick({R.id.btn_take_photo, R.id.btn_open})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_take_photo:
                photoFile = new File(Environment.getExternalStorageDirectory() + File.separator + "TestLubanYuan.jpg");

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri photoUri = Uri.fromFile(photoFile);
                photoUri.getPath();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, TAKE_PHOTO);

                break;
            case R.id.btn_open:
                Intent intent1 = new Intent(Intent.ACTION_PICK, null);
                intent1.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent1, PHOTO);
                break;
        }
    }

    private void initPersission() {
        String perm[] = {Manifest.permission.CAMERA};
        if (EasyPermissions.hasPermissions(this, perm)) {

        } else {
            EasyPermissions.requestPermissions(this, "需要摄像头权限权限",
                    CAMERA, perm);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case TAKE_PHOTO:
                    Glide.with(MainActivity.this).load(photoFile).centerCrop().crossFade().into(ivShow);
                    tvYuan.setText("原图：图片大小" + photoFile.length() / 1024 + "k" + "图片尺寸："
                            + Luban.get(getApplicationContext()).getImageSize(photoFile.getPath())[0]
                            + " * " + Luban.get(getApplicationContext()).getImageSize(photoFile.getPath())[1]);
                    compressWithLs(photoFile);
                    break;
                case PHOTO:
                    if (data != null) {// 为了取消选取不报空指针用的
                        Uri uri = data.getData();
                        String urlPath = uri.getPath();

                        //去除小米5图片返回路径携带/raw/
                        if (urlPath.contains("/raw/")) {
                            urlPath = urlPath.replace("/raw/", "");
                        }
                        File file = new File(urlPath);

//                        Log.i("TAG", "--->" + urlPath);
                        Glide.with(MainActivity.this).load(urlPath).centerCrop().crossFade().into(ivShow);
                        tvYuan.setText("原图：图片大小" + file.length() / 1024 + "k" + "图片尺寸："
                                + Luban.get(getApplicationContext()).getImageSize(file.getPath())[0]
                                + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);

                        compressWithLs(file);
                    }
                    break;
            }
        }
    }

    /**
     * 压缩图片
     */
    private void compressWithLs(File file) {
        Luban.get(this)
                .load(file)
                .putGear(Luban.THIRD_GEAR)
                .setFilename(System.currentTimeMillis() + "")
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(File file) {
                        Glide.with(MainActivity.this).load(file).centerCrop().crossFade().into(ivShowCompress);

                        tvChange.setText("压缩后：图片大小" + file.length() / 1024 + "k" + "图片尺寸："
                                + Luban.get(getApplicationContext()).getImageSize(file.getPath())[0]
                                + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();
    }


    private void compressWithRx(File file) {
        Luban.get(this)
                .load(file) //加载图片
                .putGear(Luban.THIRD_GEAR)  //设置压缩等级
                .asObservable()     //返回一个Obsetvable观察者对象
                .subscribeOn(Schedulers.io())   //压缩指定IO线程
                .observeOn(AndroidSchedulers.mainThread())  //回调返回主线程
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {     //运行异常回调
                        throwable.printStackTrace();
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends File>>() {
                    @Override
                    public Observable<? extends File> call(Throwable throwable) {   //异常处理
                        return Observable.empty();
                    }
                })
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        Glide.with(MainActivity.this).load(file).centerCrop().crossFade().into(ivShowCompress);

                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri uri = Uri.fromFile(file);
                        intent.setData(uri);
                        MainActivity.this.sendBroadcast(intent);

                        tvChange.setText("压缩后：图片大小" + file.length() / 1024 + "k" + "图片尺寸："
                                + Luban.get(getApplicationContext()).getImageSize(file.getPath())[0]
                                + " * " + Luban.get(getApplicationContext()).getImageSize(file.getPath())[1]);
                    }
                });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    //成功
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
//            canGO();
        }

    }

    //失败
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)) {
            new AppSettingsDialog.Builder(this, "需要开启摄像头权限，请到应用权限管理中打开权限")
                    .setTitle("权限需求").build().show();
        }
    }

}
