package com.aaron.sample.light;

import android.opengl.GLES30;
import android.util.Log;
import com.aaron.eglholder.Renderer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class LightRenderer implements Renderer {

    private static final String TAG = "LightRenderer";

    private Ball ball;
    private float lightOffset = -4;

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        //设置屏幕背景色RGBA
        GLES30.glClearColor(0f, 0f, 0f, 1.0f);
        //创建球对象
        ball = new Ball();
        //打开深度检测
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        //打开背面剪裁
        GLES30.glEnable(GLES30.GL_CULL_FACE);
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged");
        //设置视窗大小及位置
        GLES30.glViewport(0, 0, width, height);
        //计算GLSurfaceView的宽高比
        Constant.ratio = (float) width / height;
        // 调用此方法计算产生透视投影矩阵
        MatrixState.setProjectFrustum(-Constant.ratio, Constant.ratio, -1, 1, 20, 100);
        // 调用此方法产生摄像机9参数位置矩阵
        MatrixState.setCamera(0, 0f, 30, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        //初始化变换矩阵
        MatrixState.setInitStack();
    }

    public void onDrawFrame(GL10 gl) {
        Log.d(TAG, "onDrawFrame");
        //清除深度缓冲与颜色缓冲
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        //设置光源位置
        MatrixState.setLightLocation(lightOffset, 0, 1.5f);
        //保护现场
        MatrixState.pushMatrix();
        //绘制球
        MatrixState.pushMatrix();
        MatrixState.translate(-1.2f, 0, 0);
        ball.drawSelf();
        MatrixState.popMatrix();
        //绘制球
        MatrixState.pushMatrix();
        MatrixState.translate(1.2f, 0, 0);
        ball.drawSelf();
        MatrixState.popMatrix();
        //恢复现场
        MatrixState.popMatrix();
    }

    @Override
    public void onSurfaceDestroyed(GL10 gl) {
        ball.release();
    }

    public void setLightOffset(float lightOffset) {
        this.lightOffset = lightOffset;
    }
}
