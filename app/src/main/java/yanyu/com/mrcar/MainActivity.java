package yanyu.com.mrcar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
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

import android.util.Log;

public class MainActivity extends Activity {
    private static final String TAG = "MRCar";
//    private CameraBridgeViewBase mOpenCvCameraView;
    private String initimgPath="cartest.jpg";
    private String FILE_INSDCARD_DIR="mrcar";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int SELECT_IMAGE_ACTIVITY_REQUEST_CODE = 200;
    private String svmPath="svm.xml";
    private String annPath="ann.xml";
    private String ann_chinesePath="ann_chinese.xml";
    private String mappingPath="province_mapping";
    private Bitmap bmp ;
    private Bitmap Originbitmap=bmp;
    private ImageView im;
    private ImageButton buttonCamera;
    private ImageButton buttonFolder;
    private TextView textview;
    private EditText et;
    private boolean b2Recognition=true;
    private Uri fileUri;
    public static final int MEDIA_TYPE_IMAGE = 1;

    public static native String plateRecognition(long matImg,long matResult);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("mrcarproc");
                    new plateTask().execute();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        initFile();
        bmp= BitmapFactory.decodeFile("/sdcard/"+FILE_INSDCARD_DIR+"/"+ initimgPath);
        Originbitmap=bmp;
        im=(ImageView)findViewById(R.id.imageView);
        im.setImageBitmap(bmp);
        et=(EditText)findViewById(R.id.editText);
        buttonCamera=(ImageButton)findViewById(R.id.buttonCamera);
        buttonFolder=(ImageButton)findViewById(R.id.buttonFolder);
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });
        buttonFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*" );
                startActivityForResult(intent, SELECT_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });
        im.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(b2Recognition)
                {
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
                    int width = im.getWidth();
                    int height = im.getHeight();
                    BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
                    factoryOptions.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(fileUri.getPath(), factoryOptions);
                    int imageWidth = factoryOptions.outWidth;
                    int imageHeight = factoryOptions.outHeight;
                    int scaleFactor = Math.min(imageWidth / width, imageHeight / height);
                    factoryOptions.inJustDecodeBounds = false;
                    factoryOptions.inSampleSize = scaleFactor;
                    factoryOptions.inPurgeable = true;
                    bmp = BitmapFactory.decodeFile(fileUri.getPath(), factoryOptions);
                    Originbitmap=bmp;
                    im.setImageBitmap(bmp);
                }
            }
        }
        else
            if(requestCode ==SELECT_IMAGE_ACTIVITY_REQUEST_CODE&& resultCode == RESULT_OK && null != data)
            {
                fileUri = data.getData();
                String filePath=null;
                if(DocumentsContract.isDocumentUri(getApplicationContext(), fileUri)){
                    String wholeID = DocumentsContract.getDocumentId(fileUri);
                    String id = wholeID.split(":")[1];
                    filePath="/sdcard/"+wholeID.split(":")[1];
            }else{
                    String[] projection = { MediaStore.Images.Media.DATA };
                    Cursor cursor = getApplicationContext().getContentResolver().query(fileUri, projection, null, null, null);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    filePath = cursor.getString(column_index);
                }
                int width = im.getWidth();
                int height = im.getHeight();
                BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
                factoryOptions.inJustDecodeBounds = true;
                int imageWidth = factoryOptions.outWidth;
                int imageHeight = factoryOptions.outHeight;
                int scaleFactor = Math.min(imageWidth / width, imageHeight / height);
                factoryOptions.inJustDecodeBounds = false;
                factoryOptions.inSampleSize = scaleFactor;
                factoryOptions.inPurgeable = true;
                bmp = BitmapFactory.decodeFile(filePath, factoryOptions);
                Originbitmap=bmp;
                im.setImageBitmap(bmp);
            }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
        CopyOneFile(initimgPath);
        CopyOneFile(annPath);
        CopyOneFile(svmPath);
        CopyOneFile(mappingPath);
        CopyOneFile(ann_chinesePath);
    }
    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type)
    {
        return Uri.fromFile(getOutputMediaFile(type));
    }
    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type)
    {
        File mediaStorageDir = null;
        try
        {
            mediaStorageDir = new File("/sdcard/mrcar/");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.d(TAG, "Error in Creating mediaStorageDir: "
                    + mediaStorageDir);
        }

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists())
        {
            if (!mediaStorageDir.mkdirs())
            {
                // 在SD卡上创建文件夹需要权限：
                // <uses-permission
                // android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
                Log.d(TAG,
                        "failed to create directory, check if you have the WRITE_EXTERNAL_STORAGE permission");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        }
        else
        {
            return null;
        }
        return mediaFile;
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

}
