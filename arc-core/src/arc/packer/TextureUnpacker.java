package arc.packer;

import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData.*;
import arc.struct.*;

import java.io.*;
import java.util.concurrent.*;

/**
 * Unpacks a texture atlas into individual image files.
 * <p>
 * 将纹理图集解包为单独的图片文件。
 * @author Geert Konijnendijk
 * @author Nathan Sweet
 * @author Michael Bazos
 */
public class TextureUnpacker{
    private static final String DEFAULT_OUTPUT_PATH = "output";
    private static final int NINEPATCH_PADDING = 1;
    private static final String HELP = "Usage: atlasFile [imageDir] [outputDir]";
    private static final String ATLAS_FILE_EXTENSION = ".aatls";

    /**
     * Checks the command line arguments for correctness.
     * <p>
     * 检查命令行参数是否正确。
     * @return 0 If arguments are invalid, Number of arguments otherwise. 如果参数无效则为 0,否则为参数个数
     */
    private int parseArguments(String[] args){
        int numArgs = args.length;
        // check if number of args is right
        // 检查参数个数是否正确
        if(numArgs < 1) return 0;
        // check if the input file's extension is right
        // 检查输入文件的扩展名是否正确
        boolean extension = args[0].endsWith(ATLAS_FILE_EXTENSION);
        // check if the directory names are valid
        // 检查目录名称是否有效
        boolean directory = true;
        if(numArgs >= 2) directory = checkDirectoryValidity(args[1]);
        if(numArgs == 3) directory &= checkDirectoryValidity(args[2]);
        return extension && directory ? numArgs : 0;
    }

    private boolean checkDirectoryValidity(String directory){
        File checkFile = new File(directory);
        boolean path = true;
        // try to get the canonical path, if this fails the path is not valid
        // 尝试获取规范路径,如果失败则说明路径无效
        try{
            checkFile.getCanonicalPath();
        }catch(Exception e){
            path = false;
        }
        return path;
    }

    /**
     * Splits an atlas into seperate image and ninepatch files.
     * 将图集拆分为单独的图片和 ninepatch 文件。
     */
    public void splitAtlas(TextureAtlasData atlas, String outputDir, boolean quiet){
        // create the output directory if it did not exist yet
        // 如果输出目录尚不存在,则创建它
        File outputDirFile = new File(outputDir);
        if(!outputDirFile.exists()){
            outputDirFile.mkdirs();
            if(!quiet) System.out.printf("Creating directory: %s%n", outputDirFile.getPath());
        }

        for(AtlasPage page : atlas.pages){
            // load the image file belonging to this page as a Buffered Image
            // 将此页面对应的图片文件加载为 Buffered Image
            File file = page.textureFile.file();
            if(!file.exists()) throw new RuntimeException("Unable to find atlas image: " + file.getAbsolutePath());
            Pixmap img = new Pixmap(new Fi(file));

            // regions are independent of each other, so they are extracted and written in parallel
            // 各区域相互独立,因此可以并行提取和写入
            Ar<FutureTask<Void>> tasks = new Ar<>();
            try{
                for(Region region : atlas.regions){
                    // only regions on this page are of interest
                    // 只关心此页面上的区域
                    if(region.page != page) continue;

                    if(!quiet) System.out.printf("Processing image for %s: x[%s] y[%s] w[%s] h[%s], rotate[%s]%n",
                    region.name, region.left, region.top, region.width, region.height, region.rotate);

                    tasks.add(Tasks.submit(() -> {
                        splitRegion(img, region, outputDirFile, quiet);
                        return null;
                    }));
                }
                Tasks.joinAll(tasks);
            }finally{
                Tasks.cancelAll(tasks);
                //don't dispose while regions might still be reading the page
                // 在区域可能仍在读取页面时不要销毁
                Tasks.awaitAll(tasks);
                img.dispose();
            }
        }
    }

    private void splitRegion(Pixmap img, Region region, File outputDirFile, boolean quiet){
        Pixmap splitImage = null;
        String extension = null;

        // check if the region is a ninepatch or a normal image and delegate accordingly
        // 检查该区域是 ninepatch 还是普通图片,并相应地进行分派
        if(region.splits == null){
            splitImage = extractImage(img, region, 0);
            if(region.width != region.originalWidth || region.height != region.originalHeight){
                Pixmap originalImg = new Pixmap(region.originalWidth, region.originalHeight);
                originalImg.draw(splitImage, (int)region.offsetX, (int)(region.originalHeight - region.height - region.offsetY));
                splitImage.dispose();
                splitImage = originalImg;
            }
            extension = ".png";
        }else{
            splitImage = extractNinePatch(img, region);
            extension = "9.png";
        }

        try{
            // check if the parent directories of this image file exist and create them if not
            // 检查此图片文件的父目录是否存在,如不存在则创建
            File imgOutput = new File(outputDirFile, region.name + extension);
            File imgDir = imgOutput.getParentFile();
            if(!imgDir.exists()){
                //several regions may want the same directory at once, mkdirs is safe with that
                // 多个区域可能同时需要同一个目录,mkdirs 对此是安全的
                if(imgDir.mkdirs() && !quiet) System.out.printf("Creating directory: %s%n", imgDir.getPath());
            }

            new Fi(imgOutput).writePng(splitImage);
        }finally{
            splitImage.dispose();
        }
    }

    /**
     * Extract an image from a texture atlas.
     * <p>
     * 从纹理图集中提取图片。
     * @param page The image file related to the page the region is in 与区域所在页面相关的图片文件
     * @param region The region to extract 要提取的区域
     * @param padding padding (in pixels) to apply to the image 应用到图片的填充(像素)
     * @return The extracted image 提取出的图片
     */
    private Pixmap extractImage(Pixmap page, Region region, int padding){
        Pixmap splitImage;

        // get the needed part of the page and rotate if needed
        // 获取页面中所需的部分,并在需要时旋转
        splitImage = page.crop(region.left, region.top, region.width, region.height);

        // draw the image to a bigger one if padding is needed
        // 如果需要填充,则将图片绘制到更大的图片上
        if(padding > 0){
            Pixmap paddedImage = new Pixmap(splitImage.getWidth() + padding * 2, splitImage.getHeight() + padding * 2);
            paddedImage.draw(splitImage, padding, padding);
            splitImage.dispose();
            return paddedImage;
        }else{
            return splitImage;
        }
    }

    /**
     * Extract a ninepatch from a texture atlas, according to the android specification.
     * <p>
     * 根据 Android 规范从纹理图集中提取 ninepatch。
     * @param page The image file related to the page the region is in 与区域所在页面相关的图片文件
     * @param region The region to extract 要提取的区域
     * @see <a href="http://developer.android.com/guide/topics/graphics/2d-graphics.html#nine-patch">ninepatch specification</a>
     */
    private Pixmap extractNinePatch(Pixmap page, Region region){
        Pixmap splitImage = extractImage(page, region, NINEPATCH_PADDING);

        // Draw the four lines to save the ninepatch's padding and splits
        // 绘制四条线以保存 ninepatch 的填充和分割
        int startX = region.splits[0] + NINEPATCH_PADDING;
        int endX = region.width - region.splits[1] + NINEPATCH_PADDING - 1;
        int startY = region.splits[2] + NINEPATCH_PADDING;
        int endY = region.height - region.splits[3] + NINEPATCH_PADDING - 1;
        if(endX >= startX) splitImage.drawLine(startX, 0, endX, 0, Color.blackRgba);
        if(endY >= startY) splitImage.drawLine(0, startY, 0, endY, Color.blackRgba);
        if(region.pads != null){
            int padStartX = region.pads[0] + NINEPATCH_PADDING;
            int padEndX = region.width - region.pads[1] + NINEPATCH_PADDING - 1;
            int padStartY = region.pads[2] + NINEPATCH_PADDING;
            int padEndY = region.height - region.pads[3] + NINEPATCH_PADDING - 1;
            splitImage.drawLine(padStartX, splitImage.getHeight() - 1, padEndX, splitImage.getHeight() - 1, Color.blackRgba);
            splitImage.drawLine(splitImage.getWidth() - 1, padStartY, splitImage.getWidth() - 1, padEndY, Color.blackRgba);
        }

        return splitImage;
    }

    public static void main(String[] args){
        TextureUnpacker unpacker = new TextureUnpacker();

        String atlasFile = null, imageDir = null, outputDir = null;

        // parse the arguments and display the help text if there is a problem with the command line arguments
        // 解析参数,如果命令行参数有问题则显示帮助文本
        switch(unpacker.parseArguments(args)){
            case 0:
                System.out.println(HELP);
                return;
            case 3:
                outputDir = args[2];
            case 2:
                imageDir = args[1];
            case 1:
                atlasFile = args[0];
        }

        File atlasFileHandle = new File(atlasFile).getAbsoluteFile();
        if(!atlasFileHandle.exists()) throw new RuntimeException("Atlas file not found: " + atlasFileHandle.getAbsolutePath());
        String atlasParentPath = atlasFileHandle.getParentFile().getAbsolutePath();

        // Set the directory variables to a default when they weren't given in the variables
        // 如果变量中未给出,则将目录变量设为默认值
        if(imageDir == null) imageDir = atlasParentPath;
        if(outputDir == null) outputDir = (new File(atlasParentPath, DEFAULT_OUTPUT_PATH)).getAbsolutePath();

        // Opens the atlas file from the specified filename
        // 从指定的文件名打开图集文件
        TextureAtlasData atlas = new TextureAtlasData(new Fi(atlasFile), new Fi(imageDir), false);
        unpacker.splitAtlas(atlas, outputDir, false);
    }
}
