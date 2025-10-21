package com.ug.air.ocular_tuberculosis.ui;

import static com.ug.air.ocular_tuberculosis.ui.slideAnalysis.CameraActivity.GALLERY;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ug.air.ocular_tuberculosis.R;
import com.ug.air.ocular_tuberculosis.adapters.SpinnerAdapter;
import com.ug.air.ocular_tuberculosis.databinding.ActivityHomeBinding;
import com.ug.air.ocular_tuberculosis.models.Current;
import com.ug.air.ocular_tuberculosis.models.Image;
import com.ug.air.ocular_tuberculosis.models.Urls;
import com.ug.air.ocular_tuberculosis.ui.slideAnalysis.kotlin.GalleryActivity;
import com.ug.air.ocular_tuberculosis.utils.Functions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    ActivityHomeBinding binding;
    RadioGroup radioGroup;
    String[] items;
    int count = 0;
    TextView images;
    SharedPreferences sharedPreferences, sharedPreferences_2, sharedPreferences_3;
    SharedPreferences.Editor editor, editor_2, editor_3;
    String filePath, date, zoom, disease, className, slide_name, selected, same_name, objective, username, grading, image_path;
    public static final String SLIDE_NAME = "slide_name";
    public static final String IMAGES = "images_paths";
    public static final String SLIDE_COUNT = "slide_count";
    public static final String CATEGORY = "category";
    String[] items_slides;
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 152;
    public static final int GALLERY_REQUEST_CODE = 105;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences(GALLERY, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        binding.can.setOnClickListener(view -> {
            startActivity(new Intent(this, ModelsActivity.class));
        });

        binding.mal.setOnClickListener(view -> {
            openSlideDialog();
        });

        binding.tub.setOnClickListener(view -> {
            openConfigDialog();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        Current current = Functions.loadFirstModel(this);
        if (current == null || !current.getDownload().isEmpty()) {
            startActivity(new Intent(this, ModelsActivity.class));
            Toast.makeText(this, "Please first download the object detection model", Toast.LENGTH_SHORT).show();
        }
    }

    private void openConfigDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.configuration);
        dialog.setCancelable(true);

        dialog.show();

        Window window = dialog.getWindow();
        if(window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        Button btn_save = dialog.findViewById(R.id.btn_save);
        Spinner spinner_slides = dialog.findViewById(R.id.spinner_slide);

        getItems();
        SpinnerAdapter adapter2 = new SpinnerAdapter(this, R.layout.custom_spinner_item, items_slides);
        setupSpinner(spinner_slides, adapter2);

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String slide = spinner_slides.getSelectedItem().toString();
                if (slide.equals("Select slide")){
                    Toast.makeText(HomeActivity.this, "Please select a slide from the list", Toast.LENGTH_SHORT).show();
                    return;
                }
                editor.putString(SLIDE_NAME, slide);
                editor.putInt(SLIDE_COUNT, 0);
                editor.apply();
                startActivity(new Intent(HomeActivity.this, GalleryActivity.class));
            }
        });
    }

    private void getItems() {
        Gson gson = new Gson();
        String jsonList = sharedPreferences.getString(IMAGES, null);

        Type type = new TypeToken<ArrayList<Image>>() {}.getType();
        ArrayList<Image> imagesArrayList = gson.fromJson(jsonList, type);
        if (imagesArrayList == null){
            imagesArrayList = new ArrayList<>();
        }

        List<String> itemsList = new ArrayList<>();
        itemsList.add("Select slide");
        for (Image image : imagesArrayList){
            itemsList.add(image.getSlideName());
        }
        items_slides = itemsList.toArray(new String[0]);
    }

    private void setupSpinner(Spinner spinner, SpinnerAdapter adapter) {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void handleChange() {
        List<String> stringList = Arrays.asList("Ocular_malaria_2025-03-18_13_01_50.jpg",
                "Ocular_malaria_2025-03-18_13_02_05.jpg", "Ocular_malaria_2025-03-18_13_02_16.jpg",
                "Ocular_malaria_2025-03-18_13_10_44.jpg", "Ocular_malaria_2025-03-18_13_22_23.jpg",
                "Ocular_malaria_2025-03-18_13_22_31.jpg", "Ocular_malaria_2025-03-18_13_22_47.jpg",
                "Ocular_malaria_2025-03-18_13_23_09.jpg", "1595931386191_jpg.rf.d3fc73aedfe8c64804cdb5ab8352c176.jpg",
                "1595931443132_jpg.rf.318e8d808c66fecd2c5a101b25815d95.jpg", "1596009751426_jpg.rf.18b651637ac6eb6a16989960cee89418.jpg",
                "1596009890993_jpg.rf.f9c04781ec3028111f909f9e90083ac4.jpg", "1596011063191_jpg.rf.e5b699f1862d1a112462c8cd6fc684fa.jpg",
                "1596011063191_jpg.rf.e5b699f1862d1a112462c8cd6fc684fa.jpg");

        Gson gson = new Gson();
        String jsonList = sharedPreferences.getString(IMAGES, null);

        Type type = new TypeToken<ArrayList<Image>>() {}.getType();
        ArrayList<Image> imagesArrayList = gson.fromJson(jsonList, type);
        if (imagesArrayList == null){
            imagesArrayList = new ArrayList<>();
        }

        for (Image image : imagesArrayList){
            if (image.getSlideName().equals("cho")){
                ArrayList<Urls> urls = image.getImages();
                for (int i = 0; i < urls.size(); i++) {
                    Urls urls1 = urls.get(i);
//                    String dir = urls1.getOriginal().substring(0, urls1.getOriginal().lastIndexOf("/"));
//                    String newFileName = stringList.get(i);
//                    String newPath = dir + "/" + newFileName;
//
//                    urls1.setOriginal(newPath);
                    urls1.setAnalysed("");
                }

                break;
            }

        }

        String json2 = gson.toJson(imagesArrayList);
        editor.putString(IMAGES, json2);
        editor.apply();
    }

    private void openSlideDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.slide);
        dialog.setCancelable(true);

        dialog.show();

        Window window = dialog.getWindow();
        if(window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        Button btn_save = dialog.findViewById(R.id.btn_save);
        radioGroup = dialog.findViewById(R.id.radioGroup);
        images = dialog.findViewById(R.id.images);
        EditText editText = dialog.findViewById(R.id.slideName);

        slide_name = sharedPreferences.getString(SLIDE_NAME, "");
        if (slide_name.isEmpty()){
            items = new String[]{"New slide"};
        }
        else {
            same_name = "Same slide (" + slide_name + ")";
            items = new String[]{same_name, "New slide"};
        }

        initValues();
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // Find the selected radio button
                RadioButton radioButton = dialog.findViewById(checkedId);
                if (radioButton != null) {
                    selected = radioButton.getText().toString();
                    if (selected.equals("New slide")){
                        editText.setVisibility(View.VISIBLE);
                        images.setVisibility(View.VISIBLE);
                        images.setText("Images captured from this slide: 0");
                    }
                    else {
                        if (selected.startsWith("Same slide")){
                            count = sharedPreferences.getInt(SLIDE_COUNT, 0);
                            images.setVisibility(View.VISIBLE);
                            images.setText("Images captured from this slide: " + count);
                        }
                        else {
                            images.setVisibility(View.VISIBLE);
                            images.setText("Images captured from this slide: 0");
                        }
                        editText.setVisibility(View.GONE);
                        editText.setText("");
                    }
                }
            }
        });

        btn_save.setOnClickListener(view -> {
            String value = editText.getText().toString().trim();

            if (selected.equals("New slide") && value.isEmpty()) {
                Toast.makeText(this, "Please provide the slide name", Toast.LENGTH_SHORT).show();
            }
            else if (!value.isEmpty()){
                slide_name = value;

                editor.putString(SLIDE_NAME, slide_name);
                editor.apply();
                dialog.dismiss();

                Intent intent = new Intent(HomeActivity.this, GalleryActivity.class);
                intent.putExtra("Screen", "Home");
                startActivity(intent);
            }
            else {
                dialog.dismiss();

                Intent intent = new Intent(HomeActivity.this, GalleryActivity.class);
                intent.putExtra("Screen", "Home");
                startActivity(intent);
            }

        });

    }

    private void load_camera() {
//        startActivity(new Intent(this, CameraActivity.class));

    }

    private void initValues(){
        for (String i: items) {
            RadioButton radioButton = new RadioButton(getApplicationContext());
            radioButton.setText(i); // Set label text
            radioGroup.addView(radioButton); // Add RadioButton to RadioGroup
        }

    }
}