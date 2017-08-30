package yanyu.com.mrcar;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.RunnableFuture;

import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "MRCar";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int SELECT_IMAGE_ACTIVITY_REQUEST_CODE = 200;
    private static final String sdcarddir="/sdcard/"+MRCarUtil.ApplicationDir;
    private Bitmap bmp ;
    private Bitmap Originbitmap=bmp;
    private ImageView im;
    private ImageButton buttonCamera;
    private ImageButton buttonFolder;
    private EditText et;
    private boolean b2Recognition=true;
    private Uri fileUri;
    private static String filePath=null;
    private class plateTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... params) {
            Mat m = new Mat();
            Utils.bitmapToMat(bmp, m);
            try
            {
                String license=MRCarUtil.plateRecognition(m.getNativeObjAddr(), m.getNativeObjAddr());
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
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("mrcar");
                    new AsyncTask<Void,Void,Void>()
                    {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            initFile();
                            return null;
                        }
                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            if(filePath!=null){
                                bmp=BitmapFactory.decodeFile(filePath);
                            }
                                else{
                                bmp=BitmapFactory.decodeFile(sdcarddir+"/"+ MRCarUtil.initimgPath);
                            }
                            im.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    im.setImageBitmap(bmp);
                                }
                            },10);
                            Originbitmap=bmp;
                            if(bmp!=null)
                                new plateTask().execute();
                        }
                    }.execute();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//取消标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        im=(ImageView)findViewById(R.id.imageView);
        et=(EditText)findViewById(R.id.editText);
        buttonCamera=(ImageButton)findViewById(R.id.buttonCamera);
        buttonFolder=(ImageButton)findViewById(R.id.buttonFolder);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = MRMediaFileUtil.getOutputMediaFileUri(1);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });
        buttonFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                intent.setType("image/*" );
                startActivityForResult(intent, SELECT_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });
        im.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(b2Recognition)
                {
                    if(bmp!=null)
                        new plateTask().execute();
                }
                else
                {
                    bmp=Originbitmap;
                    im.setImageBitmap(bmp);
                    et.setText("");
                }
                b2Recognition=!b2Recognition;
            }
        });
    }

    public static Bitmap loadBitmap(ImageView im,String filepath){
        int width = im.getWidth();
        int height = im.getHeight();
        BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
        factoryOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, factoryOptions);
        int imageWidth = factoryOptions.outWidth;
        int imageHeight = factoryOptions.outHeight;
        int scaleFactor = Math.min(imageWidth / width, imageHeight / height);
        factoryOptions.inJustDecodeBounds = false;
        factoryOptions.inSampleSize = scaleFactor;
        factoryOptions.inPurgeable = true;
        Bitmap bmp = BitmapFactory.decodeFile(filepath, factoryOptions);
        im.setImageBitmap(bmp);
        return bmp;
    }

    public void loadAndShowBitmap(){
        bmp=loadBitmap(im,filePath);
        Originbitmap=bmp;
        et.setText("");
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE == requestCode)
        {
            if (RESULT_OK == resultCode)
            {
                if (data != null)
                {
                    if (data.hasExtra("data"))
                    {
                        Bitmap thumbnail = data.getParcelableExtra("data");
                        im.setImageBitmap(thumbnail);
                    }
                }
                else
                {
                    filePath=fileUri.getPath();
                }
            }
        }
        else
            if(requestCode ==SELECT_IMAGE_ACTIVITY_REQUEST_CODE&& resultCode == RESULT_OK && null != data)
            {
                fileUri = data.getData();
                filePath=MRMediaFileUtil.getPath(this,fileUri);
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
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
    private void initFile(){
        MRAssetUtil.CopyAssets(this,MRCarUtil.ApplicationDir,sdcarddir);
    }
    private void CopyOneFile(String filename){
        MRAssetUtil.CopyOneFile(filename,sdcarddir,getResources());
    }
}
