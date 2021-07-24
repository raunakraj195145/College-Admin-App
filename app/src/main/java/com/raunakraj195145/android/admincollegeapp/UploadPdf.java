package com.raunakraj195145.android.admincollegeapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;

public class UploadPdf extends AppCompatActivity {
    CardView addPdf;
    private final int REQ=1;
    private Uri pdfData;
    private EditText pdfTitle;
    private Button uploadPdfButton;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    String downloadUrl="";
    private ProgressDialog pd;
    private TextView pdfTextView;
    private String pdfName,title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload_pdf);


        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference= FirebaseStorage.getInstance().getReference();

        pd=new ProgressDialog(this);

        addPdf=findViewById(R.id.addPdf);
        pdfTitle=findViewById(R.id.pdfTitle);
        uploadPdfButton=findViewById(R.id.uploadPdfButton);
        pdfTextView=findViewById(R.id.pdfTextView);

        addPdf.setOnClickListener(view -> openGallery());
        uploadPdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                title=pdfTitle.getText().toString();
                if(title.isEmpty()){
                    pdfTitle.setError("Empty");
                    pdfTitle.requestFocus();
                }else if(pdfData==null){
                    Toast.makeText(UploadPdf.this,"Please upload pdf",Toast.LENGTH_SHORT).show();
                }else {
                    uploadPdf();
                }
            }
        });

    }

    private void uploadPdf() {
        pd.setTitle("Please wait...");
        pd.setMessage("Uploading pdf...");
        pd.show();
        StorageReference reference= this.storageReference.child("pdf/"+pdfName+"-"+System.currentTimeMillis()+".pdf");
        reference.putFile(pdfData).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask= taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri uri=uriTask.getResult();
                uploadData(String.valueOf(uri));
               // downloadUrl=String.valueOf(uri);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
                pd.dismiss();
                Toast.makeText(UploadPdf.this,"Something went wrong",Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void uploadData(String valueOf) {
        downloadUrl=valueOf;

       final String uniqueKey= databaseReference.child("pdf").push().getKey();

        HashMap data=new HashMap();
        data.put("pdfTitle",title);
        data.put("pdfUrl",downloadUrl);

        databaseReference.child("pdf").child(uniqueKey).setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<Void> task) {
                pd.dismiss();
                Toast.makeText(UploadPdf.this,"Pdf uploaded successfully",Toast.LENGTH_SHORT).show();
                pdfTitle.setText("");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull @NotNull Exception e) {
                pd.dismiss();
               Toast.makeText(UploadPdf.this,"Failed to upload pdf",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openGallery() {
       Intent intent=new Intent();
       intent.setType("pdf/docs/ppt");
       // intent.setType("*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Pdf File"),REQ);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQ&&resultCode==RESULT_OK) {
            pdfData = data.getData();
            if (pdfData.toString().startsWith("content://")) {
                Cursor cursor=null;
                try {
                    cursor= UploadPdf.this.getContentResolver().query(pdfData,null,null,null,null);
                    if(cursor!=null&&cursor.moveToFirst()){
                        pdfName=cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (pdfData.toString().startsWith("file://")) {
                pdfName=new File(pdfData.toString()).getName();
            }
            pdfTextView.setText(pdfName);
        }
    }
}