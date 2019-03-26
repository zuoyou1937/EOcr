package test.com.ocrtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class cameraActivity extends AppCompatActivity {

    CameraView mCameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCameraView = (CameraView) findViewById(R.id.main_camera);
    }
}
