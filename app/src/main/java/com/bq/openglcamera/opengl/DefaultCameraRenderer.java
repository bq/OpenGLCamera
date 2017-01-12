package com.bq.openglcamera.opengl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.renderscript.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Default camera renderer that simply draws a quad with the camera texture.
 */
public class DefaultCameraRenderer implements TextureViewGLWrapper.GLRenderer {
   private final Context context;

   private FloatBuffer positionBuffer;
   private FloatBuffer texturePositionBuffer;
   private ShortBuffer drawOrderBuffer;

   private int program = 0;
   private int positionHandle;
   private int texturePositionHandle;
   private int camTexMatrixHandle;
   private int mvpMatrixHandle;


   private Matrix4f cameraTextureMatrix = new Matrix4f();
   private Matrix4f mvpMatrix = new Matrix4f();

   private int surfaceWidth;
   private int surfaceHeight;

   public DefaultCameraRenderer(Context context) {
      this.context = context;
   }

   @Override
   public void onSurfaceCreated(SurfaceTexture eglSurfaceTexture, int surfaceWidth, int surfaceHeight) {
      this.surfaceWidth = surfaceWidth;
      this.surfaceHeight = surfaceHeight;

      //We are drawing two triangles for the texture
      short vertexOrder[] = {0, 1, 2, 1, 3, 2};
      float vertexCoordinates[] = {
         -1, +1,
         +1, +1,
         -1, -1,
         +1, -1,
      };

      //Tex coordinates are flipped vertically
      float vertexTextureCoordinates[] = {
         0.0f, 1.0f,
         1.0f, 1.0f,
         0.0f, 0.0f,
         1.0f, 0.0f,
      };

      ByteBuffer bb;

      // Draw list buffer
      bb = ByteBuffer.allocateDirect(vertexOrder.length * 2); //2 bytes short
      bb.order(ByteOrder.nativeOrder());
      drawOrderBuffer = bb.asShortBuffer();
      drawOrderBuffer.put(vertexOrder);
      drawOrderBuffer.position(0);

      // Initialize the texture holder
      bb = ByteBuffer.allocateDirect(vertexCoordinates.length * 4); //4 bytes/float
      bb.order(ByteOrder.nativeOrder());
      positionBuffer = bb.asFloatBuffer();
      positionBuffer.put(vertexCoordinates);
      positionBuffer.position(0);

      bb = ByteBuffer.allocateDirect(vertexTextureCoordinates.length * 4); //4 bytes/float
      bb.order(ByteOrder.nativeOrder());
      texturePositionBuffer = bb.asFloatBuffer();
      texturePositionBuffer.put(vertexTextureCoordinates);
      texturePositionBuffer.position(0);

      program = GlUtil.createProgram(context, "vert.glsl", "frag.glsl");
      if (program == 0) throw new IllegalStateException("Failed to create program");

      GLES20.glUseProgram(program);
      camTexMatrixHandle = GLES20.glGetUniformLocation(program, "camTexMatrix");
      mvpMatrixHandle = GLES20.glGetUniformLocation(program, "mvpMatrix");
      positionHandle = GLES20.glGetAttribLocation(program, "position");
      texturePositionHandle = GLES20.glGetAttribLocation(program, "texturePosition");
      GlUtil.checkGLError("getLocations");
   }

   @Override
   public void onSurfaceChanged(SurfaceTexture eglSurfaceTexture, int surfaceWidth, int surfaceHeight) {
      this.surfaceWidth = surfaceWidth;
      this.surfaceHeight = surfaceHeight;
   }

   @Override public void onFrameAvailable(SurfaceTexture eglSurfaceTexture) {
      //Update camera parameters
      GLES20.glUseProgram(program);

      //Make the texture available to the shader
      GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
      GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

      //Update texture
      GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
      eglSurfaceTexture.updateTexImage();

      //Update transform matrix
      eglSurfaceTexture.getTransformMatrix(cameraTextureMatrix.getArray());
      GLES20.glUniformMatrix4fv(camTexMatrixHandle, 1, false, cameraTextureMatrix.getArray(), 0);

      //Send position
      GLES20.glEnableVertexAttribArray(positionHandle);
      GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 4 * 2, positionBuffer);

      //Send texture positions
      GLES20.glEnableVertexAttribArray(texturePositionHandle);
      GLES20.glVertexAttribPointer(texturePositionHandle, 2, GLES20.GL_FLOAT, false, 4 * 2, texturePositionBuffer);

      //Send Mvp Matrix
      GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix.getArray(), 0);
      //And draw
      GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrderBuffer.remaining(), GLES20.GL_UNSIGNED_SHORT, drawOrderBuffer);
   }

   @Override public void onSurfaceDestroyed(SurfaceTexture eglSurfaceTexture) {
      //We have nothing to dispose
   }
}
