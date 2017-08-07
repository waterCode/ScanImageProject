package com.meitu.scanimageproject;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.meitu.scanimageview.ScanPhotoView;
import com.meitu.scanimageview.tools.InputStreamBitmapDecoderFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * Created by zmc on 2017/7/18.
 */

public class DisplayActivity extends AppCompatActivity {
    private String TAG = "DisplayActivity";
    private Uri uri;//怎么部分重构？
    private ScanPhotoView mScanImageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity_layout);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            //
            //finish();
        }

        uri = getIntent().getParcelableExtra("uri");

        mScanImageView = (ScanPhotoView) findViewById(R.id.scan_imageView);
        mScanImageView.setImageURI(uri);

    }

    public void onClick(View view){
        mScanImageView.testScale();
        mScanImageView.invalidate();
    }


}
