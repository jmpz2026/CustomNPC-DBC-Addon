#version 120

uniform vec2 u_resolution;
varying vec3 vertexPos;
uniform float time;
varying vec4 fragColor;
void main() {
    vec3 color1 = vec3(0.0, 1.0, 1.0);
    vec3 color2 = vec3(1.0, 1.0, 1.0);
    vec3 col = mix(color1, color2, vertexPos.y + 0.3);
    float alphaGlow = 0.5 + sin(time * 4.0) * 0.25;
    gl_FragColor = vec4(col, alphaGlow);
}
