#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;

in vec2 texCoord;
in vec4 vPosition;

out vec4 fragColor;

const vec3 pinkColor = vec3(1.0, 0.2, 0.6);

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // Mix the original color with pink based on intensity
    vec3 flashedColor = mix(color.rgb, pinkColor, Intensity * 0.5);

    fragColor = vec4(flashedColor, color.a);
}