#version 120

uniform sampler2D bgl_RenderedTexture;
uniform float time; // Passed in, see ShaderHelper.java (uploaded via glUniform1f)

// Source: http://wp.applesandoranges.eu/?p=14 (Modified)
void main() {
   vec4 sum = vec4(0);
   vec2 texcoord = vec2(gl_TexCoord[0]);
   float brightness = sin(time / 20.0) * 0.5 + 0.5;
   
   for(int i = -4 ; i < 4; i++)
        for(int j = -3; j < 3; j++)
            sum += texture2D(bgl_RenderedTexture, texcoord + vec2(j, i) * 0.004) * 0.25;

    // perf: sample the center texel once instead of up to 5 times
    vec4 center = texture2D(bgl_RenderedTexture, texcoord);
    if(center.r < 0.3)
        gl_FragColor = sum * sum * 0.012 + center;
    else if(center.r < 0.5)
        gl_FragColor = sum * sum * 0.009 * brightness + center;
    else
        gl_FragColor = sum * sum * 0.0075 * brightness + center;
}