package com.example.lab7mlapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.lab7mlapp.ml.EfficientdetLite2m;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.model.Model;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    ImageView ivResult;
    TextView tvOutPutCat;
    Button btnLoadImage, btnPredict;
    int imageSize = 224;
    double threshold = 0.3;
    Bitmap imgOrg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.ivResult);
        ivResult = (ImageView) findViewById(R.id.image_view);
        tvOutPutCat = (TextView) findViewById(R.id.tvOutPutCat);
        btnLoadImage = (Button) findViewById(R.id.load_img_button);
        btnPredict = (Button) findViewById(R.id.predict_button);

        btnLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 3);
            }
        });
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imgOrg != null)
                    detectObject(imgOrg);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == 3) {
                Uri selectedImg = data.getData();
                try {
                    this.imgOrg = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImg);
                    imageView.setImageBitmap(imgOrg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                this.imgOrg = null;
                tvOutPutCat.setText("No Image");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void detectObject(Bitmap bitmap) {
        try {
            EfficientdetLite2m model = EfficientdetLite2m.newInstance(getApplicationContext());
            Model.Options.Builder builder = new Model.Options.Builder();
            builder.setNumThreads(2);
            TensorImage image = TensorImage.fromBitmap(bitmap); //create an image for object detection, based on an input bitmap image
            EfficientdetLite2m.Outputs outputs = model.process(image); //detect object
            List<EfficientdetLite2m.DetectionResult> results = outputs.getDetectionResultList(); //get a list detected object
            model.close();
            if (results.size() > 0) {
                Bitmap bmOverlay = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig()); //create a new bitmap image with same size
                Canvas canvas = new Canvas(bmOverlay); //create a new canvas(a panel which is drawable)
                Paint paint = new Paint(); //create a paint object for primitive drawings: lines, rectangles,...)
                paint.setColor(Color.RED);
                paint.setStrokeWidth(8);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawBitmap(bitmap, new Matrix(), null); //copy the old image to the new image(to draw rectangles in the new image)
                String cat = "";
                for (EfficientdetLite2m.DetectionResult r : results) { //loop the detected object list
                    RectF location = r.getLocationAsRectF(); //get the position of the object(rectangles)
                    String category = r.getCategoryAsString(); //get the object name(category)
                    float score = r.getScoreAsFloat(); //the larger the score, the more certain that this in asn object
                    if (score > threshold) {
                        if (cat != "") cat += "\n";
                        cat += category;
                        canvas.drawRect(location, paint); //draw a rectangles as the object boundary
                    }
                }
                tvOutPutCat.setText(cat);
                ivResult.setImageBitmap(bmOverlay); //display the new image in the result view
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
