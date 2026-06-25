#version 120

uniform sampler2D bgl_RenderedTexture;
uniform float time; // Passed in, see ShaderHelper.java (uploaded via glUniform1f)

float rand(vec2 co) {
   return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec2 texcoord = vec2(gl_TexCoord[0]);
    vec4 color = texture2D(bgl_RenderedTexture, texcoord);
    float gs = rand(vec2(int(texcoord.x * 256.0), int(texcoord.y * 512.0))) * 0.5;
    
    gl_FragColor = vec4(gs, gs, gs, color.a);
}