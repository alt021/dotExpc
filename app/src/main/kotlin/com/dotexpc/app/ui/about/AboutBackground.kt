// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// Adapted from the compose-miuix-ui example (AboutPage.kt, Apache 2.0).

package com.dotexpc.app.ui.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.shader.RuntimeShader
import top.yukonga.miuix.kmp.shader.asBrush
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * 关于页背景：移植 compose-miuix-ui 示例（AboutPage.kt，Apache 2.0）的 OS3 动态渐变。
 * 四个色点在四角缓慢漂移，颜色每隔几秒在三组预设之间过渡；
 * 没有运行时着色器（API < 33）时退回纯表面色。
 */
@Composable
internal fun DynamicGradientBackground(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val record = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier

    if (!remember { isRuntimeShaderSupported() }) {
        Box(modifier = modifier) {
            Box(
                Modifier
                    .matchParentSize()
                    .then(record)
                    .background(colors.surface),
            )
            content()
        }
        return
    }

    val preset = remember(isDark) { if (isDark) OS3_PHONE_DARK else OS3_PHONE_LIGHT }
    val painter = remember { GradientPainter() }
    val colorStage = remember { Animatable(0f) }

    LaunchedEffect(preset) {
        var target = floor(colorStage.value) + 1f
        while (isActive) {
            delay((preset.colorInterpPeriod * 500).toLong())
            colorStage.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 22f))
            target += 1f
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(record)
                .gradientBackground(
                    painter = painter,
                    preset = preset,
                    isDark = isDark,
                    surface = colors.surface,
                    colorStage = { colorStage.value },
                ),
        )
        content()
    }
}

/** 色点漂移与噪点的整体速度；配合加长的颜色过渡周期，让背景变化更缓。 */
private const val TIME_SCALE = 0.4f

private data class GradientPreset(
    val points: FloatArray,
    val colors1: FloatArray,
    val colors2: FloatArray,
    val colors3: FloatArray,
    val colorInterpPeriod: Float,
    val lightOffset: Float,
    val saturateOffset: Float,
    val pointOffset: Float,
)

private val OS3_PHONE_LIGHT = GradientPreset(
    points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
    colors1 = floatArrayOf(0.62f, 0.80f, 1.0f, 1.0f, 0.52f, 0.72f, 0.99f, 1.0f, 0.70f, 0.85f, 1.0f, 1.0f, 0.46f, 0.66f, 0.97f, 1.0f),
    colors2 = floatArrayOf(0.48f, 0.69f, 0.98f, 1.0f, 0.66f, 0.83f, 1.0f, 1.0f, 0.40f, 0.60f, 0.94f, 1.0f, 0.58f, 0.76f, 1.0f, 1.0f),
    colors3 = floatArrayOf(0.72f, 0.87f, 1.0f, 1.0f, 0.55f, 0.74f, 0.99f, 1.0f, 0.60f, 0.80f, 1.0f, 1.0f, 0.42f, 0.62f, 0.95f, 1.0f),
    colorInterpPeriod = 18.0f,
    lightOffset = 0.1f,
    saturateOffset = 0.2f,
    pointOffset = 0.2f,
)

private val OS3_PHONE_DARK = GradientPreset(
    points = floatArrayOf(0.8f, 0.2f, 1.0f, 0.8f, 0.9f, 1.0f, 0.2f, 0.9f, 1.0f, 0.2f, 0.2f, 1.0f),
    colors1 = floatArrayOf(0.10f, 0.24f, 0.62f, 0.45f, 0.06f, 0.16f, 0.50f, 0.5f, 0.16f, 0.30f, 0.72f, 0.45f, 0.05f, 0.12f, 0.42f, 0.5f),
    colors2 = floatArrayOf(0.06f, 0.18f, 0.55f, 0.5f, 0.14f, 0.28f, 0.68f, 0.45f, 0.04f, 0.12f, 0.46f, 0.5f, 0.18f, 0.33f, 0.75f, 0.45f),
    colors3 = floatArrayOf(0.12f, 0.26f, 0.66f, 0.45f, 0.08f, 0.20f, 0.58f, 0.5f, 0.20f, 0.36f, 0.78f, 0.4f, 0.05f, 0.15f, 0.48f, 0.5f),
    colorInterpPeriod = 26.0f,
    lightOffset = 0.0f,
    saturateOffset = 0.17f,
    pointOffset = 0.4f,
)

private fun Modifier.gradientBackground(
    painter: GradientPainter,
    preset: GradientPreset,
    isDark: Boolean,
    surface: Color,
    colorStage: () -> Float,
): Modifier = this then GradientElement(painter, preset, isDark, surface, colorStage)

private data class GradientElement(
    val painter: GradientPainter,
    val preset: GradientPreset,
    val isDark: Boolean,
    val surface: Color,
    val colorStage: () -> Float,
) : ModifierNodeElement<GradientNode>() {

    override fun create(): GradientNode = GradientNode(painter, preset, isDark, surface, colorStage)

    override fun update(node: GradientNode) {
        node.update(painter, preset, isDark, surface, colorStage)
    }
}

/**
 * 按 60fps 推进着色器时间，只让绘制失效，不触发重组。
 */
private class GradientNode(
    private var painter: GradientPainter,
    private var preset: GradientPreset,
    private var isDark: Boolean,
    private var surface: Color,
    private var colorStage: () -> Float,
) : Modifier.Node(), DrawModifierNode {

    private var animationJob: Job? = null
    private var animTime = 0f
    private var startOffset = 0f

    override fun onAttach() {
        startAnimation()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
    }

    fun update(
        painter: GradientPainter,
        preset: GradientPreset,
        isDark: Boolean,
        surface: Color,
        colorStage: () -> Float,
    ) {
        this.painter = painter
        this.preset = preset
        this.isDark = isDark
        this.surface = surface
        this.colorStage = colorStage
        invalidateDraw()
    }

    private fun startAnimation() {
        animationJob?.cancel()
        startOffset = animTime
        animationJob = coroutineScope.launch {
            val minDeltaNanos = 1_000_000_000L / 60L
            val origin = withFrameNanos { it }
            var lastEmit = origin
            while (isActive) {
                val now = withFrameNanos { it }
                if (now - lastEmit < minDeltaNanos) continue
                lastEmit = now
                animTime = startOffset + (now - origin) / 1_000_000_000f
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawRect(surface)
        painter.updateResolution(size.width, size.height)
        painter.updateBound(size.width, size.height)
        painter.applyPresetIfNeeded(isDark)
        painter.updateColors(preset, colorStage())
        painter.updateAnimTime(animTime * TIME_SCALE)
        painter.updatePointsAnim(animTime * TIME_SCALE, preset)
        drawRect(painter.brush)
    }
}

private class GradientPainter {
    private val shader: RuntimeShader = RuntimeShader(OS3_BG_FRAG).also {
        it.setFloatUniform("uTranslateY", 0f)
        it.setFloatUniform("uNoiseScale", 1.5f)
        it.setFloatUniform("uPointRadiusMulti", 1f)
        it.setFloatUniform("uAlphaMulti", 1f)
    }

    val brush: Brush get() = shader.asBrush()

    private val resolution = FloatArray(2)
    private val bound = FloatArray(4)
    private val colorsBuffer = FloatArray(16)
    private val pointsAnimBuffer = FloatArray(8)

    private var animTime = Float.NaN
    private var isDarkCached: Boolean? = null
    private var cachedColorStage = Float.NaN
    private var cachedColorsPreset: GradientPreset? = null
    private var cachedPointsAnimTime = Float.NaN
    private var cachedPointsAnimPreset: GradientPreset? = null
    private var cachedWidth = Float.NaN
    private var cachedHeight = Float.NaN

    fun updateResolution(width: Float, height: Float) {
        if (resolution[0] == width && resolution[1] == height) return
        resolution[0] = width
        resolution[1] = height
        shader.setFloatUniform("uResolution", resolution)
    }

    fun updateAnimTime(time: Float) {
        if (animTime == time) return
        animTime = time
        shader.setFloatUniform("uAnimTime", time)
    }

    fun updatePointsAnim(time: Float, preset: GradientPreset) {
        if (cachedPointsAnimTime == time && cachedPointsAnimPreset === preset) return

        val offset = preset.pointOffset
        for (i in 0 until 4) {
            val srcX = preset.points[i * 3]
            val srcY = preset.points[i * 3 + 1]
            val animX = srcX + sin(time + srcY) * offset
            val animY = srcY + cos(time + animX) * offset
            pointsAnimBuffer[i * 2] = animX
            pointsAnimBuffer[i * 2 + 1] = animY
        }
        shader.setFloatUniform("uPointsAnim", pointsAnimBuffer)

        cachedPointsAnimTime = time
        cachedPointsAnimPreset = preset
    }

    fun updateColors(preset: GradientPreset, stage: Float) {
        if (cachedColorsPreset === preset && cachedColorStage == stage) return

        val base = stage.toInt()
        val fraction = stage - base
        val start = colorsForCycleIndex(preset, base)
        val end = colorsForCycleIndex(preset, base + 1)
        for (i in 0 until 16) {
            colorsBuffer[i] = start[i] + (end[i] - start[i]) * fraction
        }
        shader.setFloatUniform("uColors", colorsBuffer)

        cachedColorsPreset = preset
        cachedColorStage = stage
    }

    private fun colorsForCycleIndex(preset: GradientPreset, index: Int): FloatArray = when (index.mod(4)) {
        1 -> preset.colors1
        3 -> preset.colors3
        else -> preset.colors2
    }

    fun updateBound(width: Float, height: Float) {
        if (cachedWidth == width && cachedHeight == height) return

        // 与 Miuix 示例一致：色点铺在下方 80%，顶部由边缘颜色延伸。
        val heightRatio = 0.8f
        if (width <= height) {
            bound[0] = 0f
            bound[1] = 1f - heightRatio
            bound[2] = 1f
            bound[3] = heightRatio
        } else {
            val aspectRatio = width / height
            val contentCenterY = 1f - heightRatio / 2f
            bound[0] = 0f
            bound[1] = contentCenterY - aspectRatio / 2f
            bound[2] = 1f
            bound[3] = aspectRatio
        }
        shader.setFloatUniform("uBound", bound)

        cachedWidth = width
        cachedHeight = height
    }

    fun applyPresetIfNeeded(isDark: Boolean) {
        if (isDarkCached == isDark) return

        val preset = if (isDark) OS3_PHONE_DARK else OS3_PHONE_LIGHT
        shader.setFloatUniform("uPoints", preset.points)
        shader.setFloatUniform("uLightOffset", preset.lightOffset)
        shader.setFloatUniform("uSaturateOffset", preset.saturateOffset)

        isDarkCached = isDark
    }
}

private const val OS3_BG_FRAG = """
    uniform vec2 uResolution;
    uniform float uAnimTime;
    uniform vec4 uBound;
    uniform float uTranslateY;
    uniform vec3 uPoints[4];
    uniform vec2 uPointsAnim[4];
    uniform vec4 uColors[4];
    uniform float uAlphaMulti;
    uniform float uNoiseScale;
    uniform float uPointRadiusMulti;
    uniform float uSaturateOffset;
    uniform float uLightOffset;

    vec3 rgb2hsv(vec3 c) {
        vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
        vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
        vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
        float d = q.x - min(q.w, q.y);
        float e = 1.0e-10;
        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
    }

    vec3 hsv2rgb(vec3 c) {
        vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
        vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
    }

    float hash(vec2 p) {
        vec3 p3 = fract(vec3(p.xyx) * 0.13);
        p3 += dot(p3, p3.yzx + 3.333);
        return fract((p3.x + p3.y) * p3.z);
    }

    float perlin(vec2 x) {
        vec2 i = floor(x); vec2 f = fract(x);

        float a = hash(i); float b = hash(i + vec2(1.0, 0.0));
        float c = hash(i + vec2(0.0, 1.0)); float d = hash(i + vec2(1.0, 1.0));

        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
    }

    float gradientNoise(in vec2 uv) {
        return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
    }

    vec4 main(vec2 fragCoord){
        vec2 vUv = fragCoord/uResolution;
        vUv.y = 1.0-vUv.y;
        vec2 uv = vUv;
        uv -= vec2(0., uTranslateY);
        uv.xy -= uBound.xy;
        uv.xy /= uBound.zw;

        vec4 color = vec4(0.0);
        float noiseValue = perlin(vUv * uNoiseScale + vec2(-uAnimTime, -uAnimTime));

        for (int i = 0; i < 4; i++){
            vec4 pointColor = uColors[i];
            pointColor.rgb *= pointColor.a;
            vec2 point = uPointsAnim[i];
            float rad = uPoints[i].z * uPointRadiusMulti;

            float d = distance(uv, point);
            float pct = smoothstep(rad, 0., d);
            color.rgb = mix(color.rgb, pointColor.rgb, pct);
            color.a = mix(color.a, pointColor.a, pct);
        }

        float oppositeNoise = smoothstep(0., 1., noiseValue);
        color.rgb /= color.a;
        vec3 hsv = rgb2hsv(color.rgb);
        hsv.y = mix(hsv.y, 0.0, oppositeNoise * uSaturateOffset);
        color.rgb = hsv2rgb(hsv);
        color.rgb += oppositeNoise * uLightOffset;

        color.a = clamp(color.a, 0., 1.);
        color.a *= uAlphaMulti;

        color += (10.0 / 255.0) * gradientNoise(fragCoord.xy) - (5.0 / 255.0);
        return vec4(color.rgb * color.a, color.a);
    }
"""
