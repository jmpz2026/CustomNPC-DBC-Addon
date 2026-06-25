#version 120
varying vec2 texCoord;

uniform vec2 u_resolution;
uniform sampler2D mainTexture;
uniform int horizontal;
uniform float blurIntensity;
// NpcDbcResu perf: 7-tap gaussian (was 11-tap). ~36% fewer texture fetches per blur pass.
const int kernelSize = 3;
const float weights[7] = float[](0.071303, 0.131514, 0.189879, 0.214607, 0.189879, 0.131514, 0.071303);


void main() {
    vec4 blurColor = vec4(0);
    vec2 resolution = u_resolution / blurIntensity;
    vec2 texelSize = horizontal == 1? vec2(1./resolution.x, 0) : vec2(0, 1./resolution.y);

    for (int x = -kernelSize; x<= kernelSize; x++){
        blurColor += texture2D(mainTexture, texCoord + float(x) * texelSize) * weights[x+kernelSize];
    }

    gl_FragColor =blurColor;
}
