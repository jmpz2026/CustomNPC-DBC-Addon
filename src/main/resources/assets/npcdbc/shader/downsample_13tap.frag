#version 120

uniform sampler2D mainTexture;
uniform vec2 u_resolution;
varying vec2 texCoord;


vec4 Downsample13(vec2 uv) {
    vec2 texelSize = vec2(1./(u_resolution.xy));

    vec4 A = texture2D(mainTexture, uv + texelSize * vec2(-1.0, -1.0));
    vec4 B = texture2D(mainTexture, uv + texelSize * vec2(0.0, -1.0));
    vec4 C = texture2D(mainTexture, uv + texelSize * vec2(1.0, -1.0));
    vec4 D = texture2D(mainTexture, uv + texelSize * vec2(-0.5, -0.5));
    vec4 E = texture2D(mainTexture, uv + texelSize * vec2(0.5, -0.5));
    vec4 F = texture2D(mainTexture, uv + texelSize * vec2(-1.0, 0.0));
    vec4 G = texture2D(mainTexture, uv);
    vec4 H = texture2D(mainTexture, uv + texelSize * vec2(1.0, 0.0));
    vec4 I = texture2D(mainTexture, uv + texelSize * vec2(-0.5, 0.5));
    vec4 J = texture2D(mainTexture, uv + texelSize * vec2(0.5, 0.5));
    vec4 K = texture2D(mainTexture, uv + texelSize * vec2(-1.0, 1.0));
    vec4 L = texture2D(mainTexture, uv + texelSize * vec2(0.0, 1.0));
    vec4 M = texture2D(mainTexture, uv + texelSize * vec2(1.0, 1.0));

    vec2 div = (1.0 / 4.0) * vec2(0.5, 0.125);

    vec4 pixel = (D + E + I + J) * div.x;
    pixel += (A + B + G + F) * div.y;
    pixel += (B + C + H + G) * div.y;
    pixel += (F + G + L + K) * div.y;
    pixel += (G + H + M + L) * div.y;

    return pixel;
}

void main() {
    vec3 color = Downsample13(texCoord ).rgb;

    gl_FragColor = vec4(color, 1.0);
}

