/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import com.xuggle.xuggler.IAudioSamples.Format;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;

/**
 *
 * @author kyo
 */
public class VideoInfo {
    /**
     * 
     */
    public double toPacketTB;
    /**
     * 
     */
    public double toPictureTB;
    /**
     * 
     */
    public int durationPacket;
    public int picDurationPacket;
    /**
     * 
     */
    public String url;
    /**
     * 
     */
    public String fileName;
    /**
     * 
     */
    public String videoDirectory;
    //general info
    /**
     * 
     */
    public int numStreams;
    /**
     * 
     */
    public long duration;
    /**
     * 
     */
    public long fileSize;
    /**
     * 
     */
    public int bitRate;
    //video
    /**
     * 
     */
    public int vstream;
    /**
     * 
     */
    public String vtype;
    /**
     * 
     */
    public ICodec vcodec;
    /**
     * 
     */
    public long vduration;
    /**
     * 
     */
    public long vstartTime;
    /**
     * 
     */
    public int vtimeBase;
    /**
     * 
     */
    public int vcoderTimeBase;
    /**
     * 
     */
    public double vframeRate;
    /**
     * 
     */
    public int vwidth;
    /**
     * 
     */
    public int vheight;
    /**
     * 
     */
    public IPixelFormat.Type vformat;
    //audio
    /**
     * 
     */
    public int astream;
    /**
     * 
     */
    public String atype;
    /**
     * 
     */
    public ICodec acodec;
    /**
     * 
     */
    public long aduration;
    /**
     * 
     */
    public long astartTime;
    /**
     * 
     */
    public int atimeBase;
    /**
     * 
     */
    public int acoderTimeBase;
    /**
     * 
     */
    public int asampleRate;
    /**
     * 
     */
    public int achannels;
    /**
     * 
     */
    public Format aformat;
    
    
    

    /**
     * 
     * @param numStreams
     * @param duration
     * @param fileSize
     * @param bitRate
     */
    public VideoInfo(int numStreams, long duration, long fileSize, int bitRate) {
        this.numStreams = numStreams;
        this.duration = duration;
        this.fileSize = fileSize;
        this.bitRate = bitRate;
    }

    /**
     * 
     * @param stream
     * @param type
     * @param codec
     * @param duration
     * @param startTime
     * @param timeBase
     * @param coderTimeBase
     */
    public void setVGeneralInfo(int stream, String type, ICodec codec, long duration, long startTime, int timeBase, int coderTimeBase) {
        this.vstream = stream;
        this.vtype = type;
        this.vcodec = codec;
        this.vduration = duration;
        this.vstartTime = startTime;
        this.vtimeBase = timeBase;
        this.vcoderTimeBase = coderTimeBase;
    }

    /**
     * 
     * @param stream
     * @param type
     * @param codec
     * @param duration
     * @param startTime
     * @param timeBase
     * @param coderTimeBase
     */
    public void setAGeneralInfo(int stream, String type, ICodec codec, long duration, long startTime, int timeBase, int coderTimeBase) {
        this.astream = stream;
        this.atype = type;
        this.acodec = codec;
        this.aduration = duration;
        this.astartTime = startTime;
        this.atimeBase = timeBase;
        this.acoderTimeBase = coderTimeBase;
    }

    /**
     * 
     * @param rate
     * @param format
     * @param channels
     */
    public void setAudioInfo(int rate, Format format, int channels) {
        this.asampleRate = rate;
        this.aformat = format;
        this.achannels = channels;
    }

    /**
     * 
     * @param rate
     * @param format
     * @param width
     * @param height
     */
    public void setVideoInfo(double rate, IPixelFormat.Type format, int width, int height) {
        this.vframeRate = rate;
        this.vformat = format;
        this.vwidth = width;
        this.vheight = height;
    }
}
