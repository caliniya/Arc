package arc.util.io;

import java.io.ByteArrayInputStream;

/**
 * A {@link ByteArrayInputStream} that can have its content bytes reset.
 * 一个 {@link ByteArrayInputStream},其内容字节可被重置。
 */
public class ReusableByteInStream extends ByteArrayInputStream{

    /**
     * {@link #setBytes} must be called before this stream can be used.
     * 必须先调用 {@link #setBytes},该流才能使用。
     */
    public ReusableByteInStream(){
        super(new byte[0]);
    }

    public int position(){
        return pos;
    }

    public void setBytes(byte[] bytes){
        pos = 0;
        count = bytes.length;
        mark = 0;
        buf = bytes;
    }

    public void setBytes(byte[] bytes, int offset, int length){
        this.buf = bytes;
        this.pos = offset;
        this.count = Math.min(offset + length, bytes.length);
        this.mark = offset;
    }
}
