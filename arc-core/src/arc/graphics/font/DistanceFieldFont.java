/*
 * Copyright (c) 2015, Florian Falkner
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of Matthias Mann nor
 * the names of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package arc.graphics.font;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;

/**
 * Renders bitmap fonts using distance field textures, see the <a
 * href="https://github.com/libgdx/libgdx/wiki/Distance-field-fonts">Distance Field Fonts wiki article</a> for usage. Initialize
 * the SpriteBatch with the {@link #createDistanceFieldShader()} shader.
 * <p>
 * Attention: The batch is flushed before and after each string is rendered.
 * <p>
 * 使用距离场纹理渲染位图字体,用法参见 <a href="https://github.com/libgdx/libgdx/wiki/Distance-field-fonts">Distance Field Fonts wiki 文章</a>。请使用 {@link #createDistanceFieldShader()} 着色器初始化 SpriteBatch。 <p> 注意:每渲染一个字符串前后都会刷新批处理。
 * @author Florian Falkner
 */
public class DistanceFieldFont extends Font{
    private float distanceFieldSmoothing;

    public DistanceFieldFont(FontData data, Ar<TextureRegion> pageRegions, boolean integer){
        super(data, pageRegions, integer);
    }

    public DistanceFieldFont(FontData data, TextureRegion region, boolean integer){
        super(data, region, integer);
    }

    public DistanceFieldFont(Fi fontFile, boolean flip){
        super(fontFile, flip);
    }

    public DistanceFieldFont(Fi fontFile, Fi imageFile, boolean flip, boolean integer){
        super(fontFile, imageFile, flip, integer);
    }

    public DistanceFieldFont(Fi fontFile, Fi imageFile, boolean flip){
        super(fontFile, imageFile, flip);
    }

    public DistanceFieldFont(Fi fontFile, TextureRegion region, boolean flip){
        super(fontFile, region, flip);
    }

    public DistanceFieldFont(Fi fontFile, TextureRegion region){
        super(fontFile, region);
    }

    public DistanceFieldFont(Fi fontFile){
        super(fontFile);
    }

    /**
     * Returns a new instance of the distance field shader, see https://github.com/libgdx/libgdx/wiki/Distance-field-fonts if the
     * u_smoothing uniform > 0.0. Otherwise the same code as the default SpriteBatch shader is used.
     * <p>
     * 返回距离场着色器的新实例,当 u_smoothing uniform > 0.0 时参见 https://github.com/libgdx/libgdx/wiki/Distance-field-fonts。否则使用与默认 SpriteBatch 着色器相同的代码。
     */
    public static Shader createDistanceFieldShader(){
        String vertexShader =
          "attribute vec4 " + Shader.positionAttribute + ";\n" //
        + "attribute vec4 " + Shader.colorAttribute + ";\n" //
        + "attribute vec2 " + Shader.texcoordAttribute + "0;\n" //
        + "uniform mat4 u_projTrans;\n" //
        + "varying vec4 v_color;\n" //
        + "varying vec2 v_texCoords;\n" //
        + "\n" //
        + "void main(){\n" //
        + "	v_color = " + Shader.colorAttribute + ";\n" //
        + "	v_color.a = v_color.a * (255.0/254.0);\n" //
        + "	v_texCoords = " + Shader.texcoordAttribute + "0;\n" //
        + "	gl_Position =  u_projTrans * " + Shader.positionAttribute + ";\n" //
        + "}\n";

        String fragmentShader =
          "uniform sampler2D u_texture;\n" //
        + "uniform float u_smoothing;\n" //
        + "varying vec4 v_color;\n" //
        + "varying vec2 v_texCoords;\n" //
        + "\n" //
        + "void main(){\n" //
        + "	if (u_smoothing > 0.0) {\n" //
        + "		float smoothing = 0.25 / u_smoothing;\n" //
        + "		float distance = texture2D(u_texture, v_texCoords).a;\n" //
        + "		float alpha = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);\n" //
        + "		gl_FragColor = vec4(v_color.rgb, alpha * v_color.a);\n" //
        + "	} else {\n" //
        + "		gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" //
        + "	}\n" //
        + "}\n";
        return new Shader(vertexShader, fragmentShader);
    }

    protected void load(FontData data){
        super.load(data);

        // Distance field font rendering requires font texture to be filtered Linear.
        // 距离场字体渲染要求字体纹理使用线性(Linear)过滤。
        final Ar<TextureRegion> regions = getRegions();
        for(TextureRegion region : regions)
            region.texture.setFilter(TextureFilter.linear, TextureFilter.linear);
    }

    @Override
    public FontCache newFontCache(){
        return new DistanceFieldFontCache(this, integer);
    }

    /**
     * @return The distance field smoothing factor for this font.
     * @return The distance field smoothing factor for this font. 该字体的距离场平滑系数。
     */
    public float getDistanceFieldSmoothing(){
        return distanceFieldSmoothing;
    }

    /**
     * @param distanceFieldSmoothing Set the distance field smoothing factor for this font. SpriteBatch needs to have this shader
     * set for rendering distance field fonts. 设置该字体的距离场平滑系数。渲染距离场字体时 SpriteBatch 需要设置此着色器。
     */
    public void setDistanceFieldSmoothing(float distanceFieldSmoothing){
        this.distanceFieldSmoothing = distanceFieldSmoothing;
    }

    /**
     * Provides a font cache that uses distance field shader for rendering fonts. Attention: breaks batching because uniform is
     * needed for smoothing factor, so a flush is performed before and after every font rendering.
     * <p>
     * 提供使用距离场着色器渲染字体的字体缓存。注意:由于平滑系数需要 uniform,会破坏批处理,因此每次字体渲染前后都会执行刷新。
     * @author Florian Falkner
     */
    private static class DistanceFieldFontCache extends FontCache{
        public DistanceFieldFontCache(DistanceFieldFont font){
            super(font, font.usesIntegerPositions());
        }

        public DistanceFieldFontCache(DistanceFieldFont font, boolean integer){
            super(font, integer);
        }

        private float getSmoothingFactor(){
            final DistanceFieldFont font = (DistanceFieldFont)super.getFont();
            return font.getDistanceFieldSmoothing() * font.getScaleX();
        }

        private void setSmoothingUniform(float smoothing){
            Draw.flush();
            Draw.getShader().setUniformf("u_smoothing", smoothing);
        }

        @Override
        public void draw(){
            setSmoothingUniform(getSmoothingFactor());
            super.draw();
            setSmoothingUniform(0);
        }

        @Override
        public void draw(int start, int end){
            setSmoothingUniform(getSmoothingFactor());
            super.draw(start, end);
            setSmoothingUniform(0);
        }
    }
}
