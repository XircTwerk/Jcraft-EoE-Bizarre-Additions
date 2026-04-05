package net.arna.jcraft.client;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import mod.azure.azurelib.render.armor.AzArmorRendererRegistry;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.pose.PoseModifiers;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.client.renderer.armor.*;
import net.arna.jcraft.client.rendering.DamageIndicatorManager;
import net.arna.jcraft.client.particle.DamageNumberParticle;
import net.arna.jcraft.client.rendering.StandUserPoseLoader;
import net.arna.jcraft.client.gravity.util.GravityChannelClient;
import net.arna.jcraft.client.gui.hud.JCraftAbilityHud;
import net.arna.jcraft.client.net.ClientPacketHandler;
import net.arna.jcraft.client.particle.*;
import net.arna.jcraft.client.registry.*;
import net.arna.jcraft.client.renderer.effects.AttackHitboxEffectRenderer;
import net.arna.jcraft.client.renderer.effects.TimeErasePredictionEffectRenderer;
import net.arna.jcraft.client.rendering.RenderHandler;
import net.arna.jcraft.client.rendering.handler.*;
import net.arna.jcraft.client.util.ClientEntityHandlerImpl;
import net.arna.jcraft.client.util.TrackedKeyBinding;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.util.MovementInputType;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class JCraftClient {
    // Keybinds
    public static TrackedKeyBinding standSummon, heavyKey, barrageKey, ultKey, special1Key, special2Key, special3Key,
            comboBreaker, cooldownCancel, utility, dash;
    @SuppressWarnings({"ConstantValue", "DataFlowIssue"}) // Not the case here cuz of the lazy getter.
    @Getter(lazy = true)
    private static final Map<TrackedKeyBinding, MoveInputType> bindings = ImmutableMap.<TrackedKeyBinding, MoveInputType>builder()
            .put(standSummon, MoveInputType.STAND_SUMMON)
            .put(TrackedKeyBinding.wrap(Minecraft.getInstance().options.keyAttack), MoveInputType.LIGHT)
            .put(heavyKey, MoveInputType.HEAVY)
            .put(barrageKey, MoveInputType.BARRAGE)
            .put(special1Key, MoveInputType.SPECIAL1)
            .put(special2Key, MoveInputType.SPECIAL2)
            .put(special3Key, MoveInputType.SPECIAL3)
            .put(ultKey, MoveInputType.ULTIMATE)
            .put(utility, MoveInputType.UTILITY)
            .build();
    @Getter(lazy = true)
    private static final Map<TrackedKeyBinding, MovementInputType> movementBindings = createMovementBindingsMap();
    @Getter(lazy = true)
    private static final TrackedKeyBinding trackedUseKey = TrackedKeyBinding.wrap(Minecraft.getInstance().options.keyUse);
    public static Supplier<DecimalFormat> decimalFormat = Suppliers.memoize(JCraftClient::createDecimalFormat);
    // public static KeyMapping menuKey;
    public static boolean comboStarted = false;
    public static int framesSinceComboStarted = 0;

    public static void init() {
        JCraft.setClientEntityHandler(ClientEntityHandlerImpl.INSTANCE);

        AutoConfig.register(JClientConfig.class, JanksonConfigSerializer::new);
        JClientConfig.load();

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new DecimalFormatUpdater());
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, StandUserPoseLoader::onReload);

        GravityChannelClient.init();

        // Rendering
        JRenderLayerRegistry.init();
        RenderHandler.init();
        JClientEventsRegistry.registerClientEvents();
        JCraftAbilityHud.init();
        PoseModifiers.register();

        initCosplay();

        InversionShaderHandler.INSTANCE.init();
        ZaWarudoShaderHandler.INSTANCE.init();
        CrimsonShaderHandler.INSTANCE.init();
        EpitaphVignetteShaderHandler.INSTANCE.init();

        // Renderer registration
        // JArmorRendererRegistry.registerArmorRenderers();

        ClientPacketHandler.init();

        AttackHitboxEffectRenderer.init();
        TimeErasePredictionEffectRenderer.init();
    }

    private static void initCosplay() {
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIAVOLO_WIG) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diavoloclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIAVOLO_SHIRT) {
            AzArmorRendererRegistry.register(ArmorRenderer.flutter("diavoloshirt"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIAVOLO_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diavoloclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIAVOLO_BOOTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diavoloclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIEGO_HAT) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diegooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIEGO_SHIRT) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diegooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIEGO_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diegooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIEGO_BOOTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diegooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_HEADBAND) {
            AzArmorRendererRegistry.register(ArmorRenderer.flutter("diojacket"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_JACKET) {
            AzArmorRendererRegistry.register(ArmorRenderer.flutter("diojacket"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_PANTS) {
            AzArmorRendererRegistry.register(DIOtardRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_BOOTS) {
            AzArmorRendererRegistry.register(DIOtardRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_CAPE) {
            AzArmorRendererRegistry.register(DIOCapeRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_P1_WIG) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_P1_JACKET) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_P1_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DIO_P1_BOOTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("diooutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DOPPIO_WIG) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("doppiotop"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.DOPPIO_SHIRT) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("doppiotop"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.FINAL_KIRA_WIG) {
            AzArmorRendererRegistry.register(FinalKiraArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.FINAL_KIRA_JACKET) {
            AzArmorRendererRegistry.register(FinalKiraJacketRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.FINAL_KIRA_PANTS) {
            AzArmorRendererRegistry.register(FinalKiraArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.FINAL_KIRA_BOOTS) {
            AzArmorRendererRegistry.register(FinalKiraArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GIORNO_WIG) {
            AzArmorRendererRegistry.register(GiornoArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GIORNO_JACKET) {
            AzArmorRendererRegistry.register(GiornoJacketRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GIORNO_PANTS) {
            AzArmorRendererRegistry.register(GiornoArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GIORNO_BOOTS) {
            AzArmorRendererRegistry.register(GiornoArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GYRO_HAT) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("gyrotop"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GYRO_SHIRT) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("gyrotop"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GYRO_PANTS) {
            AzArmorRendererRegistry.register(GyroBottomRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.GYRO_BOOTS) {
            AzArmorRendererRegistry.register(GyroBottomRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.HEAVEN_ATTAINED_WIG) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("heavenattainedoutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.HEAVEN_ATTAINED_SHIRT) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("heavenattainedoutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.HEAVEN_ATTAINED_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("heavenattainedoutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.HEAVEN_ATTAINED_BOOTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("heavenattainedoutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOHNNY_CAP) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("johnnyclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOHNNY_JACKET) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("johnnyclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOHNNY_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("johnnyclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOHNNY_BOOTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("johnnyclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_CAP) {
            AzArmorRendererRegistry.register(JotaroArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_JACKET) {
            AzArmorRendererRegistry.register(JotaroCoatRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_PANTS) {
            AzArmorRendererRegistry.register(JotaroArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_BOOTS) {
            AzArmorRendererRegistry.register(JotaroArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P4_CAP) {
            AzArmorRendererRegistry.register(JotaroArmorP4Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P4_JACKET) {
            AzArmorRendererRegistry.register(JotaroCoatP4Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P4_PANTS) {
            AzArmorRendererRegistry.register(JotaroArmorP4Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P4_BOOTS) {
            AzArmorRendererRegistry.register(JotaroArmorP4Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P6_CAP) {
            AzArmorRendererRegistry.register(JotaroArmorP6Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P6_JACKET) {
            AzArmorRendererRegistry.register(JotaroCoatP6Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P6_PANTS) {
            AzArmorRendererRegistry.register(JotaroArmorP6Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.JOTARO_P6_BOOTS) {
            AzArmorRendererRegistry.register(JotaroArmorP6Renderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KAKYOIN_WIG) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("kakyoinclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KAKYOIN_COAT) {
            AzArmorRendererRegistry.register(KakyoinCoatRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KAKYOIN_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("kakyoinclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KAKYOIN_BOOTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("kakyoinclothes"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KARS_HEADWRAP) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("karsoutfit"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KIRA_WIG) {
            AzArmorRendererRegistry.register(KiraArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KIRA_JACKET) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("kirajacket"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KIRA_PANTS) {
            AzArmorRendererRegistry.register(KiraArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KIRA_BOOTS) {
            AzArmorRendererRegistry.register(KiraArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KOSAKU_WIG) {
            AzArmorRendererRegistry.register(KosakuArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KOSAKU_JACKET) {
            AzArmorRendererRegistry.register(KosakuJacketRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KOSAKU_PANTS) {
            AzArmorRendererRegistry.register(KosakuArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.KOSAKU_BOOTS) {
            AzArmorRendererRegistry.register(KosakuArmorRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.PUCCI_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("puccis_hat"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.PUCCI_ROBE) {
            AzArmorRendererRegistry.register(PucciRobeRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.PUCCI_PANTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("puccibottom"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.PUCCI_BOOTS) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("puccibottom"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.RED_HAT) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("red_hat"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.RINGO_OUTFIT) {
            AzArmorRendererRegistry.register(RingoOutfitRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.RINGO_BOOTS) {
            AzArmorRendererRegistry.register(RingoOutfitRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.RISOTTO_JACKET) {
            AzArmorRendererRegistry.register(RisottoCapRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.RISOTTO_JACKET) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("risottotop"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.RISOTTO_PANTS) {
            AzArmorRendererRegistry.register(RisottoBottomRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.RISOTTO_BOOTS) {
            AzArmorRendererRegistry.register(RisottoBottomRenderer::new, item.get());
        }
        AzArmorRendererRegistry.register(ArmorRenderer.simple("stone_mask"), JItemRegistry.STONE_MASK.get());
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.STRAIZO_PONCHO) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("straizoponcho"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.STRAIZO_PONCHO) {
            AzArmorRendererRegistry.register(ArmorRenderer.simple("straizoponcho"), item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.VALENTINE_WIG) {
            AzArmorRendererRegistry.register(ValentineTopRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.VALENTINE_JACKET) {
            AzArmorRendererRegistry.register(ValentineTopRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.VALENTINE_PANTS) {
            AzArmorRendererRegistry.register(ValentineBottomRenderer::new, item.get());
        }
        for (RegistrySupplier<? extends ArmorItem> item : JItemRegistry.VALENTINE_BOOTS) {
            AzArmorRendererRegistry.register(ValentineBottomRenderer::new, item.get());
        }
    }

    public static void registerKeyBindings(@Nullable Consumer<KeyMapping> register) {
        if (register == null) register = KeyMappingRegistry::register;

        // Keybinding registration
        standSummon = TrackedKeyBinding.createAndRegister("key.jcraft.standsummon", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_N, "key.category.jcraft", register);
        heavyKey = TrackedKeyBinding.createAndRegister("key.jcraft.heavy", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R, "key.category.jcraft", register);
        barrageKey = TrackedKeyBinding.createAndRegister("key.jcraft.barrage", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B, "key.category.jcraft", register);
        ultKey = TrackedKeyBinding.createAndRegister("key.jcraft.ultimate", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H, "key.category.jcraft", register);
        special1Key = TrackedKeyBinding.createAndRegister("key.jcraft.special1", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V, "key.category.jcraft", register);
        special2Key = TrackedKeyBinding.createAndRegister("key.jcraft.special2", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G, "key.category.jcraft", register);
        special3Key = TrackedKeyBinding.createAndRegister("key.jcraft.special3", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M, "key.category.jcraft", register);
        //comboBreaker = TrackingKeyBinding.createAndRegister("key.jcraft.combobreaker", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.category.jcraft");
        cooldownCancel = TrackedKeyBinding.createAndRegister("key.jcraft.cooldowncancel", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT, "key.category.jcraft", register);
        utility = TrackedKeyBinding.createAndRegister("key.jcraft.utility", InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_5, "key.category.jcraft", register);
        dash = TrackedKeyBinding.createAndRegister("key.jcraft.dash", InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4, "key.category.jcraft", register);

        // todo: actually finish jcraft menu
        // menuKey = new KeyMapping("key.jcraft.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_DIVIDE, "key.category.jcraft");
        // register.accept(menuKey);
    }

    /// TEXT HUD
    public static final List<String> comboRemarks = List.of("admin rdm!!!", "baby combo", "caught lackin", "kinda ez", "skill issue", "cancelled on twitter", "sent to bulgaria", "down bad");
    public static int comboCounter = 0;
    public static float damageScaling = 1.00f;
    public static int framesSinceCounted = 0;

    public static int IPSTriggerFramesLeft = 0;
    public static final int IPS_TRIGGER_FRAMES = 120;

    public static void markIPSTriggered() {
        IPSTriggerFramesLeft = IPS_TRIGGER_FRAMES;
    }


    public static void markComboStarted() {
        comboStarted = true;
        framesSinceComboStarted = 0;
    }

    private static Map<TrackedKeyBinding, MovementInputType> createMovementBindingsMap() {
        final Options options = Minecraft.getInstance().options;
        return ImmutableMap.<TrackedKeyBinding, MovementInputType>builder()
                .put(TrackedKeyBinding.wrap(options.keyUp), MovementInputType.FORWARD)
                .put(TrackedKeyBinding.wrap(options.keyDown), MovementInputType.BACKWARD)
                .put(TrackedKeyBinding.wrap(options.keyLeft), MovementInputType.LEFT)
                .put(TrackedKeyBinding.wrap(options.keyRight), MovementInputType.RIGHT)
                .put(TrackedKeyBinding.wrap(options.keyJump), MovementInputType.JUMP)
                .put(TrackedKeyBinding.wrap(options.keyShift), MovementInputType.CROUCH)
                .put(dash, MovementInputType.DASH)
                .build();
    }

    /**
     * @return a cleaned-up version of TranslatableText name of button
     */
    public static String generateName(final KeyMapping keyBinding, boolean makeShort) {
        final String str = keyBinding.saveString();
        final String[] components = str.split("\\.");

        String last = components[components.length - 1];
        String secondLast = components[components.length - 2] + " ";

        if (components[components.length - 2].equals("keyboard")) {
            secondLast = "";
        }

        if (makeShort) {
            if (secondLast.length() > 0) secondLast = secondLast.substring(0, 1);
            if (last.length() > 0) last = last.substring(0, 1);
        }

        return StringUtils.capitalize(secondLast) + StringUtils.capitalize(last);
    }

    public static <E extends Enum<E>> Object2BooleanMap<E> getChangedInputs(final Map<TrackedKeyBinding, E> bindings) {
        return bindings.entrySet().stream()
                .filter(entry -> entry.getKey().isChangedThisTick())
                .collect(Object2BooleanOpenHashMap::new, (map, entry) -> map.put(entry.getValue(), entry.getKey().isPressedThisTick()),
                        Object2BooleanMap::putAll);
    }

    @Nullable
    public static StandEntity<?, ?> getStandEntity() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }

        for (Entity e : player.getPassengers()) {
            if (e instanceof StandEntity<?, ?> s) {
                return s;
            }
        }
        return null;
    }

    private static DecimalFormat createDecimalFormat() {
        return new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(
                Locale.forLanguageTag(Minecraft.getInstance().options.languageCode)));
    }

    public static void registerParticleSpriteSets() {
        ParticleProviderRegistry.register(JParticleTypeRegistry.COMBO_BREAK, ComboBreakerParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.COOLDOWN_CANCEL, CooldownCancelParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.HITSPARK_1, provider -> new HitsparkParticle.Factory(provider, 0.4f, 5));
        ParticleProviderRegistry.register(JParticleTypeRegistry.HITSPARK_2, provider -> new HitsparkParticle.Factory(provider, 0.66f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.HITSPARK_3, provider -> new HitsparkParticle.Factory(provider, 1f, 8));
        ParticleProviderRegistry.register(JParticleTypeRegistry.INVERTED_HITSPARK_3, provider -> new InvertedHitsparkParticle.Factory(provider, 1f, 8));
        ParticleProviderRegistry.register(JParticleTypeRegistry.STUN_SLASH, provider -> new HitsparkParticle.Factory(provider, 0.6f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.STUN_PIERCE, provider -> new HitsparkParticle.Factory(provider, 0.6f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.KCPARTICLE, KCParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BACKSTAB, BackstabParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.SPEED_PARTICLE, SpeedParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BITES_THE_DUST, BitesTheDustParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BOOM_1, BoomParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.PIXEL, PixelParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BLOCKSPARK, provider -> new BlocksparkParticle.Factory(provider, 0.15f));
        ParticleProviderRegistry.register(JParticleTypeRegistry.GO, GoParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.AURA_ARC, AuraArcParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.AURA_BLOB, AuraBlobParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.INVERSION, InversionParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.SUN_LOCK_ON, BackstabParticle.Factory::new); // 9 frames, reusing
        ParticleProviderRegistry.register(JParticleTypeRegistry.PURPLE_HAZE_CLOUD, PurpleHazeCloudParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.PURPLE_HAZE_PARTICLE, PurpleHazeErraticParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.DAMAGE_NUMBER, DamageNumberParticle.Factory::new);
        DamageIndicatorManager.setDamageNumberParticle(JParticleTypeRegistry.DAMAGE_NUMBER.get());
        ParticleProviderRegistry.register(JParticleTypeRegistry.HAMON_SPARK, provider -> new HitsparkParticle.Factory(provider, 0.2f, 6));
    }

    @Getter
    private static class DecimalFormatUpdater implements PreparableReloadListener {
        private final ResourceLocation fabricId = JCraft.id("decimal_format_updater");

        @Override
        public CompletableFuture<Void> reload(final PreparationBarrier synchronizer, final ResourceManager manager, final ProfilerFiller prepareProfiler,
                                              final ProfilerFiller applyProfiler, final Executor prepareExecutor, final Executor applyExecutor) {
            return synchronizer.wait(Unit.INSTANCE).thenRunAsync(() ->
                    decimalFormat = Suppliers.memoize(JCraftClient::createDecimalFormat), applyExecutor);
        }
    }
}
