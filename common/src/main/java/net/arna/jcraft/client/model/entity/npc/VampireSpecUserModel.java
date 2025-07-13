package net.arna.jcraft.client.model.entity.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mod.azure.azurelib.core.animatable.model.CoreGeoBone;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.model.GeoModel;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.spec.VampireSpecUser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class VampireSpecUserModel extends GeoModel<VampireSpecUser> {

    private final Map<String, JsonObject> animationCache = new HashMap<>();
    private String lastAnimationId = "";
    private float animationStartTime = 0;

    @Override
    public ResourceLocation getModelResource(final VampireSpecUser animatable) {
        return JCraft.id("geo/vampirespecuser.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(final VampireSpecUser animatable) {
        return JCraft.id("textures/entity/vampirespecuser.png");
    }

    @Override
    public ResourceLocation getAnimationResource(final VampireSpecUser animatable) {
        return JCraft.id("animations/vampirespecuser.animation.json");
    }

    @Override
    public void setCustomAnimations(VampireSpecUser animatable, long instanceId, AnimationState<VampireSpecUser> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        String animationId = animatable.getAnimationId();
        float animationSpeed = animatable.getAnimationSpeed();

        if (!animationId.isEmpty()) {
            // Start new animation
            if (!animationId.equals(lastAnimationId)) {
                animationStartTime = (float) animationState.getAnimationTick();
                lastAnimationId = animationId;
            }

            applyPlayerAnimation(animatable, animationId, animationSpeed, animationState);
        } else {
            // Clear animation
            if (!lastAnimationId.isEmpty()) {
                lastAnimationId = "";
                resetBones();
            }
            playDefaultAnimations(animatable, animationState);
        }
    }

    private void applyPlayerAnimation(VampireSpecUser animatable, String animationId, float animationSpeed, AnimationState<VampireSpecUser> animationState) {
        JsonObject animationJson = getAnimation(animationId);
        if (animationJson == null) return;

        JsonObject animations = animationJson.getAsJsonObject("animations");
        if (animations == null) return;

        JsonObject animation = animations.getAsJsonObject(animationId);
        if (animation == null) return;

        float animationLength = animation.get("animation_length").getAsFloat();
        JsonObject bones = animation.getAsJsonObject("bones");
        if (bones == null) return;

        // Calculate current time and check if finished
        float elapsedTime = ((float) animationState.getAnimationTick() - animationStartTime) / 20.0f;
        float currentTime = elapsedTime * animationSpeed;

        if (currentTime >= animationLength) {
            // Animation finished, clear it
            resetBones();
            return;
        }

        // Apply bone transforms
        bones.entrySet().forEach(entry -> {
            String boneName = entry.getKey();
            JsonObject boneData = entry.getValue().getAsJsonObject();
            CoreGeoBone bone = this.getBone(boneName).orElse(null);

            if (bone != null) {
                applyBoneTransforms(bone, boneData, currentTime);
            }
        });
    }

    private void applyBoneTransforms(CoreGeoBone bone, JsonObject boneData, float currentTime) {
        // Apply rotation
        JsonObject rotation = boneData.getAsJsonObject("rotation");
        if (rotation != null) {
            float[] rot = interpolate(rotation, currentTime);
            if (rot != null) {
                // Different coordinate fixes based on bone type
                String boneName = bone.getName();

                if (boneName.equals("head") || boneName.equals("torso")) {
                    // Head and torso seem to work normally
                    bone.setRotX((float) Math.toRadians(rot[0]));
                    bone.setRotY((float) Math.toRadians(rot[1]));
                    bone.setRotZ((float) Math.toRadians(rot[2]));
                } else {
                    // Arms and legs need different coordinate mapping
                    bone.setRotX((float) Math.toRadians(-rot[0])); // Invert X
                    bone.setRotY((float) Math.toRadians(-rot[1])); // Invert Y
                    bone.setRotZ((float) Math.toRadians(rot[2]));  // Keep Z normal
                }
            }
        }

        // Apply position
        JsonObject position = boneData.getAsJsonObject("position");
        if (position != null) {
            float[] pos = interpolate(position, currentTime);
            if (pos != null) {
                bone.setPosX(pos[0]);
                bone.setPosY(pos[1]);
                bone.setPosZ(pos[2]);
            }
        }
    }

    private float[] interpolate(JsonObject transformData, float currentTime) {
        String prevKey = null, nextKey = null;
        float prevTime = 0, nextTime = Float.MAX_VALUE;

        // Find surrounding keyframes
        for (String timeKey : transformData.keySet()) {
            // Skip non-numeric keys
            if (timeKey.equals("vector") || timeKey.equals("easing")) {
                continue;
            }

            try {
                float keyTime = Float.parseFloat(timeKey);
                if (keyTime <= currentTime && keyTime > prevTime) {
                    prevTime = keyTime;
                    prevKey = timeKey;
                }
                if (keyTime >= currentTime && keyTime < nextTime) {
                    nextTime = keyTime;
                    nextKey = timeKey;
                }
            } catch (NumberFormatException e) {
                // Skip any other non-numeric keys
                continue;
            }
        }

        // Single keyframe or exact match
        if (prevKey == null || nextKey == null || prevKey.equals(nextKey)) {
            String key = prevKey != null ? prevKey : nextKey;
            if (key != null) {
                JsonObject keyframe = transformData.getAsJsonObject(key);
                if (keyframe != null && keyframe.has("vector")) {
                    var vector = keyframe.getAsJsonArray("vector");
                    return new float[]{
                            vector.get(0).getAsFloat(),
                            vector.get(1).getAsFloat(),
                            vector.get(2).getAsFloat()
                    };
                }
            }
            return null;
        }

        // Interpolate between keyframes
        JsonObject prevFrame = transformData.getAsJsonObject(prevKey);
        JsonObject nextFrame = transformData.getAsJsonObject(nextKey);

        if (prevFrame == null || nextFrame == null) return null;
        if (!prevFrame.has("vector") || !nextFrame.has("vector")) return null;

        var prevVector = prevFrame.getAsJsonArray("vector");
        var nextVector = nextFrame.getAsJsonArray("vector");

        float factor = (currentTime - prevTime) / (nextTime - prevTime);

        // Apply easing
        String easing = nextFrame.has("easing") ? nextFrame.get("easing").getAsString() : "linear";
        factor = applyEasing(factor, easing);

        return new float[]{
                lerp(prevVector.get(0).getAsFloat(), nextVector.get(0).getAsFloat(), factor),
                lerp(prevVector.get(1).getAsFloat(), nextVector.get(1).getAsFloat(), factor),
                lerp(prevVector.get(2).getAsFloat(), nextVector.get(2).getAsFloat(), factor)
        };
    }

    private float applyEasing(float t, String easingType) {
        return switch (easingType.toLowerCase()) {
            case "easeinoutsine" -> (float) (-(Math.cos(Math.PI * t) - 1) / 2);
            case "easein" -> t * t;
            case "easeout" -> (float) (1 - Math.pow(1 - t, 2));
            case "easeinout" -> (float) (t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2);
            default -> t;
        };
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private JsonObject getAnimation(String animationId) {
        JsonObject cached = animationCache.get(animationId);
        if (cached != null) return cached;

        try {
            ResourceLocation file = JCraft.id("player_animation/vampire/" + animationId + ".json");
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(file).orElse(null);

            if (resource != null) {
                try (InputStream stream = resource.open()) {
                    JsonElement element = JsonParser.parseReader(new InputStreamReader(stream));
                    if (element.isJsonObject()) {
                        JsonObject json = element.getAsJsonObject();
                        animationCache.put(animationId, json);
                        return json;
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        return null;
    }

    private void resetBones() {
        String[] boneNames = {"head", "torso", "leftArm", "rightArm", "leftLeg", "rightLeg"};
        for (String boneName : boneNames) {
            CoreGeoBone bone = this.getBone(boneName).orElse(null);
            if (bone != null) {
                bone.setRotX(0);
                bone.setRotY(0);
                bone.setRotZ(0);
                bone.setPosX(0);
                bone.setPosY(0);
                bone.setPosZ(0);
            }
        }
    }

    private void playDefaultAnimations(VampireSpecUser animatable, AnimationState<VampireSpecUser> animationState) {
        AnimationController<VampireSpecUser> controller = animationState.getController();
        if (controller == null) return;

        boolean isMoving = animatable.getDeltaMovement().horizontalDistanceSqr() > 0.01;
        String animName = isMoving ? "walk" : "idle";

        if (controller.tryTriggerAnimation(animName)) {
            controller.setAnimationSpeed(1.0f);
        }
    }
}