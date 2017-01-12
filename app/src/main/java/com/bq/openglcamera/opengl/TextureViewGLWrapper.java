package com.bq.openglcamera.opengl;


import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;

import java.util.concurrent.Semaphore;

/**
 * Wrapper for TextureView that allows attaching a {@link GLRenderer}.
 * Forward all {@link TextureView.SurfaceTextureListener} callbacks to this class
 * and use {@link #getEglSurfaceTexture()} in place of {@link TextureView#getSurfaceTexture()}.
 * <p>
 * Egl context is created and destroyed when the underlying TextureView is created / destroyed.
 */
@SuppressWarnings("Convert2Lambda")
public class TextureViewGLWrapper
   implements SurfaceTexture.OnFrameAvailableListener, TextureView.SurfaceTextureListener {

   private final EglHelper eglHelper = new EglHelper();

   private RenderThread renderThread;
   private SurfaceTexture surfaceTexture;
   private SurfaceTexture eglSurfaceTexture;
   private EGLSurfaceTextureListener listener;
   private Handler listenerHandler;
   private int surfaceWidth, surfaceHeight;

   //Drawing
   private GLRenderer renderer;

   public TextureViewGLWrapper(GLRenderer renderer) {
      this.renderer = renderer;
   }

   /**
    * Configure the listener for the EglSurface creation and the handler used to receive the
    * callback.
    */
   public void setListener(EGLSurfaceTextureListener listener, Handler handler) {
      this.listener = listener;
      this.listenerHandler = handler;
   }

   @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
      if (renderThread != null) {
         throw new IllegalStateException("Already have a context");
      }
      this.surfaceTexture = surface;
      this.renderThread = new RenderThread();
      this.renderThread.start();
      this.surfaceWidth = width;
      this.surfaceHeight = height;
   }

   @Override
   public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
      if (renderThread == null) {
         throw new IllegalStateException("Context not ready");
      }
      this.surfaceWidth = width;
      this.surfaceHeight = height;
      this.renderThread.blockingHandler().post(new Runnable() {
         @Override public void run() {
            renderer.onSurfaceChanged(eglSurfaceTexture, surfaceWidth, surfaceHeight);
         }
      });
   }

   @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
      if (renderThread == null) return true;

      renderThread.handler.post(new Runnable() {
         @Override public void run() {
            Looper looper = Looper.myLooper();
            if (looper != null) {
               looper.quit();
            }
         }
      });
      renderThread = null;
      return true; //Unused
   }

   @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {
      //Nothing to do
   }

   private void configure() {
      //Configure the display
      eglSurfaceTexture = eglHelper.createSurface(surfaceTexture, false);
      renderer.onSurfaceCreated(eglSurfaceTexture, surfaceWidth, surfaceHeight);
      eglSurfaceTexture.setOnFrameAvailableListener(this, renderThread.handler);
      //At this point we should be ready to accept frames from the camera
      listenerHandler.post(new Runnable() {
         @Override public void run() {
            listener.onSurfaceTextureReady(eglSurfaceTexture);
         }
      });
   }

   @Override public void onFrameAvailable(SurfaceTexture surfaceTexture) {
      renderer.onFrameAvailable(eglSurfaceTexture);
      eglHelper.makeCurrent();
      eglHelper.swapBuffers();
   }

   private void dispose() {
      renderer.onSurfaceDestroyed(eglSurfaceTexture);
      eglHelper.destroySurface();
   }

   private class RenderThread extends Thread {
      private Semaphore eglContextReadyLock = new Semaphore(0);
      private Handler handler;

      @Override public void run() {
         Looper.prepare();
         handler = new Handler();
         configure();
         eglContextReadyLock.release();
         Looper.loop();
         dispose();
      }

      Handler blockingHandler() {
         //Block until the EGL context is ready to accept messages
         eglContextReadyLock.acquireUninterruptibly();
         eglContextReadyLock.release();
         return this.handler;
      }
   }

   public SurfaceTexture getEglSurfaceTexture() {
      return eglSurfaceTexture;
   }

   public interface EGLSurfaceTextureListener {
      /**
       * Underlying EGL Context is ready.
       */
      void onSurfaceTextureReady(SurfaceTexture surfaceTexture);
   }

   /**
    * Renderer for the TextureView.
    */
   public interface GLRenderer {

      /**
       * Initialize the shader.
       */
      void onSurfaceCreated(SurfaceTexture eglSurfaceTexture, int surfaceWidth, int surfaceHeight);

      /**
       * Surface resized.
       */
      void onSurfaceChanged(SurfaceTexture eglSurfaceTexture, int surfaceWidth, int surfaceHeight);

      /**
       * Remove allocated resources.
       */
      void onSurfaceDestroyed(SurfaceTexture eglSurfaceTexture);

      /**
       * A frame from the camera is ready to be displayed.
       * <pre>
       * GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
       * surfaceTexture.updateTexImage();
       * </pre>
       * Call {@link SurfaceTexture#updateTexImage()} when {@link android.opengl.GLES20#GL_TEXTURE0}
       * is active to bind the camera output to the <code>samplerExternalOES</code> in the shader.
       */
      void onFrameAvailable(SurfaceTexture eglSurfaceTexture);
   }
}
