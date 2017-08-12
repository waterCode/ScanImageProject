动图


用法
uri是从上一个Activity里传来的
```java
        uri = getIntent().getParcelableExtra("uri");
        mScanImageView = (ScanPhotoView) findViewById(R.id.scan_imageView);
        mScanImageView.setImageURI(uri);
```