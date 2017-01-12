//The matrix the camera internally applies to the output it produces
uniform mat4 camTexMatrix;
//MVP matrix for the quad we are drawing
uniform mat4 mvpMatrix;

attribute vec4 position;
attribute vec4 texturePosition;

varying vec2 camTexCoordinate;

void main() {
    camTexCoordinate = (camTexMatrix * texturePosition).xy;
    gl_Position = mvpMatrix * position;
}