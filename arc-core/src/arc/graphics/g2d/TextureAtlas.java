package arc.graphics.g2d;

import arc.*;
import arc.Files.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

import java.io.*;

/**
 * Loads images from texture atlases created by TexturePacker.<br>
 * <br>
 * A TextureAtlas must be disposed to free up the resources consumed by the backing textures.
 * <p>
 * 从 TexturePacker 创建的纹理图集加载图像。 <br> <br> 必须释放 TextureAtlas,以回收底层纹理占用的资源。
 * @author Nathan Sweet
 */
public class TextureAtlas implements Disposable{
    private final Ar<AtlasRegion> regions = new Ar<>(false);
    private final ObjectMap<String, Drawable> drawables = new ObjectMap<>();
    private final ObjectMap<String, AtlasRegion> regionMap = new ObjectMap<>();
    private final Ar<AtlasPage> pages = new Ar<>();
    private TextureArray textureArray;
    protected AtlasRegion error, white;
    protected float drawableScale = 1f;

    /**
     * Returns a new texture atlas with only a blank texture region.
     * 返回只含一个空白纹理区域的新纹理图集。
     */
    public static TextureAtlas blankAtlas(){
        TextureAtlas a =  new TextureAtlas();
        a.white = new AtlasRegion(Pixmaps.blankTextureRegion());
        return a;
    }

    /**
     * Creates an empty atlas to which regions can be added.
     * 创建可添加区域的空图集。
     */
    public TextureAtlas(){
    }

    /**
     * Loads the specified pack file using {@link FileType#internal}, using the parent directory of the pack file to find the page
     * images.
     * <p>
     * 使用 {@link FileType#internal} 加载指定的 pack 文件,并以 pack 文件的父目录查找页面图像。
     */
    public TextureAtlas(String internalPackFile){
        this(Core.files.internal(internalPackFile));
    }

    /**
     * Loads the specified pack file, using the parent directory of the pack file to find the page images.
     * 加载指定的 pack 文件,并以 pack 文件的父目录查找页面图像。
     */
    public TextureAtlas(Fi packFile){
        this(packFile, packFile.parent());
    }

    /**
     * @param flip If true, all regions loaded will be flipped for use with a perspective where 0,0 is the upper left corner. 若为 true,加载的所有区域都会翻转,以适配 (0,0) 位于左上角的视角。
     * @see #TextureAtlas(Fi)
     */
    public TextureAtlas(Fi packFile, boolean flip){
        this(packFile, packFile.parent(), flip);
    }

    public TextureAtlas(Fi packFile, Fi imagesDir){
        this(packFile, imagesDir, false);
    }

    /**
     * @param flip If true, all regions loaded will be flipped for use with a perspective where 0,0 is the upper left corner.
     * @param flip If true, all regions loaded will be flipped for use with a perspective where 0,0 is the upper left corner. 若为 true,加载的所有区域都会翻转,以适配 (0,0) 位于左上角的视角。
     */
    public TextureAtlas(Fi packFile, Fi imagesDir, boolean flip){
        this(new TextureAtlasData(packFile, imagesDir, flip));
    }

    /**
     * @param data May be null.
     * @param data May be null. 可为 null。
     */
    public TextureAtlas(TextureAtlasData data){
        if(data != null) load(data);
    }

    private void load(TextureAtlasData data){
        this.pages.set(data.pages);
        AtlasPage first = data.pages.first();
        if(data.texture != null){
            textureArray = data.texture;
        }else{
            textureArray = new TextureArray(data.pages.map(p -> p.textureFile).toArray(Fi.class), first.useMipMaps);
        }

        textureArray.setWrap(first.uWrap, first.vWrap);
        textureArray.setFilter(first.minFilter, first.magFilter);

        int i = 0;
        for(AtlasPage page : data.pages){
            page.texture = new ArraySliceTexture(textureArray, i ++);
        }

        for(Region region : data.regions){
            int width = region.width;
            int height = region.height;
            AtlasRegion atlasRegion = new AtlasRegion(region.page.texture, region.left, region.top,
            region.rotate ? height : width, region.rotate ? width : height);
            atlasRegion.name = region.name;
            atlasRegion.offsetX = region.offsetX;
            atlasRegion.offsetY = region.offsetY;
            atlasRegion.originalHeight = region.originalHeight;
            atlasRegion.originalWidth = region.originalWidth;
            atlasRegion.rotate = region.rotate;
            atlasRegion.splits = region.splits;
            atlasRegion.pads = region.pads;
            if(region.flip) atlasRegion.flip(false, true);
            regions.add(atlasRegion);
            regionMap.put(atlasRegion.name, atlasRegion);
        }

        error = find("error");
    }

    public TextureArray getTexture(){
        return textureArray;
    }

    public void setTexture(TextureArray textureArray){
        this.textureArray = textureArray;
    }

    public Ar<AtlasPage> getPages(){
        return pages;
    }

    public void setDrawableScale(float scale){
        this.drawableScale = scale;
    }

    /**
     * Returns all regions in the atlas.
     * 返回图集中的所有区域。
     */
    public Ar<AtlasRegion> getRegions(){
        return regions;
    }

    /**
     * Returns the region map in the atlas.
     * 返回图集中的区域映射。
     */
    public ObjectMap<String, AtlasRegion> getRegionMap(){
        return regionMap;
    }

    /**
     * Returns the blank 1x1 texture region, if it exists.
     * 返回空白的 1x1 纹理区域(若存在)。
     */
    public AtlasRegion white(){
        if(white == null){
            white = find("white");
        }
        return white;
    }

    /**
     * Finds and sets error region as name.
     * 查找错误区域并将其设为该名称。
     */
    public boolean setErrorRegion(String name) {
        if(error != null || !has(name)) return false;
        error = find(name);
        return true;
    }

    public boolean isFound(TextureRegion region){
        return region != error;
    }

    /**
     * Returns the first region found with the specified name. This method's performance is no longer garbage.
     * <p>
     * 返回按指定名称找到的第一个区域。此方法的性能已不再是垃圾。
     * @return The region, or the error region (if it is defined), or null. 该区域,或错误区域(若已定义),或 null。
     */
    public AtlasRegion find(String name){
        AtlasRegion r = regionMap.get(name, error);
        if(r == null && !name.equals("error"))
            throw new IllegalArgumentException("The region \"" + name + "\" does not exist!");
        return r;
    }

    public TextureRegion find(String name, String def){
        return find(name, find(def));
    }

    public TextureRegion find(String name, TextureRegion def){
        TextureRegion region = regionMap.get(name);
        return region == null || region == error ? def : region;
    }

    public boolean has(String s){
        return regionMap.containsKey(s);
    }

    @SuppressWarnings("unchecked")
    public <T extends Drawable> T getDrawable(String name){
        return (T)drawable(name);
    }

    /** Creates and caches a new drawable by name.
     * <p>
     * 按名称创建并缓存新的 drawable。若找不到,返回 'error' 纹理区域 drawable。
     * If nothing is found, returns an 'error' texture region drawable. */
    public Drawable drawable(String name){
        if(drawables.containsKey(name)){
            return drawables.get(name);
        }

        Drawable out = null;

        if(has(name)){
            AtlasRegion region = find(name);

            if(region.splits != null){
                int[] splits = region.splits;
                NinePatch patch = new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
                int[] pads = region.pads;
                if(pads != null) patch.setPadding(pads[0], pads[1], pads[2], pads[3]);
                out = new ScaledNinePatchDrawable(patch, drawableScale);
            }else{
                out = new TextureRegionDrawable(region, drawableScale);
            }
        }

        if(error == null && out == null) throw new IllegalArgumentException("No drawable '" + name + "' found.");
        if(out == null) out = new TextureRegionDrawable(error);
        drawables.put(name, out);

        return out;
    }

    public ObjectMap<String, Drawable> getDrawables(){
        return drawables;
    }

    /**
     * Releases all resources associated with this TextureAtlas instance. This releases all the textures backing all TextureRegions
     * and Sprites, which should no longer be used after calling dispose.
     * <p>
     * 释放与此 TextureAtlas 实例关联的所有资源。这会释放所有 TextureRegion 和 Sprite 的底层纹理,调用 dispose 后它们不应再被使用。
     */
    @Override
    public void dispose(){
        if(textureArray != null) textureArray.dispose();
        textureArray = null;
    }

    public static class TextureAtlasData{
        public static final byte formatVersion = 0;
        public static final byte[] formatHeader = new byte[]{'A', 'A', 'T', 'L', 'S'};

        public @Nullable TextureArray texture;
        public final Ar<AtlasPage> pages = new Ar<>();
        public final Ar<Region> regions = new Ar<>();

        public TextureAtlasData(Fi packFile, Fi imagesDir, boolean flip){
            try(Reads read = packFile.reads()){
                for(byte b : formatHeader){
                    if(read.b() != b){
                        throw new IOException("Invalid binary header. Have you re-packed sprites?");
                    }
                }
                //discard version
                // 丢弃版本行
                read.b();

                while(read.checkEOF() != -1){
                    String image = read.str();
                    Fi file = imagesDir.child(image);

                    short pageWidth = read.s(), pageHeight = read.s();

                    TextureFilter min = TextureFilter.all[read.b()], mag = TextureFilter.all[read.b()];
                    TextureWrap wrapX = TextureWrap.all[read.b()], wrapY = TextureWrap.all[read.b()];

                    int rects = read.i();

                    AtlasPage page = new AtlasPage(file, pageWidth, pageHeight, min.isMipMap(), min, mag, wrapX, wrapY);
                    pages.add(page);

                    for(int j = 0; j < rects; j++){
                        Region region = new Region();
                        region.flip = flip;
                        region.page = page;
                        region.name = read.str();
                        region.left = read.s();
                        region.top = read.s();
                        region.width = read.s();
                        region.height = read.s();

                        //offsets
                        // 偏移量
                        if(read.bool()){
                            region.offsetX = read.s();
                            region.offsetY = read.s();
                            region.originalWidth = read.s();
                            region.originalHeight = read.s();
                        }

                        //splits
                        // 分割信息
                        if(read.bool()){
                            region.splits = new int[]{read.s(), read.s(), read.s(), read.s()};
                        }

                        //pads
                        // 内边距信息
                        if(read.bool()){
                            region.pads = new int[]{read.s(), read.s(), read.s(), read.s()};
                        }

                        regions.add(region);
                    }
                }
            }catch(Exception e){
                throw new ArcRuntimeException("Error reading pack file: " + packFile, e);
            }
        }

        public static class AtlasPage{
            public final Fi textureFile;
            public final int width, height;
            public final boolean useMipMaps;
            public final TextureFilter minFilter;
            public final TextureFilter magFilter;
            public final TextureWrap uWrap;
            public final TextureWrap vWrap;
            Texture texture;

            public AtlasPage(Fi handle, int width, int height, boolean useMipMaps, TextureFilter minFilter,
                             TextureFilter magFilter, TextureWrap uWrap, TextureWrap vWrap){
                this.width = width;
                this.height = height;
                this.textureFile = handle;
                this.useMipMaps = useMipMaps;
                this.minFilter = minFilter;
                this.magFilter = magFilter;
                this.uWrap = uWrap;
                this.vWrap = vWrap;
            }
        }

        public static class Region{
            public AtlasPage page;
            public String name;
            public float offsetX;
            public float offsetY;
            public int originalWidth;
            public int originalHeight;
            public boolean rotate;
            public int left;
            public int top;
            public int width;
            public int height;
            public boolean flip;
            public int[] splits;
            public int[] pads;
        }
    }

    /**
     * Describes the region of a packed image and provides information about the original image before it was packed.
     * 描述打包图像的区域,并提供打包前原图像的信息。
     */
    public static class AtlasRegion extends TextureRegion{

        /**
         * The name of the original image file, up to the first underscore. Underscores denote special instructions to the texture
         * packer.
         * <p>
         * 原始图像文件名(至第一个下划线为止)。下划线表示给纹理打包器的特殊指令。
         */
        public String name;

        /**
         * The offset from the left of the original image to the left of the packed image, after whitespace was removed for packing.
         * 打包去除空白后,从原图像左侧到打包图像左侧的偏移。
         */
        public float offsetX;

        /**
         * The offset from the bottom of the original image to the bottom of the packed image, after whitespace was removed for
         * packing.
         * <p>
         * 打包去除空白后,从原图像底部到打包图像底部的偏移。
         */
        public float offsetY;

        /**
         * The width of the image, after whitespace was removed for packing.
         * 打包去除空白后的图像宽度。
         */
        public int packedWidth;

        /**
         * The height of the image, after whitespace was removed for packing.
         * 打包去除空白后的图像高度。
         */
        public int packedHeight;

        /**
         * The width of the image, before whitespace was removed and rotation was applied for packing.
         * 打包去除空白和旋转之前的图像宽度。
         */
        public int originalWidth;

        /**
         * The height of the image, before whitespace was removed for packing.
         * 打包去除空白之前的图像高度。
         */
        public int originalHeight;

        /**
         * If true, the region has been rotated 90 degrees counter clockwise.
         * 若为 true,该区域已逆时针旋转 90 度。
         */
        public boolean rotate;

        /**
         * The ninepatch splits, or null if not a ninepatch. Has 4 elements: left, right, top, bottom.
         * 九宫格分割值;若非九宫格则为 null。含 4 个元素:left、right、top、bottom。
         */
        public int[] splits;

        /**
         * The ninepatch pads, or null if not a ninepatch or the has no padding. Has 4 elements: left, right, top, bottom.
         * 九宫格内边距;若非九宫格或无内边距则为 null。含 4 个元素:left、right、top、bottom。
         */
        public int[] pads;

        public AtlasRegion(Texture texture, int x, int y, int width, int height){
            super(texture, x, y, width, height);
            originalWidth = width;
            originalHeight = height;
            packedWidth = width;
            packedHeight = height;
        }

        public AtlasRegion(){

        }

        public AtlasRegion(AtlasRegion region){
            set(region);
            name = region.name;
            offsetX = region.offsetX;
            offsetY = region.offsetY;
            packedWidth = region.packedWidth;
            packedHeight = region.packedHeight;
            originalWidth = region.originalWidth;
            originalHeight = region.originalHeight;
            rotate = region.rotate;
            splits = region.splits;
        }

        public AtlasRegion(TextureRegion region){
            set(region);
            name = "unknown";
        }

        /**
         * Flips the region, adjusting the offset so the image appears to be flip as if no whitespace has been removed for packing.
         * 翻转区域并调整偏移,使图像看起来如同未去除空白就翻转一样。
         */
        @Override
        public void flip(boolean x, boolean y){
            super.flip(x, y);
            if(x) offsetX = originalWidth - offsetX - getRotatedPackedWidth();
            if(y) offsetY = originalHeight - offsetY - getRotatedPackedHeight();
        }

        /**
         * Returns the packed width considering the rotate value, if it is true then it returns the packedHeight, otherwise it
         * returns the packedWidth.
         * <p>
         * 考虑 rotate 值返回打包宽度:若为 true 返回 packedHeight,否则返回 packedWidth。
         */
        public float getRotatedPackedWidth(){
            return rotate ? packedHeight : packedWidth;
        }

        /**
         * Returns the packed height considering the rotate value, if it is true then it returns the packedWidth, otherwise it
         * returns the packedHeight.
         * <p>
         * 考虑 rotate 值返回打包高度:若为 true 返回 packedWidth,否则返回 packedHeight。
         */
        public float getRotatedPackedHeight(){
            return rotate ? packedWidth : packedHeight;
        }

        public String toString(){
            return name;
        }
    }
}
