#version 120

varying vec2 texCoord;

uniform sampler2D sceneTexture;
uniform sampler2D bloomTexture;
uniform float exposure;

void main() {
    vec4 sceneColor = texture2D(sceneTexture, texCoord);
    vec4 bloomColor = texture2D(bloomTexture, texCoord);

    bloomColor.rgb = vec3(1.0) - exp(-bloomColor.rgb * exposure); // tone mapping

    gl_FragColor = sceneColor + bloomColor;
}
