package com.aaron.egl;


public interface Renderer {

    void onSurfaceCreated();

    void onSurfaceChanged(int width, int height);

    void onDrawFrame();

    void onSurfaceDestroyed();

}