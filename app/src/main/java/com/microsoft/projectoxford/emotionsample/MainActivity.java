package com.microsoft.projectoxford.emotionsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

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

    private static final int REQUEST_SELECT_IMAGE_IN_ALBUM = 1;

    // The button to select an image
    private Button mButtonSelectImage;

    // The URI of the image selected to detect.
    private Uri mImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;

    // The edit to show status and result.
    private EditText mEditText;

    private EmotionServiceClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (client == null) {
            client = new EmotionServiceRestClient(getString(R.string.subscription_key));
        }

        mButtonSelectImage = (Button) findViewById(R.id.buttonSelectImage);
        mEditText = (EditText) findViewById(R.id.editTextResult);
    }

    public void doRecognize() {
        mButtonSelectImage.setEnabled(false);

        // Do emotion detection using auto-detected faces.
        try {
            new doRequest(false).execute();
        } catch (Exception e) {
            mEditText.append("Error encountered. Exception is: " + e.toString());
        }
    }

    // Called when the "Select Image" button is clicked.
    public void selectImage(View view) {
        mEditText.setText("");
/*
        Intent intent;
        intent = new Intent(MainActivity.this, com.microsoft.projectoxford.emotionsample.helper.SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);*/
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

                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
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
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = this.client.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private class doRequest extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public doRequest(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            if (this.useFaceRectangles == false) {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            } else {
                try {
                    return processWithAutoFaceDetection();
                } catch (Exception e) {
                    this.e = e;    // Store error
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence

            if (e != null) {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
                if (result.size() == 0) {
                    mEditText.append("No emotion detected :(");
                } else {
                    Integer count = 0;
                    // Covert bitmap to a mutable bitmap by copying it
                    Bitmap bitmapCopy = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas faceCanvas = new Canvas(bitmapCopy);
                    faceCanvas.drawBitmap(mBitmap, 0, 0, null);
                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.RED);

                    for (RecognizeResult r : result) {
                        mEditText.append(String.format("Face #%1$d \n", count+1));
                        if(r.scores.anger > 0.60)          mEditText.append(String.format("\t Emotion anger : %.2f %%\n", r.scores.anger*100));
                        else if (r.scores.contempt > 0.60) mEditText.append(String.format("\t Emotion contempt : %.2f %%\n", r.scores.contempt*100));
                        else if (r.scores.disgust > 0.60)  mEditText.append(String.format("\t Emotion disgust : %.2f %%\n", r.scores.disgust*100));
                        else if (r.scores.fear > 0.60)     mEditText.append(String.format("\t Emotion fear : %.2f %%\n", r.scores.fear*100));
                        else if (r.scores.happiness > 0.60)mEditText.append(String.format("\t Emotion happiness : %.2f %%\n", r.scores.happiness*100));
                        else if (r.scores.neutral > 0.60)  mEditText.append(String.format("\t Emotion neutral : %.2f %%\n", r.scores.neutral*100));
                        else if (r.scores.sadness > 0.60)  mEditText.append(String.format("\t Emotion sadness : %.2f %%\n", r.scores.sadness*100));
                        else if (r.scores.surprise > 0.60) mEditText.append(String.format("\t Emotion surprise : %.2f %%\n", r.scores.surprise*100));
                        else mEditText.append(String.format("\t Not found Face Emotion \n"));

                        faceCanvas.drawRect(r.faceRectangle.left,
                                r.faceRectangle.top,
                                r.faceRectangle.left + r.faceRectangle.width,
                                r.faceRectangle.top + r.faceRectangle.height,
                                paint);
                        count++;
                    }
                    ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                    imageView.setImageDrawable(new BitmapDrawable(getResources(), mBitmap));
                }
                mEditText.setSelection(0);
            }

            mButtonSelectImage.setEnabled(true);
        }
    }
}
