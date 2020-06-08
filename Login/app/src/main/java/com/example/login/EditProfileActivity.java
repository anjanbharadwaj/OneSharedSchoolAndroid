package com.example.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.listeners.IPickResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.chatsdk.core.session.ChatSDK;


public class EditProfileActivity extends AppCompatActivity implements IPickResult {

    static boolean pictureChanged;
    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
    final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
    Spinner spinnerSchools;
    Spinner spinnerLang;
    EditText name;
    EditText bio;
    EditText phone;
    static FloatingActionButton fab;
    User u;
    ImageButton changePfp;
    static CircularImageView profilePic;
    int initial = 0;
    static Context cont;
    ArrayList<String> spinnerArray;
    ArrayAdapter<String> spinnerArrayAdapter;
    HashMap<String, String> schoolToId = new HashMap<String, String>();

    public List<Address> latLongToText(String schoolLocation){


        double latitude = Double.parseDouble(schoolLocation.split(",")[0]);
        double longitude = Double.parseDouble(schoolLocation.split(",")[1].substring(1));


        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return addresses;
    }

    private int getIndex(Spinner spinner, String myString){
        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                return i;
            }
        }

        return 0;
    }
    public void loadInfo(){
        DatabaseReference mySchoolsRef = FirebaseDatabase.getInstance().getReference().child("Schools");
        mySchoolsRef.keepSynced(true);
        mySchoolsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot1) {
                schoolToId.clear();
                for(DataSnapshot ds : dataSnapshot1.getChildren()) {
                    School school = ds.getValue(School.class);
                    String schoolName = school.name;
                    String schoolLocation = school.location;

                    List<Address> addresses = latLongToText(schoolLocation);
                    String cityName = addresses.get(0).getLocality();
                    String countryName = addresses.get(0).getCountryName();
                    String text = schoolName + ": " + cityName + ", " + countryName;

                    schoolToId.put(text, school.id + "_" + schoolLocation);

                    spinnerArray.add(text);
                }

                Collections.sort(spinnerArray);
                spinnerArrayAdapter.notifyDataSetChanged();

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        u = dataSnapshot.getValue(User.class);
                        Log.e("UserInEditProfile", u.toString());
                        if(u.name!=null) name.setText(u.name);
                        if(u.bio!=null) bio.setText(u.bio);
                        if(u.phone!=null) phone.setText(u.phone);
                        String lang = u.language;
                        if(lang!=null) spinnerLang.setSelection(getIndex(spinnerLang, lang));

                        String schoolNum = u.school;
                        if(schoolNum!=null) {
                            School userSchool = dataSnapshot1.child(schoolNum).getValue(School.class);
                            String school = userSchool.name;
                            String schoolLocation = userSchool.location;

                            List<Address> addresses = latLongToText(schoolLocation);
                            String cityName = addresses.get(0).getLocality();
                            String countryName = addresses.get(0).getCountryName();
                            String text = school + ": " + cityName + ", " + countryName;
                            spinnerSchools.setSelection(spinnerArrayAdapter.getPosition(text));
                        }

                        MyOnItemSelectedListener moisl = new MyOnItemSelectedListener();
                        spinnerSchools.setOnItemSelectedListener(moisl);
                        spinnerLang.setOnItemSelectedListener(moisl);

                        TextWatcher tw = new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                fab.show();
                            }

                            @Override
                            public void afterTextChanged(Editable s) {

                            }
                        };
                        name.addTextChangedListener(tw);
                        bio.addTextChangedListener(tw);
                        phone.addTextChangedListener(tw);


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }

                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_personal);
        cont = this.getApplicationContext();

        spinnerSchools = findViewById(R.id.schoolSpinner);
        spinnerLang = findViewById(R.id.langSpinner);
        name = findViewById(R.id.nameEditText);
        bio = findViewById(R.id.bioEditText);
        phone = findViewById(R.id.phoneEditText);
        fab = findViewById(R.id.fab);
        fab.hide();

        changePfp = findViewById(R.id.changePfp);
        profilePic = findViewById(R.id.profilePic);
        changePfpMethod();
        changePfp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickImageDialog.build(new PickSetup()).show(getSupportFragmentManager());
            }
        });


        spinnerArray = new ArrayList<String>();
        spinnerArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, spinnerArray);
        spinnerSchools.setAdapter(spinnerArrayAdapter);

        loadInfo();



        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pictureChanged){

                    //delete the image at uid_replace
                    final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
                    final StorageReference reference = FirebaseStorage.getInstance().getReference().child("Users").child(uid);

                    Bitmap bm=((BitmapDrawable)profilePic.getDrawable()).getBitmap();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    UploadTask uploadTask = reference.putBytes(byteArray);


                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    FirebaseDatabase.getInstance().getReference().child("Users").child(uid).child("pfpUrl").setValue(uri.toString());
                                    updateChanges(uri.toString());
                                }
                            });
                        }
                    });

                } else{
                    updateChanges(null);
                }
            }
        });

    }


    public void updateChanges(String pfpUrl){
        String nameText = name.getText().toString();
        String bioText = bio.getText().toString();
        String phoneText = phone.getText().toString();
        String language = spinnerLang.getSelectedItem().toString();

        boolean nameChanged = false;
        if(!u.name.equals(nameText)) {
            u.name = nameText;
            nameChanged = true;
        }
        u.bio = bioText;
        u.phone = phoneText;
        u.language = language;

        boolean pfpChanged = false;
        if(pfpUrl!=null){
            pfpChanged = true;
            u.pfpUrl = pfpUrl;
        }
//        final String schoolName = spinnerSchools.getSelectedItem().toString().split(":")[0];
//        final String schoolLocation = spinnerSchools.getSelectedItem().toString().split(":")[1];

        u.school = schoolToId.get(spinnerSchools.getSelectedItem().toString()).split("_")[0];
        u.location = spinnerSchools.getSelectedItem().toString().split(": ")[1];
        Log.e("USER", u.toString());
        ref.setValue(u);

        changeChatSDK(nameChanged, pfpChanged);

        finish();


//        FirebaseDatabase.getInstance().getReference().child("Schools").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for(DataSnapshot ds : dataSnapshot.getChildren()) {
//                    School school = ds.getValue(School.class);
//                    if(school.name.equals(schoolName) && latLongToText(school.location).equals(schoolLocation)){
////                        ref.child("location").setValue(school.location);
//                        u.location = school.location;
////                        ref.child("school").setValue(school.id);
//                        u.school = school.id;
//                        ref.setValue(u);
//                        break;
//                    }
//                }
//
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });


    }

    public void changeChatSDK(boolean nameChanged, boolean pfpChanged){
        co.chatsdk.core.dao.User chatUser = ChatSDK.db().fetchEntityWithEntityID(FirebaseAuth.getInstance().getUid().toString(), co.chatsdk.core.dao.User.class);

        Log.e("changeChatSDK", chatUser.toString());
        DatabaseReference ref_chatprofile = FirebaseDatabase.getInstance().getReference().child("prod").child("users").child(uid).child("meta");
        if(nameChanged) {
            chatUser.setMetaString("name", u.name);
            chatUser.setMetaString("name-lowercase", u.name.toLowerCase());

            ChatSDK.core().pushUser().subscribe();
//            ref_chatprofile.child("name").setValue(u.name);
//            ref_chatprofile.child("name-lowercase").setValue(u.name.toLowerCase());
        }
        if(pfpChanged){
            StorageReference pfp_ref = FirebaseStorage.getInstance().getReference().child("Users").child(uid);
            pfp_ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Log.e("updating pfp", "for chatsdk");
                    chatUser.setMetaString("pictureURL", uri.toString());
                    ChatSDK.core().pushUser().subscribe();
//                    ref_chatprofile.child("pictureURL").setValue(uri.toString());
//                    ChatSDK.currentUser().update();

                }
            });
        }




    }

    @Override
    public void onPickResult(PickResult r) {
        if (r.getError() == null) {
            //If you want the Uri.
            //Mandatory to refresh image from Uri.
            //getImageView().setImageURI(null);

            //Setting the real returned image.
            //getImageView().setImageURI(r.getUri());

            //If you want the Bitmap.

//            getImageView().setImageBitmap(r.getBitmap());

            //Image path
            //r.getPath();

            Intent cropIntent = new Intent(getApplicationContext(), CropActivity.class);
            cropIntent.putExtra("Uri", r.getUri());
//            startActivityForResult(cropIntent, 1);
            startActivity(cropIntent);

        } else {
            //Handle possible errors
//            //TODO: do what you have to do with r.getError();
//            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void changePfpMethod(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
        StorageReference reference = FirebaseStorage.getInstance().getReference().child("Users").child(uid);
        GlideApp.with(cont).load(reference).apply(new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true).circleCrop()).into(profilePic);

    }
    public static void changePfpMethod1(Context c){
        pictureChanged = true;
        Bitmap bitmap = new ImageSaver(c).
                setFileName("profile.png").
                setDirectoryName("images").
                load();
        Log.e("ProfilePic", ""+bitmap.getByteCount());
        profilePic.setImageBitmap(bitmap);
        fab.show();
//        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
//        StorageReference reference = FirebaseStorage.getInstance().getReference().child("Users").child(uid +"_replace");
//        GlideApp.with(cont).load(reference).apply(new RequestOptions()
//                .diskCacheStrategy(DiskCacheStrategy.NONE)
//                .skipMemoryCache(true).circleCrop()).into(profilePic);

    }

    public class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent,
                                   View view, int pos, long id) {
            if(initial<2){
                initial++;
                return;
            }
            else{
                fab.show();
            }

        }

            public void onNothingSelected(AdapterView parent) {
            // Do nothing.
        }
    }
}
