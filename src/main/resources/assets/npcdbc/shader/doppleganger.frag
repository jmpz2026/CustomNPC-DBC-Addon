#version 120

varying vec2 texcoord;
varying vec3 vertPos;

uniform sampler2D bgl_RenderedTexture;
uniform float time; // Passed in, see ShaderHelper.java (uploaded via glUniform1f)

uniform float grainIntensity; // Passed in via Callback

float rand(vec2 co) {
   return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec4 color = texture2D(bgl_RenderedTexture, texcoord);
    float gs = (color.r + color.g + color.b) / 50.0;
    
    // perf: rand(texcoord) was identical across all 3 channels -> compute once
    float v = gs + rand(texcoord) * grainIntensity;

    gl_FragColor = vec4(v, v, v, color.a);

}