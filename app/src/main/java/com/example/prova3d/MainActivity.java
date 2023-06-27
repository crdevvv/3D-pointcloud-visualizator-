package com.example.prova3d;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import org.ddogleg.struct.DogArray;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import boofcv.alg.cloud.PointCloudWriter;
import boofcv.io.points.PointCloudIO;
import boofcv.struct.Point3dRgbI_F32;

public class MainActivity extends AppCompatActivity {

    private OpenGLES20SurfaceView glSurfaceView;
    private static final int READ_REQUEST_CODE = 0;
    protected Uri mUri;
    public float[] vertices;
    public float[] colors;
    public float[] mean = new float[3];
    public float threshold = 0.98f;
    public float squareDeviation=0f;

    public List<KdTree.Node> nodes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new OpenGLES20SurfaceView(this);
        setContentView(glSurfaceView);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }


    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();

        if (mUri == null) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
    }


    @Override
    protected void onPause(){
        super.onPause();
        glSurfaceView.onPause();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                mUri = data.getData();
                System.out.println(getExternalFilesDir(null).getAbsolutePath() + "/" + mUri.getPath().split(":")[1]);
                DogArray<Point3dRgbI_F32> storage = new DogArray<>(Point3dRgbI_F32::new);
                try {
                    for (int i=0; i<2; i++) {
                        Toast.makeText(getApplicationContext(), "Loading Point cloud...", Toast.LENGTH_LONG).show();
                    }
                    storage = loadPc(storage);

                    float std  = calculateStandardDeviation(storage);
                    System.out.println("dev standard: "  + std);
                    calculateMean(storage);
                    System.out.println(mean[0] + "  medie  " + mean[1] + "  medie  " + mean[2]);
                    for (int i=0; i<1 ; i++) {
                        Toast.makeText(getApplicationContext(), "Reading coordinates...", Toast.LENGTH_LONG).show();
                    }
                    readPointCloudCord(storage);
                    System.out.println("vertex number:"  + storage.size());

                    glSurfaceView.setRendererVertices(vertices);
                    glSurfaceView.setRendererColors(colors);
                    kdTree(storage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    public void kdTree(DogArray<Point3dRgbI_F32> storage){
        KdTree.Node node;
        long a = System.currentTimeMillis();
        for (int i=0; i< storage.size ; i++){
            node = new KdTree.Node(storage.get(i).x,storage.get(i).y,storage.get(i).z);
            nodes.add(i,node);
            System.out.println(i);
        }
        KdTree kdTree = new KdTree(3,nodes);
        KdTree.Node target = new KdTree.Node(3f,2f,5f);
        KdTree.Node nearest = kdTree.findNearest(target);
        KdTree.Node root =kdTree.getRoot();
        long b = System.currentTimeMillis();
        System.out.println("t(ms) kdtree: " + (b - a));
        System.out.println("root node: " + root.get(0) + "  " + root.get(1)+ "  " + root.get(2));
        System.out.println("Random data (" + (vertices.length/3) + " points):");
        System.out.println("target: " + target);
        System.out.println("nearest point: " + nearest);
        System.out.println("distance: " + kdTree.distance());
        System.out.println("nodes visited: " + kdTree.visited());

    }
    public void calculateMean(DogArray<Point3dRgbI_F32> storage){
        float sumX=0;
        float sumY=0;
        float sumZ=0;
        for (int i=0; i<storage.size(); i++){
            sumX += storage.get(i).x;
            sumY += storage.get(i).y;
            sumZ += storage.get(i).z;
        }

        float meanX = sumX/storage.size();
        float meanY = sumY/storage.size();
        float meanZ = sumZ/storage.size();
        mean[0] = meanX;
        mean[1] = meanY;
        mean[2] = meanZ;

    }

    public float calculateStandardDeviation(DogArray<Point3dRgbI_F32> storage){

        for (int i=0; i<storage.size(); i++){
            squareDeviation += computeDistance(mean[0],storage.get(i).x,mean[1],storage.get(i).y,mean[2],storage.get(i).z);
        }
        squareDeviation = (float) Math.sqrt(squareDeviation/storage.size());
        return squareDeviation;

    }
    public float computeDistance(float x1,float x2,float y1,float y2,float z1,float z2){
        return (float) ((float) Math.pow((x1 -x2),2) + Math.pow((y1-y2),2) + Math.pow((z1-z2),2));
    }

    public DogArray<Point3dRgbI_F32> loadPc(DogArray<Point3dRgbI_F32> storage) throws IOException{
        long a = System.currentTimeMillis();
        PointCloudWriter output = PointCloudWriter.wrapF32RGB(storage);
        InputStream inputStream = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            inputStream = Files.newInputStream(Paths.get(getExternalFilesDir(null).getAbsolutePath() + "/" + mUri.getPath().split(":")[1]));
        }
        try {
            assert inputStream != null;
            PointCloudIO.load(PointCloudIO.Format.PLY, inputStream, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        long b = System.currentTimeMillis();
        System.out.println("t(ms) load cord: " + (b - a));
        return storage;
    }

    public void readPointCloudCord(DogArray<Point3dRgbI_F32> storage) throws IOException {
        long a = System.currentTimeMillis();
        vertices = new float[storage.size * 3];
        colors = new float[storage.size*3];
        int j =0;
        for (int i = 0; i < storage.size; i++) {
            float distance = computeDistance(mean[0],storage.get(i).x,mean[1],storage.get(i).y,mean[2],storage.get(i).z);
            distance = (float) Math.sqrt(distance);
            int temp = storage.data[i].rgb;
            if (distance <= squareDeviation * threshold) {
                vertices[j * 3] = storage.get(i).x;
                vertices[j * 3 + 1] = storage.get(i).y;
                vertices[j * 3 + 2] = storage.get(i).z;

                colors[j*3] = (((temp>>16)&0xFF)/255.0f);
                colors[j*3+1] = ((temp>>8)&0xFF)/255.0f;
                colors[j*3+2] = (temp&0xFF)/255.0f;
                j = j+1;
            }

        }
        long b = System.currentTimeMillis();
        System.out.println("t(ms) read cord: " + (b - a));
    }

}