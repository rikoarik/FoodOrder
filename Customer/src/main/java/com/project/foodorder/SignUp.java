package com.project.foodorder;

import com.bumptech.glide.Glide;
import com.project.foodorder.mylibrary.User;


import static com.project.foodorder.mylibrary.SharedClass.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SignUp extends AppCompatActivity {
    private boolean dialog_open = false;

    private String name;
    private String surname;
    private String mail;
    private String phone;
    private String currentPhotoPath;
    private String psw;
    private String address;
    private String error_msg;
    private FirebaseDatabase database;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        database = FirebaseDatabase.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        Button confirm_reg = findViewById(R.id.sign_up);
        confirm_reg.setOnClickListener(e -> {
            if(checkFields()){
                auth.createUserWithEmailAndPassword(mail,psw).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        ROOT_UID = auth.getUid();
                        storeDatabase();
                    }
                    else {
                        Log.d("ERROR", "createUserWithEmail:failure", task.getException());
                    }
                });
            }
            else{
                Toast.makeText(getApplicationContext(), error_msg, Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.plus).setOnClickListener(p -> editPhoto());
        findViewById(R.id.img_profile).setOnClickListener(e -> editPhoto());
    }

    private void storeDatabase() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        DatabaseReference myRef = database.getReference(CUSTOMER_PATH + "/" + ROOT_UID);

        progressDialog.setTitle("Creating profile...");
        progressDialog.show();

        if(currentPhotoPath != null) {
            Uri url = Uri.fromFile(new File(currentPhotoPath));
            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
            ref.putFile(url).continueWithTask(task -> {
                if (!task.isSuccessful()){
                    throw Objects.requireNonNull(task.getException());
                }
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    Uri downUri = task.getResult();
                    Log.d("URL", "onComplete: Url: "+ downUri.toString());

                    Map<String, Object> new_user = new HashMap<>();
                    new_user.put("customer_info",new User("pelanggan", name, surname
                            , mail, phone, address, downUri.toString()));
                    myRef.updateChildren(new_user);

                    Intent i = new Intent();
                    setResult(1, i);

                    progressDialog.dismiss();
                    finish();
                }
            }).addOnFailureListener(task -> {
                Log.d("FAILURE",task.getMessage());
            });
        }
        else{
            Map<String, Object> new_user = new HashMap<String, Object>();
            new_user.put("customer_info",new User("pelanggan", name, surname
                    ,mail,phone, address, null));
            myRef.updateChildren(new_user);

            Intent i = new Intent();
            setResult(1, i);

            progressDialog.dismiss();
            finish();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void editPhoto(){
        AlertDialog alertDialog = new AlertDialog.Builder(SignUp.this, R.style.AppTheme_AlertDialogStyle).create();
        LayoutInflater factory = LayoutInflater.from(SignUp.this);
        final View view = factory.inflate(R.layout.custom_dialog, null);

        dialog_open = true;

        alertDialog.setOnCancelListener(dialog -> {
            dialog_open = false;
            alertDialog.dismiss();
        });

        view.findViewById(R.id.camera).setOnClickListener( c -> {
            cameraIntent();
            dialog_open = false;
            alertDialog.dismiss();
        });
        view.findViewById(R.id.gallery).setOnClickListener( g -> {
            galleryIntent();
            dialog_open = false;
            alertDialog.dismiss();
        });
        view.findViewById(R.id.button_camera).setOnClickListener(v-> {
            cameraIntent();
            dialog_open = false;
            alertDialog.dismiss();
        });

        view.findViewById(R.id.button_gallery).setOnClickListener(r->{
            galleryIntent();
            dialog_open = false;
            alertDialog.dismiss();
        });
        alertDialog.setView(view);
        alertDialog.show();

    }


    private void cameraIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                photoFile = createImageFile();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.project.foodorder.fileprovider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 2);
            }
        }
    }

    private void galleryIntent(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                    PERMISSION_GALLERY_REQUEST);
        }
        else{
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, 1);
        }
    }

    private boolean checkFields(){
        name = ((EditText)findViewById(R.id.name)).getText().toString();
        surname = ((EditText)findViewById(R.id.surname)).getText().toString();
        mail = ((EditText)findViewById(R.id.mail)).getText().toString();
        phone = ((EditText)findViewById(R.id.phone)).getText().toString();
        psw = ((EditText)findViewById(R.id.psw)).getText().toString();
        address = ((EditText)findViewById(R.id.address)).getText().toString();

        if(name.trim().length() == 0){
            error_msg = "Masukkan Nama";
            return false;
        }

        if(surname.trim().length() == 0){
            error_msg = "Masukkan Nama Panggilan";
            return false;
        }

        if(mail.trim().length() == 0 || !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()){
            error_msg = "Email Tidak Valid";
            return false;
        }

        if(phone.trim().length() != 12){
            error_msg = "No. Tlp Tidak Valid";
            return false;
        }
        if(address.trim().length() == 0){
            error_msg = "Masukkan Alamat";
            return false;
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private File createImageFile() {
        // Create an image file name
        @SuppressLint("SimpleDateFormat")
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File( storageDir + File.separator +
                imageFileName + /* prefix */
                ".jpg"
        );
        currentPhotoPath = image.getAbsolutePath();

        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_GALLERY_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission Run Time: ", "Obtained");

                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, 1);
                } else {
                    Log.d("Permission Run Time: ", "Denied");

                    Toast.makeText(getApplicationContext(), "Access to media files denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if((requestCode == 1) && resultCode == RESULT_OK && null != data){
            Uri selectedImage = data.getData();

            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            currentPhotoPath = picturePath;
        }

        if((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK){
            Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView)findViewById(R.id.img_profile));
        }


    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(Name, ((EditText)findViewById(R.id.name)).getText().toString());
        savedInstanceState.putString(surname, ((EditText)findViewById(R.id.surname)).getText().toString());
        savedInstanceState.putString(Address, ((EditText)findViewById(R.id.address)).getText().toString());
        savedInstanceState.putString(Mail, ((EditText)findViewById(R.id.mail)).getText().toString());
        savedInstanceState.putString(Phone, ((EditText)findViewById(R.id.phone)).getText().toString());
        savedInstanceState.putString(Photo, currentPhotoPath);
        savedInstanceState.putBoolean(CameraOpen, dialog_open);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        ((EditText)findViewById(R.id.name)).setText(savedInstanceState.getString(Name));
        ((EditText)findViewById(R.id.surname)).setText(savedInstanceState.getString(surname));
        ((EditText)findViewById(R.id.address)).setText(savedInstanceState.getString(Address));
        ((EditText)findViewById(R.id.mail)).setText(savedInstanceState.getString(Mail));
        ((EditText)findViewById(R.id.phone)).setText(savedInstanceState.getString(Phone));
        currentPhotoPath = savedInstanceState.getString(Photo);
        if(currentPhotoPath != null){
            Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView) findViewById(R.id.img_profile));
        }

        if(savedInstanceState.getBoolean(CameraOpen))
            editPhoto();
    }
}

