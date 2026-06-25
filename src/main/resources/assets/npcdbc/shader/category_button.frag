#version 120

uniform float time; // Passed in, see ShaderHelper.java (uploaded via glUniform1f)

uniform float heightMatch; // Passed in via Callback
uniform sampler2D image;
uniform sampler2D mask;

void main() {
    vec2 texcoord = vec2(gl_TexCoord[0]);
    vec4 color = texture2D(image, texcoord);
    vec4 maskColor = texture2D(mask, texcoord);
    float maskgs = (maskColor.r + maskColor.g + maskColor.b) / 3.0;

    if(maskgs <= heightMatch)
    	gl_FragColor = color;
    else gl_FragColor = vec4(0.0, 0.0, 0.0, color.a);
}