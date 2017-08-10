package com.fragtest.android.pa.Test;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Try different methods to write data to disk
 */

public class TestIO {

    public TestIO() {

        String LOG = "TestIO";

        long start;
        OutputStream os;
        RandomAccessFile raf;
        int size = 250000;  // floats * 4 = 1 MB
        ByteBuffer buffer = ByteBuffer.allocate(4 * size);
        for (int i = 0; i < size; i++) {
            buffer.putFloat(1.0f);
        }


        // test multiplication vs. divison
        float divisor = 2048;
        float factor = 1/divisor;
        float result;

        start = System.currentTimeMillis();
        for (int i = 0; i < size; i++)
            result = 1 / divisor;
        Log.d(LOG, "Division: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < size; i++)
            result = 1 * factor;
        Log.d(LOG, "Multiplication: " + (System.currentTimeMillis() - start));

        File file = new File(Environment.getExternalStorageDirectory() + "/TestIO.dmp");

        Log.d(LOG, "Dump bytebuffer of " + size + " floats to disk.");

//        // FileChannel
//        start = System.currentTimeMillis();
//        try {
//            raf = new RandomAccessFile(file,"rw");
//            FileChannel fc = raf.getChannel();
//            fc.write(buffer);
//            fc.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.d(LOG, "FileChannel: " + (System.currentTimeMillis() - start));
//
//
//        // BufferedOutputStream
//        start = System.currentTimeMillis();
//        try {
//            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
//            bos.write(buffer.array());
//            bos.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.d(LOG, "BufferedOutputStream: " + (System.currentTimeMillis() - start));
//
//
//        // FileOutputStream
//        start = System.currentTimeMillis();
//        try {
//            os = new FileOutputStream(file);
//            os.write(buffer.array());
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.d(LOG, "FileOutputStream: "  + (System.currentTimeMillis() - start));
//
//
//        // RandomAccessFile (rws)
//        start = System.currentTimeMillis();
//        try {
//            raf = new RandomAccessFile(file,"rws");
//            raf.write(buffer.array());
//            raf.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.d(LOG, "RandomAccessFile (rws): "  + (System.currentTimeMillis() - start));
//
//
//        // RandomAccessFile (rw)
//        start = System.currentTimeMillis();
//        try {
//            raf = new RandomAccessFile(file,"rw");
//            raf.write(buffer.array());
//            raf.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Log.d(LOG, "RandomAccessFile (rw): "  + (System.currentTimeMillis() - start));

    }

}
