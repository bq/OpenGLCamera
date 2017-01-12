/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bq.openglcamera.opengl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * Some OpenGL utility functions. From Grafika
 */
public final class GlUtil {

    /** Identity matrix for general use.  Don't modify or life will get weird. */
    public static final float[] IDENTITY_MATRIX;

    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    private GlUtil() {
        //No instances
    }

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    public static int createProgram(Context context, String vertexAssetFile, String fragmentAssetFile) {
        String vertexSource = getStringFromFileInAssets(context, vertexAssetFile);
        String fragmentSource = getStringFromFileInAssets(context, fragmentAssetFile);
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        Timber.d("vertexShader log: %s", GLES20.glGetShaderInfoLog(vertexShader));

        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            return 0;
        }
        Timber.d("fragmentShader log: %s", GLES20.glGetShaderInfoLog(fragmentShader));

        int program = GLES20.glCreateProgram();
        checkGLError("glCreateProgram");
        if (program == 0) {
            Timber.e("Could not create program from %s, %s", fragmentAssetFile, vertexAssetFile);
        } else {
            GLES20.glAttachShader(program, vertexShader);
            checkGLError("glAttachShader");
            GLES20.glAttachShader(program, fragmentShader);
            checkGLError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Timber.e("Could not link program: %s", GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    private static int compileShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGLError("glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Timber.e("Could not compile shader %d", shaderType);
            Timber.e(GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }


    private static String getStringFromFileInAssets(Context ctx, String filename) {
        try {
            InputStream is = ctx.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            is.close();
            return builder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGLError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Timber.e(msg);
            throw new RuntimeException(msg);
        }
    }

}