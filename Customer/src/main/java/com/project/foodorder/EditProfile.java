package com.project.foodorder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.project.foodorder.mylibrary.User;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.project.foodorder.mylibrary.SharedClass.Address;
import static com.project.foodorder.mylibrary.SharedClass.CUSTOMER_PATH;
import static com.project.foodorder.mylibrary.SharedClass.CameraOpen;
import static com.project.foodorder.mylibrary.SharedClass.Mail;
import static com.project.foodorder.mylibrary.SharedClass.Name;
import static com.project.foodorder.mylibrary.SharedClass.PERMISSION_GALLERY_REQUEST;
import static com.project.foodorder.mylibrary.SharedClass.Phone;
import static com.project.foodorder.mylibrary.SharedClass.Photo;
import static com.project.foodorder.mylibrary.SharedClass.ROOT_UID;
import static com.project.foodorder.mylibrary.SharedClass.user;

public class EditProfile extends AppCompatActivity {
    private String name;
    private String surname;
    private String mail;
    private String phone;
    private String currentPhotoPath;
    private String address;

    private boolean dialog_open = false;
    private boolean photoChanged = false;

    private String error_msg;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        getData();

        Button confirm_reg = findViewById(R.id.back_order_button);
        confirm_reg.setOnClickListener(e -> {
            if(checkFields()){
                storeDatabase();
            }
            else{
                Toast.makeText(getApplicationContext(), error_msg, Toast.LENGTH_LONG).show();
            }
        });
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.plus).setOnClickListener(p -> editPhoto());
        findViewById(R.id.img_profile1).setOnClickListener(e -> editPhoto());
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void getData(){
        name = user.getName();
        surname = user.getSurname();
        address = user.getAddr();
        mail = user.getEmail();
        phone = user.getPhone();
        currentPhotoPath = user.getPhotoPath();

        ((EditText)findViewById(R.id.name1)).setText(name);
        ((EditText)findViewById(R.id.address_modify)).setText(address);
        ((EditText)findViewById(R.id.mail1)).setText(mail);
        ((EditText)findViewById(R.id.phone1)).setText(phone);
        ((EditText)findViewById(R.id.surname1)).setText(surname);

        InputStream inputStream;
        try{
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            inputStream = new URL(currentPhotoPath).openStream();
            if(inputStream != null)
                Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView)findViewById(R.id.img_profile1));
            else
                ((ImageView)findViewById(R.id.img_profile1)).setImageResource(R.drawable.person);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    private boolean checkFields(){
        name = ((EditText)findViewById(R.id.name1)).getText().toString();
        surname = ((EditText)findViewById(R.id.surname1)).getText().toString();
        mail = ((EditText)findViewById(R.id.mail1)).getText().toString();
        phone = ((EditText)findViewById(R.id.phone1)).getText().toString();
        address = ((EditText)findViewById(R.id.address_modify)).getText().toString();

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
        if(Address.trim().length() == 0){
            error_msg = "Masukkan Alamat";
            return false;
        }


        return true;
    }

    private void storeDatabase(){
        final ProgressDialog progressDialog = new ProgressDialog(this);
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference(CUSTOMER_PATH + "/" + ROOT_UID);
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Map<String, Object> profileMap = new HashMap<>();

        progressDialog.setTitle("Updating profile...");
        progressDialog.show();

        if(photoChanged && currentPhotoPath != null) {
            Uri photoUri = Uri.fromFile(new File(currentPhotoPath));
            StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());

            ref.putFile(photoUri).continueWithTask(task -> {
                if (!task.isSuccessful()){
                    throw Objects.requireNonNull(task.getException());
                }
                return ref.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    Uri downUri = task.getResult();

                    profileMap.put("customer_info", new User("pelanggan", name, surname
                            , mail, phone, address, downUri.toString()));
                    myRef.updateChildren(profileMap);

                    progressDialog.dismiss();
                    finish();
                }
            });
        }
        else{
            if(currentPhotoPath != null)
                profileMap.put("customer_info", new User("pelanggan", name, surname
                        , mail, phone, address, currentPhotoPath));
            else
                profileMap.put("customer_info", new User("pelanggan", name, surname
                        , mail, phone, address, null));

            myRef.updateChildren(profileMap);

            progressDialog.dismiss();
            finish();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void editPhoto(){
        AlertDialog alertDialog = new AlertDialog.Builder(EditProfile.this, R.style.AppTheme_AlertDialogStyle).create();
        LayoutInflater factory = LayoutInflater.from(EditProfile.this);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private File createImageFile() {
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
            case PERMISSION_GALLERY_REQUEST: {// If request is cancelled, the result arrays are empty.
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
            photoChanged = true;

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
            Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView)findViewById(R.id.img_profile1));
        }

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(Name, ((EditText)findViewById(R.id.name1)).getText().toString());
        savedInstanceState.putString(surname, ((EditText)findViewById(R.id.surname1)).getText().toString());
        savedInstanceState.putString(Address, ((EditText)findViewById(R.id.address_modify)).getText().toString());
        savedInstanceState.putString(Mail, ((EditText)findViewById(R.id.mail1)).getText().toString());
        savedInstanceState.putString(Phone, ((EditText)findViewById(R.id.phone1)).getText().toString());
        savedInstanceState.putString(Photo, currentPhotoPath);
        savedInstanceState.putBoolean(CameraOpen, dialog_open);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        ((EditText)findViewById(R.id.name1)).setText(savedInstanceState.getString(Name));
        ((EditText)findViewById(R.id.surname1)).setText(savedInstanceState.getString(surname));
        ((EditText)findViewById(R.id.address_modify)).setText(savedInstanceState.getString(Address));
        ((EditText)findViewById(R.id.mail1)).setText(savedInstanceState.getString(Mail));
        ((EditText)findViewById(R.id.phone1)).setText(savedInstanceState.getString(Phone));
        currentPhotoPath = savedInstanceState.getString(Photo);
        if(currentPhotoPath != null)
            Glide.with(getApplicationContext()).load(currentPhotoPath).into((ImageView)findViewById(R.id.img_profile1));

        if(savedInstanceState.getBoolean(CameraOpen))
            editPhoto();
    }
}