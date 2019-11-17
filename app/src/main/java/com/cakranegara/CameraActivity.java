package com.cakranegara;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CameraActivity extends AppCompatActivity implements Scene.OnUpdateListener {
    private TextView tvCoordinate;

    // AR variables
    private ArFragment fragment;
    private Scene arScene;

    // Anchor variables
    private AnchorNode anchorNode;

    // Pointer variables
    private PointerDrawable pointer = new PointerDrawable();
    private boolean isTracking;
    private boolean isHitting;

    // Model loader variables
    private ModelLoader modelLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // set the current fragment to the scene form
        fragment = (ArFragment)
                getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        // set the scene
        arScene = fragment.getArSceneView().getScene();

        // add a listener to the scene
        arScene.addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            onUpdate();
        });

        // onscreen coordinate update
        tvCoordinate = findViewById(R.id.tvCoordinate);
        fragment.getArSceneView().getScene().addOnUpdateListener(this);

        // initialize model loader
        modelLoader = new ModelLoader(new WeakReference<>(this));

        //initialize buttons
        Button button_add = findViewById(R.id.button_add_anchor);
        button_add.setOnClickListener(view -> addObject(Uri.parse("pin.sfb")));

        Button button_clr = findViewById(R.id.button_clear_anchor);
        button_clr.setOnClickListener(view -> addObject(null));

        fab.setOnClickListener(view -> takePhoto());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        Frame frame = fragment.getArSceneView().getArFrame();
        Pose currentPose = frame.getCamera().getPose();

        Log.d("API123", "onUpdateframe... current anchor node " + (currentPose == null));

        if (anchorNode != null) {
            Pose anchorPose = anchorNode.getAnchor().getPose();

            float dx = currentPose.tx() - anchorPose.tx();
            float dy = currentPose.ty() - anchorPose.ty();
            float dz = currentPose.tz() - anchorPose.tz();

            tvCoordinate.setText(String.format("[%.2f, %.2f, %.2f] (x, y, height)",
                    dx, dz, dy));
        }
    }

    /**
     * Method to update the tracking state.
     * If ARCore is not tracking, remove the pointer until tracking is restored.
     * If ARCore is tracking, check for the gaze of the user hitting a plane detected
     * by ARCore and enable the pointer accordingly.
     */
    private void onUpdate() {
        boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer);
            } else {
                contentView.getOverlay().remove(pointer);
            }
            contentView.invalidate();
        }

        if (isTracking) {
            boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
    }

    /**
     * Method to show the current tracking state.
     * @return True if tracking state has changed since call
     */
    private boolean updateTracking() {
        Frame frame = fragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = frame != null &&
                frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    /**
     * Method to get the hit status.
     * As soon as any hit is detected, the method returns.
     * @return
     */
    private boolean updateHitTest() {
        Frame frame = fragment.getArSceneView().getArFrame();
        android.graphics.Point pt = getScreenCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        isHitting = false;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane &&
                        ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    /**
     * Method to get the center of the screen
     * @return center point (Point)
     */
    private android.graphics.Point getScreenCenter() {
        View vw = findViewById(android.R.id.content);
        return new android.graphics.Point(vw.getWidth()/2, vw.getHeight()/2);
    }

    /**
     * Method to put a model to the screen.
     * @param model: The selected model from the gallery.
     */
    private void addObject(Uri model) {
        if (model == null) {
            addNodeToScene(null, null);
        }
        else {
            Frame frame = fragment.getArSceneView().getArFrame();
            android.graphics.Point pt = getScreenCenter();
            List<HitResult> hits;
            if (frame != null) {
                hits = frame.hitTest(pt.x, pt.y);
                for (HitResult hit : hits) {
                    Trackable trackable = hit.getTrackable();
                    if (trackable instanceof Plane &&
                            ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                        modelLoader.loadModel(hit.createAnchor(), model);
                        break;

                    }
                }
            }
        }
    }

    /**
     * Method to build two nodes and attach them to the ArSceneView's scene object.
     *
     * @param anchor
     * @param renderable
     */
    public void addNodeToScene(@Nullable Anchor anchor, @Nullable ModelRenderable renderable) {
        if (anchorNode != null) {
            // If an AnchorNode existed before, remove and nullify it.
            arScene.removeChild(anchorNode);
            anchorNode = null;
        }
        if (anchor != null) {
            anchorNode = new AnchorNode(anchor);
            TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
            node.setRenderable(renderable);
            node.setParent(anchorNode);
            fragment.getArSceneView().getScene().addChild(anchorNode);
            node.select();
        }
    }

    /**
     * When the network is down, loading a model will remotely fail.
     * @param throwable
     */
    public void onException(Throwable throwable){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(throwable.getMessage())
                .setTitle("Codelab error!");
        AlertDialog dialog = builder.create();
        dialog.show();
        return;
    }

    /**
     * Generates file name for the pictures taken.
     * @return
     */
    private String generateFilename() {
        Frame frame = fragment.getArSceneView().getArFrame();
        String date = new SimpleDateFormat(
                "yyyyMMddHHmmss",
                java.util.Locale.getDefault()).format(new Date());
        String camera_coordinate = getCoordinates(frame.getCamera().getPose());
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .getAbsolutePath()
                + File.separator
                + "Trajctory/"
                + date
                + "_"
                + camera_coordinate
                + ".jpg";
    }

    /**
     * Writes out bitmap to the file.
     * @param bitmap
     * @param filename
     * @throws IOException
     */
    private void saveBitmapToDisk(Bitmap bitmap, String filename) throws IOException {

        File out = new File(filename);
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(filename);
             ByteArrayOutputStream outputData = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData);
            outputData.writeTo(outputStream);
            outputStream.flush();
        } catch (IOException ex) {
            throw new IOException("Failed to save bitmap to disk", ex);
        }
    }

    /**
     * Uses the PixelCopy API to capture a screenshot of the ArSceneView.
     *
     * When the listener is called, the bitmap is saved to the disk, and then
     * a snackbar is shown with an intent to open the image in the Pictures application.
     */
    private void takePhoto() {
        Toast message = Toast.makeText(CameraActivity.this,
                "Taking picture ...",
                Toast.LENGTH_LONG);
        message.show();

        final String filename = generateFilename();
        ArSceneView view = fragment.getArSceneView();

        // Create a bitmap the size of the scene view.
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Create a handler thread to offload the processing of the image.
        final HandlerThread handlerThread = new HandlerThread("PixelCopier");
        handlerThread.start();
        // Make the request to copy.
        PixelCopy.request(view, bitmap, (copyResult) -> {
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename);
                } catch (IOException e) {
                    Toast toast = Toast.makeText(CameraActivity.this, e.toString(),
                            Toast.LENGTH_LONG);
                    toast.show();
                    return;
                }
                scanMedia(filename);
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Photo saved", Snackbar.LENGTH_LONG);
                snackbar.setAction("Open in Photos", v -> {
                    File photoFile = new File(filename);

                    Uri photoURI = FileProvider.getUriForFile(CameraActivity.this,
                            CameraActivity.this.getPackageName() + ".ar.codelab.name.provider",
                            photoFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW, photoURI);
                    intent.setDataAndType(photoURI, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);

                });
                snackbar.show();
            } else {
                Toast toast = Toast.makeText(CameraActivity.this,
                        "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                toast.show();
            }
            handlerThread.quitSafely();
        }, new Handler(handlerThread.getLooper()));
    }

    /**
     * Sends a broadcast to have the media scanner scan a file
     * @param path: The file to scan
     */
    private void scanMedia(String path) {
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        Intent scanFileIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(scanFileIntent);
    }

    /**
     * Get the coordinate based on the anchor.
     * @param pose: Position of the phone or the anchor.
     * @return
     */
    private String getCoordinates(Pose pose) {
        Pose anchorPose = anchorNode.getAnchor().getPose();

        float dx = pose.tx() - anchorPose.tx();
        float dy = pose.ty() - anchorPose.ty();
        float dz = pose.tz() - anchorPose.tz();

        return String.format("[(%.2f,%.2f,%.2f)]", dx, dz, dy);
    }
}
