package com.xxxzxxx.caffe.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import com.universal.robot.core.helper.Logger;
import com.xxxzxxx.caffe.android.lib.CaffeMobile;
import com.xxxzxxx.caffe.android.lib.CaffeMobile.CreateInstanceException;
import com.xxxzxxx.caffe.android.lib.CaffeMobile.Result;


public class MainActivity extends Activity implements CaffeMobile.CNNCompletedListener {
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static String[] IMAGENET_CLASSES;

    private Button btnCamera;
    private Button btnSelect;
    private ImageView ivCaptured;
    private TextView tvLabel;
    private Uri fileUri;
    private ProgressDialog dialog;
    private Bitmap bmp;
    private CaffeMobile caffeMobile;
    File sdcard = Environment.getExternalStorageDirectory();
    String modelDir = sdcard.getAbsolutePath() + "/caffe_mobile/bvlc_reference_caffenet";
    String modelProto = modelDir + "/deploy.prototxt";
    String modelBinary = modelDir + "/bvlc_reference_caffenet.caffemodel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivCaptured = (ImageView) findViewById(R.id.ivCaptured);
        tvLabel = (TextView) findViewById(R.id.tvLabel);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
                startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
            }
        });

        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        try
        {
	        if (caffeMobile == null)
	        {
				caffeMobile = new CaffeMobile();
				caffeMobile.setupAsynctask(this);
	        }
	        AssetManager am = this.getAssets();
            InputStream is = am.open("synset_words.txt");
            Scanner sc = new Scanner(is);
            List<String> lines = new ArrayList<String>();
            while (sc.hasNextLine()) {
                final String temp = sc.nextLine();
                lines.add(temp.substring(temp.indexOf(" ") + 1));
            }
            IMAGENET_CLASSES = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        } catch (CreateInstanceException e) {
			e.printStackTrace();
            finish();
		}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = fileUri.getPath();
            } else {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
            }
            Log.i(this.getLocalClassName(), imgPath);
            bmp = BitmapFactory.decodeFile(imgPath);
            dialog = ProgressDialog.show(MainActivity.this, "Predicting...", "Wait for one sec...", true);
            caffeMobile.executePredictImageWithExtractFeaturesAsyncTask(new File(imgPath),caffeMobile.kBlobNames,this);
        } else {
            btnCamera.setEnabled(true);
            btnSelect.setEnabled(true);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initPrediction() {
        btnCamera.setEnabled(false);
        btnSelect.setEnabled(false);
        tvLabel.setText("");
    }

    @Override
    public void onTaskCompleted(Result[] results) {
    	final long started = Logger.start();
    	if (results.length < 1)
    	{
        	Logger.debug("results.length < 1");
    	}
    	else
    	{
        	Logger.debug("results.length >= 1");
	        ivCaptured.setImageBitmap(bmp);
	        for (Result res : results)
	        {
	        	Logger.debug("Result");
	        	int[] executeResults = res.getExecuteResults();
	        	if (null != executeResults)
	        	{
		        	Logger.debug("executeResults");
		        	for (int i = 0; i < executeResults.length; i++){
			        	Logger.debug("executeResults[%d]=%d",i,executeResults[i]);
		        	}
		        	Logger.debug("executeResults");
	        	}
	        	else
	        	{
		        	Logger.debug("executeResults is null");
	        	}
	        	float[][] extractFeatures = res.getExtractFeatures();
	        	if (null != extractFeatures)
	        	{
		        	Logger.debug("extractFeatures");
		        	for (int i = 0; i <extractFeatures.length; i++){
		        		float[] extractFeature = extractFeatures[i];
			        	for (int ii = 0; ii <extractFeature.length; ii++){
			        		Logger.debug("extractFeatures[%d][%d]=%f",i,ii,extractFeature[ii]);
			        	}
		        	}
		        	Logger.debug("extractFeatures");
	        	}
	        	else
	        	{
		        	Logger.debug("extractFeatures is null");
	        	}

	        }
//	        tvLabel.setText(IMAGENET_CLASSES[result[0].getExecuteResult()]);
	        btnCamera.setEnabled(true);
	        btnSelect.setEnabled(true);
    	}
        if (dialog != null) {
            dialog.dismiss();
        }
        Logger.end(started);
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    @SuppressLint("NewApi")
	private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Caffe-Android-Demo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
