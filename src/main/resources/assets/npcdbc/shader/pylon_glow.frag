#version 120

uniform sampler2D bgl_RenderedTexture;
uniform float time; // Passed in, see ShaderHelper.java (uploaded via glUniform1f)

// Source: http://wp.applesandoranges.eu/?p=14 (Modified)
void main() {
    vec4 color = vec4(0);
    vec2 texcoord = vec2(gl_TexCoord[0]);

    vec4 center = texture2D(bgl_RenderedTexture, texcoord);

    for(int i = -4 ; i < 4; i++)
        for(int j = -3; j < 3; j++)
            color += texture2D(bgl_RenderedTexture, texcoord + vec2(j, i));

    float brightness = sin(time / 20.0) * 0.5 + 0.5 + 0.15;
    float alpha = sin(time / 30.0) * 0.5 + 0.5 + 0.35;

    // perf: reuse the center sample instead of fetching it a second time
    gl_FragColor = vec4((color * color * 0.0005 * brightness + center).rgb, alpha * gl_Color.a);
}
