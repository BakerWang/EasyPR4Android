package yanyu.com.mrcar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.util.Log;

public class MainActivity extends Activity {
    private static final String    TAG = "MRCar";
    private CameraBridgeViewBase mOpenCvCameraView;
    private String imgPath="cartest.jpg";
    private String FILE_INSDCARD_DIR="mrcar";
    private String svmPath="svm.xml";
    private String annPath="ann.xml";
    private String ann_chinesePath="ann_chinese.xml";
    private String mappingPath="province_mapping";
    Bitmap bmp ;
    ImageView im;
    TextView textview;
    EditText et;
    boolean bgray=true;
    private void CopyOneFile(String path){
        File file=new File(String.format("/sdcard/"+FILE_INSDCARD_DIR+"/"+path));
        try {
            copyFileFromAssetsToSDCard(path,"/sdcard/"+FILE_INSDCARD_DIR+"/"+ path);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
    private void initFile(){
        File file=new File(String.format("/sdcard/"+FILE_INSDCARD_DIR + "/"));
        if(!file.exists())
            file.mkdirs();
             CopyOneFile(imgPath);
             CopyOneFile(annPath);
             CopyOneFile(svmPath);
             CopyOneFile(mappingPath);
             CopyOneFile(ann_chinesePath);
    }

    private void copyFileFromAssetsToSDCard(String resname,String sdpath) throws Throwable {
        InputStream is = getResources().getAssets().open(resname);
        OutputStream os = new FileOutputStream(sdpath);
        byte data[] = new byte[1024];
        int len;
        while ((len = is.read(data)) > 0) {
            os.write(data, 0, len);
        }
        is.close();
        os.close();
    }
    public static native String plateRecognition(long matImg,long matResult);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("mrcarproc");
                    im.setImageBitmap(bmp);
                    new plateTask().execute();
                    im.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(bgray)
                            {
                                new plateTask().execute();
                            }
                           else
                            {
                                bmp= BitmapFactory.decodeFile("/sdcard/"+FILE_INSDCARD_DIR+"/"+ imgPath);
                                im.setImageBitmap(bmp);
                                et.setText("");
                            }
                            bgray=!bgray;
                        }
                    });

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    public Handler mHandler=new Handler() {
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case 1:
                    Bundle b=msg.getData();
                    String str=b.getString("license");
                    et.setText(b.getString("license"));
                    im.setImageBitmap((Bitmap)b.getParcelable("bitmap"));
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private class plateTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            Mat m = new Mat();
            Utils.bitmapToMat(bmp, m);
            try
            {
                String license=plateRecognition(m.getNativeObjAddr(), m.getNativeObjAddr());
                Utils.matToBitmap(m, bmp);
                Message msg=new Message();
                Bundle b=new Bundle();
                b.putString("license",license);
                b.putParcelable("bitmap", bmp);
                msg.what=1;
                msg.setData(b);
                mHandler.sendMessage(msg);
            }
            catch (Exception e)
            {
                Log.d(TAG,"exception occured!");
            }
            return null;
        }
    }
        @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//取消标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        initFile();
        bmp= BitmapFactory.decodeFile("/sdcard/"+FILE_INSDCARD_DIR+"/"+ imgPath);
        im=(ImageView)findViewById(R.id.imageView);
        et=(EditText)findViewById(R.id.editText);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
