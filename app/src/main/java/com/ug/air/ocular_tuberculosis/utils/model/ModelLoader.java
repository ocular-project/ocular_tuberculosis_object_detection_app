package com.ug.air.ocular_tuberculosis.utils.model;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ModelLoader {

    private static final String TAG = "ModelLoader";
    public static Interpreter loadModel(Context context, String modelFileName) {
        File baseDir = context.getFilesDir();
        File modelFile = new File(baseDir, modelFileName);

        if (!modelFile.exists()) {
            Log.e(TAG, "Model file does not exist: " + modelFile.getAbsolutePath());
            return null;
        }

        try {
            // Open the model file
            FileInputStream fis = new FileInputStream(modelFile);
            FileChannel fileChannel = fis.getChannel();

            // Map the model file into a ByteBuffer
            ByteBuffer modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

            // Close the file input stream
            fis.close();

            // Initialize the TensorFlow Lite Interpreter
            return new Interpreter(modelBuffer);

        } catch (IOException e) {
            Log.e(TAG, "Error loading model file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
