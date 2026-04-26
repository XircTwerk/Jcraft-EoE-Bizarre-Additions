package net.arna.jcraft.api.spec;

public interface JSpecHolder {
    void setSpecType(SpecType type);
    JSpec<?, ?> getSpec();
    void setAnimation(String animationID, float animationSpeed);
}
