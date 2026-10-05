package arc.graphics;

import arc.graphics.gl.*;

public enum TextureFilter{
    /**
     * Fetch the nearest texel that best maps to the pixel on screen.
     * 获取与屏幕上像素最匹配的最近纹素。
     */
    nearest(Gl.nearest),

    /**
     * Fetch four nearest texels that best maps to the pixel on screen.
     * 获取与屏幕上像素最匹配的四个最近纹素。
     */
    linear(Gl.linear),

    /** @see TextureFilter#mipMapLinearLinear */
    mipMap(Gl.linearMipmapLinear),

    /**
     * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
     * nearest filter.
     * <p>
     * 根据像素/纹素比率从 mip map 链中获取最合适的图像,然后用最近邻过滤采样纹素。
     */
    mipMapNearestNearest(Gl.nearestMipmapNearest),

    /**
     * Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a
     * Linear filter.
     * <p>
     * 根据像素/纹素比率从 mip map 链中获取最合适的图像,然后用线性过滤采样纹素。
     */
    mipMapLinearNearest(Gl.linearMipmapNearest),

    /**
     * Fetch the two best fitting images from the mip map chain and then sample the nearest texel from each of the two images,
     * combining them to the final output pixel.
     * <p>
     * 从 mip map 链中获取两张最合适的图像,分别采样最近纹素,再合成为最终输出像素。
     */
    mipMapNearestLinear(Gl.nearestMipmapLinear),

    /**
     * Fetch the two best fitting images from the mip map chain and then sample the four nearest texels from each of the two
     * images, combining them to the final output pixel.
     * <p>
     * 从 mip map 链中获取两张最合适的图像,分别采样四个最近纹素,再合成为最终输出像素。
     */
    mipMapLinearLinear(Gl.linearMipmapLinear);

    public static final TextureFilter[] all = values();

    public final int glEnum;

    TextureFilter(int glEnum){
        this.glEnum = glEnum;
    }

    public boolean isMipMap(){
        return glEnum != Gl.nearest && glEnum != Gl.linear;
    }
}
