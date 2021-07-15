package me.jellysquid.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.binding.compat.VanillaBooleanOptionBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawChunkRenderBackend;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.options.AttackIndicator;
import net.minecraft.client.options.GraphicsMode;
import net.minecraft.client.options.Option;
import net.minecraft.client.options.ParticlesMode;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.List;

public class SodiumGameOptionPages {
    private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
    private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();

    public static OptionPage general() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("渲染距离")
                        .setTooltip("渲染距离控制地形应被渲染至多远。更低的距离意味着更少的可见地形，也就有利于提升帧率。")
                        .setControl(option -> new SliderControl(option, 2, 32, 1, ControlValueFormatter.quantity("个区块距离")))
                        .setBinding((options, value) -> options.viewDistance = value, options -> options.viewDistance)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("明亮")
                        .setTooltip("控制游戏画面的亮度（伽马值）。")
                        .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                        .setBinding((opts, value) -> opts.gamma = value * 0.01D, (opts) -> (int) (opts.gamma / 0.01D))
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("云")
                        .setTooltip("控制云是否可见。")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.quality.enableClouds = value;

                            if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                                Framebuffer framebuffer = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
                                if (framebuffer != null) {
                                    framebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                                }
                            }
                        }, (opts) -> opts.quality.enableClouds)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("界面尺寸")
                        .setTooltip("设置界面尺寸比例。如果设置为“自动”，则使用可能的最大比例。")
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.guiScale()))
                        .setBinding((opts, value) -> {
                            opts.guiScale = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            client.onResolutionChanged();
                        }, opts -> opts.guiScale)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("全屏")
                        .setTooltip("启用后，游戏将全屏显示（若支持）。")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> {
                            opts.fullscreen = value;

                            MinecraftClient client = MinecraftClient.getInstance();
                            Window window = client.getWindow();

                            if (window != null && window.isFullscreen() != opts.fullscreen) {
                                window.toggleFullscreen();

                                // The client might not be able to enter full-screen mode
                                opts.fullscreen = window.isFullscreen();
                            }
                        }, (opts) -> opts.fullscreen)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("垂直同步")
                        .setTooltip("启用后，游戏的帧率将与显示器的刷新率同步，从而在牺牲整体输入延迟的情况下获得更流畅的体验。如果设备配置过低，此设置可能会降低性能。")
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VSYNC))
                        .setImpact(OptionImpact.VARIES)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("最大帧率")
                        .setTooltip("限制最大帧率。此选项有限制游戏渲染速度的的效果，因此开启此选项有利于提升电池续航或多任务处理。启用垂直同步时此选项会被自动忽略，除非该值低于显示器的刷新")
                        .setControl(option -> new SliderControl(option, 5, 260, 5, ControlValueFormatter.fpsLimit()))
                        .setBinding((opts, value) -> {
                            opts.maxFps = value;
                            MinecraftClient.getInstance().getWindow().setFramerateLimit(value);
                        }, opts -> opts.maxFps)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("视角摇晃")
                        .setTooltip("启用后，玩家在移动时的视角会摇晃摆动。禁用该选项可缓解晕动症症状。")
                        .setControl(TickBoxControl::new)
                        .setBinding(new VanillaBooleanOptionBinding(Option.VIEW_BOBBING))
                        .build())
                .add(OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                        .setName("攻击指示器")
                        .setTooltip("控制攻击指示器显示在屏幕上的位置。")
                        .setControl(opts -> new CyclingControl<>(opts, AttackIndicator.class, new String[] { "关闭", "十字准星", "工具栏" }))
                        .setBinding((opts, value) -> opts.attackIndicator = value, (opts) -> opts.attackIndicator)
                        .build())
                .build());

        return new OptionPage("通用", ImmutableList.copyOf(groups));
    }

    public static OptionPage quality() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(GraphicsMode.class, vanillaOpts)
                        .setName("图像品质")
                        .setTooltip("默认图像品质控制一些原版选项，且对于Mod兼容性是必要的。若下方的选项保留为“默认”，则将会使用此选项的品质。")
                        .setControl(option -> new CyclingControl<>(option, GraphicsMode.class, new String[] { "流畅", "高品质", "极佳" }))
                        .setBinding(
                                (opts, value) -> opts.graphicsMode = value,
                                opts -> opts.graphicsMode)
                        .setImpact(OptionImpact.HIGH)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("云的品质")
                        .setTooltip("控制云在空中的渲染品质。")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.cloudQuality = value, opts -> opts.quality.cloudQuality)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                        .setName("天气的品质")
                        .setTooltip("控制雨、雪的渲染质量。")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                        .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(ParticlesMode.class, vanillaOpts)
                        .setName("颗粒品质")
                        .setTooltip("控制每次可以在屏幕上出现的颗粒的最大数量。")
                        .setControl(opt -> new CyclingControl<>(opt, ParticlesMode.class, new String[] { "高", "中", "低" }))
                        .setBinding((opts, value) -> opts.particles = value, (opts) -> opts.particles)
                        .setImpact(OptionImpact.MEDIUM)
                        .build())
                .add(OptionImpl.createBuilder(SodiumGameOptions.LightingQuality.class, sodiumOpts)
                        .setName("平滑光照")
                        .setTooltip("控制平滑光照效果的品质。\n\n关 - 无平滑滑光照\n低 - 只有方块应用平滑光照\n高（新！） - 方块和实体都应用平滑光照")
                        .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.LightingQuality.class))
                        .setBinding((opts, value) -> opts.quality.smoothLighting = value, opts -> opts.quality.smoothLighting)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("生物群系过度距离")
                        .setTooltip("控制生物群系之间方块颜色的采样范围。较高的值会极大地增加渲染区块时因质量改进而所花费的时间。")
                        .setControl(option -> new SliderControl(option, 0, 7, 1, ControlValueFormatter.quantityOrDisabled("个方块", "无")))
                        .setBinding((opts, value) -> opts.biomeBlendRadius = value, opts -> opts.biomeBlendRadius)
                        .setImpact(OptionImpact.LOW)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("实体渲染距离")
                        .setTooltip("控制实体的显示距离。较高的值会以牺牲帧率为代价增加渲染距离。")
                        .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                        .setBinding((opts, value) -> opts.entityDistanceScaling = value / 100.0F, opts -> Math.round(opts.entityDistanceScaling * 100.0F))
                        .setImpact(OptionImpact.MEDIUM)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName("实体阴影")
                        .setTooltip("启用后，在生物和其他实体下面渲染简单的阴影。")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.entityShadows = value, opts -> opts.entityShadows)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("晕影")
                        .setTooltip("启用后，屏幕四角处会轻微变暗。除非限制覆盖范围，否则基本不影响帧率。")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());


        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName("Mipmap 级别")
                        .setTooltip("控制平滑材质的多级纹理（Mipmap）的级别。较高的级别可使远处的物体获得更好的渲染效果，但可能会在渲染很多动态材质时产生性能损失。")
                        .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                        .setBinding((opts, value) -> opts.mipmapLevels = value, opts -> opts.mipmapLevels)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                        .build())
                .build());


        return new OptionPage("品质", ImmutableList.copyOf(groups));
    }

    public static OptionPage advanced() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("启用区块多点取样")
                        .setTooltip("多点取样允许以更少的绘制调用渲染多个区块，在渲染世界时大大减低CPU性能压力，同时还可能更有效的GPU利用率。 " + 
                                    "此优化可能会导致某些图形驱动程序出现问题，因此如果你遇到故障，应尝试禁用它。")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useChunkMultidraw = value, opts -> opts.advanced.useChunkMultidraw)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .setImpact(OptionImpact.EXTREME)
                        .setEnabled(MultidrawChunkRenderBackend.isSupported(sodiumOpts.getData().advanced.ignoreDriverBlacklist))
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("启用顶点数组阵对象")
                        .setTooltip("通过将有关如何渲染顶点数据的信息移动到驱动程序中来帮助提高性能，使其能够更好地优化相同对象的重复渲染。 " + 
                                    "除非你使用不兼容的模组，否则通常没有理由禁用此功能。")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useVertexArrayObjects = value, opts -> opts.advanced.useVertexArrayObjects)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .setImpact(OptionImpact.LOW)
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("启用方块表面剔除")
                        .setTooltip("启用后，将只会渲染面向镜头的方块表面。 这可以在渲染过程的早期剔除大量方块表面，从而节省GPU上的内存带宽和时间。 " + 
                                    "某些资源包可能会遇到此选项的问题，因此如果你看到方块显示不全，请尝试禁用它。")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useBlockFaceCulling = value, opts -> opts.advanced.useBlockFaceCulling)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("启用顶点压缩格式")
                        .setTooltip("如果启用，则将使用更紧凑的顶点格式来呈现方块。这可以减少图形内存使用和带宽" +
                                    "尤其是对集成显卡，这些都是影响性能的主要问题，但可能会导致与一些资源包的冲突”+
                             "如何降低位置和纹理坐标属性的精度。")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useCompactVertexFormat = value, opts -> opts.advanced.useCompactVertexFormat)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("启用迷雾遮挡")
                        .setTooltip("启用后，被迷雾效果完全隐藏的区块将不会被渲染，有助于提高性能。 当迷雾效果较重时（例如在水下时），" + 
                                    "改进可能会更加显着，但在某些情况下可能会导致天空和雾之间出现不良的视觉伪影。")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.useFogOcclusion = value, opts -> opts.advanced.useFogOcclusion)
                        .setImpact(OptionImpact.MEDIUM)
                        .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("启用实体剔除")
                        .setTooltip("启用后，则在渲染期间跳过在不可见区块的实体。 这可以通过避免渲染位于地下或墙后的实体来帮助提高性能。")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useEntityCulling = value, opts -> opts.advanced.useEntityCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("启用颗粒剔除")
                        .setTooltip("启用后，将仅渲染可见的颗粒。当周围有许多颗粒时，这可以显著地提高帧率。")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.MEDIUM)
                        .setBinding((opts, value) -> opts.advanced.useParticleCulling = value, opts -> opts.advanced.useParticleCulling)
                        .build()
                )
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("仅渲染可见的动态材质")
                        .setTooltip("启用后，出现特定受顶点限制的情况时（如粒子和文本的渲染）将使用特殊的内部函数来加速客户端内存的复制。此选项仅用于调试目的，除非你知道自己在干什么，否则应保持启用。")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setBinding((opts, value) -> opts.advanced.animateOnlyVisibleTextures = value, opts -> opts.advanced.animateOnlyVisibleTextures)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("允许直接访问内存")
                        .setTooltip("启用后，将允许某些关键代码使用路径直接访问内存来提高性能。 这通常会大大降低区块和实体渲染的CPU性能，但会使诊断某些错误和崩溃变得更加困难。 如果你被要求或以其他方式知道您在做什么，你应该只禁用它。")
                        .setControl(TickBoxControl::new)
                        .setImpact(OptionImpact.HIGH)
                        .setEnabled(UnsafeUtil.isSupported())
                        .setBinding((opts, value) -> opts.advanced.allowDirectMemoryAccess = value, opts -> opts.advanced.allowDirectMemoryAccess)
                        .build()
                )
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, sodiumOpts)
                        .setName("忽略驱动程序黑名单")
                        .setTooltip("If enabled, known incompatibilities with your hardware/driver configuration will be ignored, allowing you to enable options that " +
                                "may cause issues with your game. You should generally not touch this option unless you know exactly what you are doing. After changing " +
                                "this option, you must save, close, and then re-open the settings screen.")
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> opts.advanced.ignoreDriverBlacklist = value, opts -> opts.advanced.ignoreDriverBlacklist)
                        .build()
                )
                .build());

        return new OptionPage("高级", ImmutableList.copyOf(groups));
    }
}
