package com.microsoft.projectoxford.emotionsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.emotionsample.helper.ImageHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;

    // The button to select an image
    private Button mButtonSelectImage;

    // The URI of the image selected to detect.
    private Uri mImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;

    // The edit to show status and result.
    private TextView mTextView;

    private EmotionServiceClient client;

    private String textIntro = "แอปพลิเคชันวิเคราะห์อารมณ์จากรูปภาพ";
    private String textIntro2 = "กรุณาเลือกรูปภาพ เพื่อวิเคราะห์อารมณ์";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (client == null) {
            client = new EmotionServiceRestClient(getString(R.string.subscription_key));
        }

        mButtonSelectImage = (Button) findViewById(R.id.buttonSelectImage);
        mTextView = (TextView) findViewById(R.id.textViewResult);

        Toast toastIntro = Toast.makeText(MainActivity.this, textIntro, Toast.LENGTH_LONG);
        toastIntro.show();

        Toast toastIntro2 = Toast.makeText(MainActivity.this, textIntro2, Toast.LENGTH_LONG);
        toastIntro2.show();
    }

    public void doRecognize() {
        mButtonSelectImage.setEnabled(false);

        // Do emotion detection using auto-detected faces.
        try {
            new doRequest().execute();
        } catch (Exception e) {
            mTextView.append("Error encountered. Exception is: " + e.toString());
        }
    }

    // Called when the "Select Image" button is clicked.
    public void selectImage(View view) {
        mTextView.setText("");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SELECT_IMAGE);
        }
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MainActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:

                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data.getData();

                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(mImageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                        imageView.setImageBitmap(mBitmap);

                        // Add detection log.
                        Log.d("RecognizeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());

                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }


    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);
        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));

        return result;
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence
            if (e != null) {
                //mTextView.setText("เกิดข้อผิดพลาด: " + e.getMessage());
                mTextView.setText("เกิดข้อผิดพลาด: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    mTextView.append("ไม่พบใบหน้า !!! :(");
                } else {
                    int count = 0;

                    Toast toastReselt = Toast.makeText(MainActivity.this, "วิเคราะห์สำเร็จ เย้ๆ", Toast.LENGTH_LONG);
                    toastReselt.show();

                    for (RecognizeResult r : result) {
                        mTextView.append(String.format("ใบหน้าที่ %1$d -->", count+1));

                        if      (r.scores.anger     > 0.60) mTextView.append(String.format("\t โกรธ : \t%.2f %%\n", r.scores.anger*100));
                        else if (r.scores.contempt  > 0.60) mTextView.append(String.format("\t กำลังดูถูกคน : \t%.2f %%\n", r.scores.contempt*100));
                        else if (r.scores.disgust   > 0.60) mTextView.append(String.format("\t รังเกียจ : \t%.2f %%\n", r.scores.disgust*100));
                        else if (r.scores.fear      > 0.60) mTextView.append(String.format("\t กลัว : \t%.2f %%\n", r.scores.fear*100));
                        else if (r.scores.happiness > 0.60) mTextView.append(String.format("\t มีความสุข : \t%.2f %%\n", r.scores.happiness*100));
                        else if (r.scores.neutral   > 0.60) mTextView.append(String.format("\t นิ่งเฉย : \t%.2f %%\n", r.scores.neutral*100));
                        else if (r.scores.sadness   > 0.60) mTextView.append(String.format("\t โศกเศร้า : \t%.2f %%\n", r.scores.sadness*100));
                        else if (r.scores.surprise  > 0.60) mTextView.append(String.format("\t เซอร์ไพร์ : \t%.2f %%\n", r.scores.surprise*100));
                        else                                mTextView.append(String.format("\t ไม่พบอารมณ์ความรู้สึก \n"));

                        count++;
                    }
                    ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                    imageView.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
                }
            }
            mButtonSelectImage.setEnabled(true);
        }
    }
}
