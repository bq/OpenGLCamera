#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES camTex;
varying vec2 camTexCoordinate;

void main () {
    vec4 color = texture2D(camTex, camTexCoordinate);
    gl_FragColor = color;
}
