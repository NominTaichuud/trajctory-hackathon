package com.cakranegara;

import android.net.Uri;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.lang.ref.WeakReference;

/**
 * A class to start the asynchronous loading of the 3D model using the ModelRenderable builder.
 * The activity class can be replaced or destroyed at any point, even while a model is loading.
 * A weak reference is used to ensure the ModelLoader respects the Activity lifecycle.
 */
public class ModelLoader {
    private final WeakReference<CameraActivity> owner;
    private static final String TAG = "ModelLoader";

    ModelLoader(WeakReference<CameraActivity> owner) {
        this.owner = owner;
    }

    void loadModel(Anchor anchor, Uri uri) {
        if (owner.get() == null) {
            Log.d(TAG, "Activity is null.  Cannot load model.");
            return;
        }
        ModelRenderable.builder()
                .setSource(owner.get(), uri)
                .build()
                .handle((renderable, throwable) -> {
                    CameraActivity activity = owner.get();
                    if (activity == null) {
                        return null;
                    } else if (throwable != null) {
                        activity.onException(throwable);
                    } else {
                        activity.addNodeToScene(anchor, renderable);
                    }
                    return null;
                });

        return;
    }
}
