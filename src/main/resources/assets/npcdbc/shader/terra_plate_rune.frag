#version 120

uniform sampler2D bgl_RenderedTexture;
uniform float time; // Passed in, see ShaderHelper.java (uploaded via glUniform1f)

void main() {
    vec2 texcoord = vec2(gl_TexCoord[0]);
    vec4 color = texture2D(bgl_RenderedTexture, texcoord);
    float gs = (color.r + color.g + color.b) / 16.0;
    
    float tx = (texcoord.x + 0.075) * 10.0;
    float ty = (texcoord.y) * 10.0;
    float g = ((sin((time + sin(tx) * 250.0 + sin(ty) * 50.0) / 6.0) + 1.0) * 0.5 + 0.5) / 5.0;
    float b = ((cos((time + sin(tx) * 50.0 + sin(ty) * 100.0) / 6.0) + 1.0) * 0.5 + 0.5) / 5.0;
    
    gl_FragColor = vec4(gs - 0.1, gs + g, gs + b, color.a * gl_Color.a * 1.4);
}