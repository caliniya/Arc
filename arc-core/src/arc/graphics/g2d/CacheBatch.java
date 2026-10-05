package arc.graphics.g2d;

import arc.graphics.*;
import arc.math.*;

public class CacheBatch extends Batch{
    public SpriteCache cache;
    float[] tmpVertices = new float[20];

    public CacheBatch(int size){
        this(new SpriteCache(size, false));
    }

    public CacheBatch(SpriteCache cache){
        this.cache = cache;
    }

    @Override
    public void flush(){
        //does nothing, since flushing like this isn't needed
        // 不做任何事,因为不需要这种刷新
    }

    @Override
    public void setPackedColor(float color){
        cache.setPackedColor(color);
    }

    @Override
    public float getPackedColor(){
        return cache.getPackedColor();
    }

    @Override
    public void setProjection(Mat projection){
        cache.setProjectionMatrix(projection);
    }

    public void beginCache(){
        cache.beginCache();
    }

    public int endCache(){
        return cache.endCache();
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        //this creates a new array, but considering it's being cached garbage probably isn't important anyway
        // 这会创建一个新数组,但考虑到是缓存场景,产生的垃圾应该无关紧要
        float[] vertices = count / 6 * 5 == tmpVertices.length ? tmpVertices : new float[count / 6 * 5];
        for(int i = 0; i < count / 6; i++){
            int index = i * 6;
            int dest = i * 5;
            vertices[dest] = spriteVertices[offset + index];
            vertices[dest + 1] = spriteVertices[offset + index + 1];
            vertices[dest + 2] = spriteVertices[offset + index + 2];
            vertices[dest + 3] = spriteVertices[offset + index + 3];
            vertices[dest + 4] = spriteVertices[offset + index + 4];
        }

        //TODO do some copying to fix this for non-indexed batches
        // TODO 通过一些复制操作来修复非索引批处理的情况
        cache.add(texture, vertices, 0, vertices.length);
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        cache.add(region, x, y, originX, originY, width, height, 1f, 1f, rotation);
    }

    @Override
    public void setShader(Shader shader){
        setShader(shader, true);
    }

    @Override
    public void setShader(Shader shader, boolean apply){
        boolean drawing = cache.isDrawing();

        if(drawing) cache.end();
        cache.setShader(shader);
        if(drawing) cache.begin();
        if(apply && shader != null) shader.apply();
    }

    @Override
    public void dispose(){
        super.dispose();
        cache.dispose();
    }

    public void beginDraw(){
        cache.begin();
    }

    public void endDraw(){
        cache.end();
    }

    public void drawCache(int id){
        cache.draw(id);
    }
}
