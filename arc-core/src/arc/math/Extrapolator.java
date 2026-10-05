package arc.math;

import java.util.*;

public class Extrapolator{
    private float[] snapPos, snapVel, aimPos, lastPacketPos;
    private float[] tmpArr, tmpArr2;
    private double snapTime, aimTime, lastPacketTime, latency, updateTime;
    private int size;

    public Extrapolator(int size){
        this.size = size;
        snapPos = new float[size];
        snapVel = new float[size];
        aimPos = new float[size];
        lastPacketPos = new float[size];
        tmpArr = new float[size];
        tmpArr2 = new float[size];
    }

    public boolean addSample(double packetTime, double curTime, float[] pos){
        // The best guess I can make for velocity is the difference between
        // 对速度的最佳猜测是以下两者之差
        // this sample and the last registered sample.
        // 此样本与上一个登记的样本。
        float[] vel = tmpArr2;
        if(Math.abs(packetTime - lastPacketTime) > 1e-4){
            double dt = 1.0 / (packetTime - lastPacketTime);
            for(int i = 0; i < size; ++i){
                vel[i] = (float)((pos[i] - lastPacketPos[i]) * dt);
            }
        }else{
            clear(vel);
        }

        return addSample(packetTime, curTime, pos, vel);
    }

    public boolean addSample(double packetTime, double curTime, float[] pos, float[] vel){
        if(!estimates(packetTime, curTime)){
            return false;
        }

        copyArray(lastPacketPos, pos);
        lastPacketTime = packetTime;
        readPosition(curTime, snapPos);
        aimTime = curTime + updateTime;
        double dt = aimTime - packetTime;
        snapTime = curTime;
        for(int i = 0; i < size; ++i){
            aimPos[i] = (float)(pos[i] + vel[i] * dt);
        }

        // I now have two positions and two times:
        // 我现在有两个位置和两个时间:
        //   1. aimPos / aimTime
        // 1. aimPos / aimTime(瞄准位置与瞄准时间)
        //   2. snapPos / snapTime
        // 2. snapPos / snapTime(快照位置与快照时间)
        // I must generate the interpolation velocity based on these two samples.
        // 我必须根据这两个样本生成插值速度。
        // However, if aimTime is the same as snapTime, I'm in trouble.
        // 然而,如果 aimTime 与 snapTime 相同,那我就麻烦了。
        // In that case, use the supplied velocity.
        // 这种情况下,使用提供的速度。
        if(Math.abs(aimTime - snapTime) < 1e-4){
            copyArray(snapVel, vel);
        }else{
            dt = 1.0 / (aimTime - snapTime);
            for(int i = 0; i < size; ++i){
                snapVel[i] = (float)((aimPos[i] - snapPos[i]) * dt);
            }
        }

        return true;
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     * {@code size = 1} 的外推器版本。
     */
    public boolean addSample(double packetTime, double curTime, float pos){
        checkArraySizeOne();

        tmpArr[0] = pos;
        return addSample(packetTime, curTime, tmpArr);
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     * {@code size = 1} 的外推器版本。
     */
    public boolean addSample(double packetTime, double curTime, float pos, float vel){
        checkArraySizeOne();

        tmpArr[0] = pos;
        tmpArr2[0] = vel;
        return addSample(packetTime, curTime, tmpArr, tmpArr2);
    }

    public void reset(double packetTime, double curTime, float[] pos){
        reset(packetTime, curTime, pos, clear(tmpArr));
    }

    public void reset(double packetTime, double curTime, float[] pos, float[] vel){
        lastPacketTime = packetTime;
        copyArray(lastPacketPos, pos);
        snapTime = curTime;
        copyArray(snapPos, pos);
        updateTime = curTime - packetTime;
        latency = updateTime;
        aimTime = curTime + updateTime;
        copyArray(snapVel, vel);

        for(int i = 0; i < size; ++i){
            aimPos[i] = (float)(snapPos[i] + snapVel[i] * updateTime);
        }
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     * {@code size = 1} 的外推器版本。
     */
    public void reset(double packetTime, double curTime, float pos){
        checkArraySizeOne();

        reset(packetTime, curTime, pos, 0);
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     * {@code size = 1} 的外推器版本。
     */
    public void reset(double packetTime, double curTime, float pos, float vel){
        checkArraySizeOne();

        lastPacketTime = packetTime;
        lastPacketPos[0] = pos;
        snapTime = curTime;
        snapPos[0] = pos;
        updateTime = curTime - packetTime;
        latency = updateTime;
        aimTime = curTime + updateTime;
        snapVel[0] = vel;
        aimPos[0] = (float)(snapPos[0] + snapVel[0] * updateTime);
    }

    public boolean readPosition(double forTime, float[] outPos){
        return readPosition(forTime, outPos, null);
    }

    public boolean readPosition(double forTime, float[] outPos, float[] outVel){
        boolean isOk = true;

        // asking for something before allowable time?
        // 请求的时间早于允许的时间?
        if(forTime < snapTime){
            forTime = snapTime;
            isOk = false;
        }

        // asking for something very far in the future?
        // 请求的是非常遥远的未来?
        double maxRange = aimTime + updateTime;
        if(forTime > maxRange){
            forTime = maxRange;
            isOk = false;
        }

        // calculate the interpolated position
        // 计算插值后的位置
        for(int i = 0; i < size; ++i){
            if(outVel != null){
                outVel[i] = snapVel[i];
            }

            outPos[i] = (float)(snapPos[i] + snapVel[i] * (forTime - snapTime));
        }

        if(!isOk && outVel != null){
            clear(outVel);
        }

        return isOk;
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     * {@code size = 1} 的外推器版本。
     */
    public float readPosition(double forTime){
        checkArraySizeOne();

        if(readPosition(forTime, tmpArr)){
            return tmpArr[0];
        }

        return 0;
    }

    public double estimateLatency(){
        return latency;
    }

    public double estimateUpdateTime(){
        return updateTime;
    }

    private boolean estimates(double packet, double cur){
        if(packet <= lastPacketTime){
            return false;
        }

        // The theory is that, if latency increases, quickly
        // 理论依据是:如果延迟增大,就迅速
        // compensate for it, but if latency decreases, be a
        // 对其进行补偿,但如果延迟减小,则要更
        // little more resilient; this is intended to compensate
        // 韧性更强一点;此设计旨在补偿
        // for jittery delivery.
        // 以应对颠簸的传输。
        double lat = cur - packet;
        if(lat < 0){
            lat = 0;
        }
        if(lat > latency){
            latency = (latency + lat) * 0.5;
        }else{
            latency = (latency * 7 + lat) * 0.125;
        }

        // Do the same running average for update time.
        // 对更新时间做同样的滑动平均。
        // Again, the theory is that a lossy connection wants
        // 同样,其理论依据是:有损连接希望
        // an average of a higher update time.
        // 更新时间的平均值偏大。
        double tick = packet - lastPacketTime;
        if(tick > updateTime){
            updateTime = (updateTime + tick) * 0.5;
        }else{
            updateTime = (updateTime * 7 + tick) * 0.125;
        }

        return true;
    }

    private float[] clear(float[] arr){
        Arrays.fill(arr, 0);
        return arr;
    }

    private void copyArray(float[] dest, float[] src){
        for(int i = 0, n = src.length; i < n; ++i){
            dest[i] = src[i];
        }
    }

    private void checkArraySizeOne(){
        if(size != 1){
            throw new UnsupportedOperationException("This function should be called only when size = 1!");
        }
    }
}