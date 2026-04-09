#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

const vec3 pinkColor = vec3(1.0, 0.2, 0.6); //same as default mandom aura color
//TODO: make the overlay change relative to mandom's aura color for skin

void main() {
    // Subtle wave distortion: horizontal ripple + gentle vertical roll
    vec2 coord = texCoord;
    coord.x += sin(texCoord.y * 12.0 + Time * 4.0) * 0.004 * Intensity;
    coord.y += cos(texCoord.x * 8.0  + Time * 3.0) * 0.002 * Intensity;

    vec4 scene = texture(DiffuseSampler, coord);

    // Full-screen pink overlay rectangle with alpha bcs mixing pixels isnt common practice
    vec4 overlay = vec4(pinkColor, Intensity * 0.5);

    // Porter-Duff "over": overlay on top of scene/source
    fragColor = vec4(overlay.rgb * overlay.a + scene.rgb * (1.0 - overlay.a), scene.a);
}