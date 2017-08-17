GitHub同步 https://github.com/waterCode
动图
![image](https://github.com/waterCode/ScanImageProject/blob/master/scanimageview/src/main/assets/ezgif.com-video-to-gif.gif)


用法
uri是从上一个Activity里传来的
```java
        uri = getIntent().getParcelableExtra("uri");
        mScanImageView = (ScanPhotoView) findViewById(R.id.scan_imageView);
        mScanImageView.setImageURI(uri);
```
使用手势包括放大缩小，快滑，双击，可以查看大图