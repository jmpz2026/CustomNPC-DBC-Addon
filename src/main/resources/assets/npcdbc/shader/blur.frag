#version 120
varying vec2 texCoord;

uniform vec2 u_resolution;
uniform sampler2D mainTexture;
uniform int horizontal;
uniform float blurIntensity;
// NpcDbcResu perf: 7-tap gaussian folded into 5 bilinear taps (was 7 discrete, was 11).
// Outer pairs (+/-2,+/-3) are sampled between texels so the GPU's free linear
// interpolation does the weighting. Same blur, ~29% fewer texture fetches per pass.
// No GLSL array constructors -> compiles on the widest range of GL2.1 drivers.
const float wCenter = 0.214607;
const float wInner  = 0.189879;            // offset +/-1
const float wOuter  = 0.202817;            // folded weight of offsets +/-2 and +/-3
const float offOuter = 2.351574;           // weighted midpoint of offsets 2 and 3

void main() {
    vec2 resolution = u_resolution / blurIntensity;
    vec2 texelSize = horizontal == 1 ? vec2(1.0 / resolution.x, 0.0) : vec2(0.0, 1.0 / resolution.y);

    vec4 blurColor  = texture2D(mainTexture, texCoord) * wCenter;
    blurColor += texture2D(mainTexture, texCoord + texelSize) * wInner;
    blurColor += texture2D(mainTexture, texCoord - texelSize) * wInner;
    blurColor += texture2D(mainTexture, texCoord + texelSize * offOuter) * wOuter;
    blurColor += texture2D(mainTexture, texCoord - texelSize * offOuter) * wOuter;

    gl_FragColor = blurColor;
}
