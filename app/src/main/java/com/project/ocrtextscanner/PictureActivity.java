package com.project.ocrtextscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptions;

import com.squareup.picasso.Picasso;

import java.util.List;

import static android.Manifest.permission.CAMERA;
import static com.project.ocrtextscanner.MainActivity.cropBitmap;
import static com.project.ocrtextscanner.MainActivity.resultUri;


public class PictureActivity extends AppCompatActivity {
    ImageView captureImage;
    Button copy,share;
    TextView imageText;
    String finalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        getActionBar();

        //rotation=getWindowManager().getDefaultDisplay().getRotation();

        captureImage=findViewById(R.id.img);
        imageText=findViewById(R.id.textOfImage);
        copy=findViewById(R.id.imageViewCopy);
        share=findViewById(R.id.imageViewShare);

        Picasso.get().load(resultUri).into(captureImage);
        detectText();


        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("OCRTextScanner",imageText.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(PictureActivity.this,R.string.clipboard,Toast.LENGTH_LONG).show();
            }
        });
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                String shareBody =finalText;
                String shareSub = "OCR Text Scanner";
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSub);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share using"));
            }
        });
    }
   /* private Bitmap rotateImage(Bitmap image) {
        Matrix rotateMatrix = new Matrix();
        if(rotation==0)return image;
        rotateMatrix.postRotate(rotation);
        rotatedBitmap = Bitmap.createBitmap(image, 0, 0,
               image.getWidth(), image.getHeight(),
                rotateMatrix, false);
        return rotatedBitmap;
    }*/

    private void detectText(){
        InputImage image=InputImage.fromBitmap(cropBitmap,0);
        TextRecognizer recognizer= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result=recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(@NonNull Text text) {
                StringBuilder result=new StringBuilder();
                for(Text.TextBlock block:text.getTextBlocks()){
                    String blockText = block.getText();
                    Point[] blockCornerPoint = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();
                    for(Text.Line line:block.getLines()){
                        String lineText = line.getText();
                        Point[] lineCornerPoint = line.getCornerPoints();
                        Rect lineRect = line.getBoundingBox();
                        for(Text.Element element:line.getElements()){
                            String elementText = element.getText();
                            result.append(elementText+" ");
                        }
                        result.append(System.getProperty ("line.separator"));
                    }
                }
                if(result.length()!=0){
                    imageText.setText(result);
                    finalText = String.valueOf(result);
                }
                else {
                    imageText.setText(R.string.no_text);
                    finalText= String.valueOf(R.string.no_text);
                }


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PictureActivity.this,R.string.no_text,Toast.LENGTH_LONG).show();
            }
        });
    }

}
