/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IAudioSamples.Format;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kyo
 */
class ImageProcessor extends Thread{

  public LinkedBlockingQueue<BufferedImage> bImages;
  public LinkedBlockingQueue<IAudioSamples> bAudio;
  
  public IStreamCoder videoCoder;
  public IStreamCoder audioCoder;
  
  public VideoInfo vinfo;
  public CoderInfo videoInfo;
  public CoderInfo audioInfo;
  
  private boolean isReady;
  private String filename;
  private String errMsg;
  
  class VideoInfo {
    public int numStreams;
    public long duration;
    public long fileSize;
    public int bitRate;
    
    public VideoInfo(int numStreams, long duration, long fileSize, int bitRate){
        this.numStreams = numStreams;
        this.duration = duration;
        this.fileSize = fileSize;
        this.bitRate = bitRate;
    }
  }
  
  class CoderInfo {
    public int stream;
    public String type;
    public String codec;
    public long duration;
    public long startTime;
    public int timeBase;
    public int coderTimeBase;

    //for both coder
    public double rate;
    
    //for video coder
    public int width;
    public int height;
    public IPixelFormat.Type vformat;
    //for audio coder
    public int channels;
    public Format aformat;
    
    public CoderInfo(int stream, String type, String codec, long duration, long startTime, int timeBase, int coderTimeBase){
        this.stream = stream;
        this.type = type;
        this.codec = codec;
        this.duration = duration;
        this.startTime = startTime;
        this.timeBase = timeBase;
        this.coderTimeBase = coderTimeBase;
    }
    
    public void setAudioInfo(double rate, Format format, int channels){
        this.rate = rate;
        this.aformat = format;
        this.channels = channels;
    }
    
    public void setVideoInfo(double rate, IPixelFormat.Type format, int width, int height){
        this.rate = rate;
        this.vformat = format;
        this.width = width;
        this.height = height;
    }
  }
  
  
  public ImageProcessor(String url){
      isReady = false;
      filename = url;
      bImages = new LinkedBlockingQueue<BufferedImage>(100);
      bAudio = new LinkedBlockingQueue<IAudioSamples>(100);
      
  }
  
  public String getError(){
      return errMsg;
  }
  
  @SuppressWarnings("deprecation")
  public void run() {
     
    // Let's make sure that we can actually convert video pixel formats.
    if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)){
      isReady = false;
      errMsg += "you must install the GPL version of Xuggler (with IVideoResampler support) for this demo to work\n";
      throw new RuntimeException("you must install the GPL version of Xuggler (with IVideoResampler support) for this demo to work");
    }
    
    // Create a Xuggler container object
    IContainer container = IContainer.make();
     
    // Open up the container
    if (container.open(filename, IContainer.Type.READ, null) < 0){
      isReady = false;
      errMsg += "could not open file: " + filename +"\n";
      throw new IllegalArgumentException("could not open file: " + filename);
    }
    
    //store general video info
    vinfo = new VideoInfo(container.getNumStreams(), container.getDuration(), container.getFileSize(), container.getBitRate());

    // and iterate through the streams to find the first audio stream
    int videoStreamId = -1;
    videoCoder = null;
    int audioStreamId = -1;
    audioCoder = null;
    for(int i = 0; i < vinfo.numStreams; i++)
    {
      // Find the stream object
      IStream stream = container.getStream(i);
      // Get the pre-configured decoder that can decode this stream;
      IStreamCoder coder = stream.getStreamCoder();

      
      if (videoStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
      {
          
        videoStreamId = i;
        videoCoder = coder;
        //public CoderInfo(int stream, String type, String codec, int duration, int startTime, int timeBase, int coderTimeBase){
        videoInfo = new CoderInfo(i,coder.getCodecType().toString(),coder.getCodecID().toString(),stream.getDuration(),container.getStartTime(),
                stream.getTimeBase().getDenominator(),coder.getTimeBase().getDenominator());
        //public void setVideoInfo(int rate, String format, int width, int height){
        videoInfo.setVideoInfo(coder.getFrameRate().getDouble(), coder.getPixelType(), coder.getWidth(), coder.getHeight());
      }
      else if (audioStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO)
      {
        audioStreamId = i;
        audioCoder = coder;
        //public CoderInfo(int stream, String type, String codec, int duration, int startTime, int timeBase, int coderTimeBase){
        audioInfo = new CoderInfo(i,coder.getCodecType().toString(),coder.getCodecID().toString(),stream.getDuration(),container.getStartTime(),
                stream.getTimeBase().getDenominator(),coder.getTimeBase().getDenominator());
        //public void setAudioInfo(double rate, String format, int channels){
        audioInfo.setAudioInfo(coder.getSampleRate(), coder.getSampleFormat(), coder.getChannels());
      }
      

      
    }
    if (videoStreamId == -1 && audioStreamId == -1){
      isReady = false;
      throw new RuntimeException("could not find audio or video stream in container: "+filename);
    }
    /*
     * Check if we have a video stream in this file.  If so let's open up our decoder so it can
     * do work.
     */
    IVideoResampler resampler = null;
    if (videoCoder != null)
    {
      if(videoCoder.open() < 0){
        isReady = false;
        errMsg += "could not open audio decoder for container: " + filename +"\n";
        throw new RuntimeException("could not open audio decoder for container: "+filename);
      }
      
      if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24)
      {
        // if this stream is not in BGR24, we're going to need to
        // convert it.  The VideoResampler does that for us.
        resampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24,
            videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
        if (resampler == null){
          isReady = false;
          errMsg += "could not create color space resampler for: " + filename +"\n";
          throw new RuntimeException("could not create color space resampler for: " + filename);
        }
      }

    }
     
    if (audioCoder != null)
    {
      if (audioCoder.open() < 0){
        isReady = false;  
        errMsg += "could not open audio decoder for container: " + filename +"\n";
        throw new RuntimeException("could not open audio decoder for container: "+filename);
      }
    }
     
    isReady = true;  
    /*
     * Now, we start walking through the container looking at each packet.
     */
    //long mSystemVideoClockStartTime = System.currentTimeMillis();
    
    IPacket packet = IPacket.make();
    while(container.readNextPacket(packet) >= 0)
    {
      /*
       * Now we have a packet, let's see if it belongs to our video stream
       */
      if (packet.getStreamIndex() == videoStreamId)
      {
        /*
         * We allocate a new picture to get the data out of Xuggler
         */
        IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
            videoCoder.getWidth(), videoCoder.getHeight());
         
        //System.out.println(packet.getSize());
        //System.out.println(packet.getTimeStamp() - prevStamp);
        //prevStamp = packet.getTimeStamp(); 
        
        /*
         * Now, we decode the video, checking for any errors.
         * 
         */
        int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
        if (bytesDecoded < 0){
          errMsg += "got error decoding audio in: " + filename +"\n";
          throw new RuntimeException("got error decoding audio in: " + filename);
        }
        /*
         * Some decoders will consume data in a packet, but will not be able to construct
         * a full video picture yet.  Therefore you should always check if you
         * got a complete picture from the decoder
         */
        if (picture.isComplete())
        {
          IVideoPicture newPic = picture;
          /*
           * If the resampler is not null, that means we didn't get the video in BGR24 format and
           * need to convert it into BGR24 format.
           */
          if (resampler != null)
          {
            // we must resample
            newPic = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
            if (resampler.resample(newPic, picture) < 0){
              errMsg += "could not resample video from: " + filename +"\n";
              throw new RuntimeException("could not resample video from: " + filename);
            }
          }
          if (newPic.getPixelType() != IPixelFormat.Type.BGR24){
            errMsg += "could not decode video as BGR 24 bit data in: " + filename +"\n";
            throw new RuntimeException("could not decode video as BGR 24 bit data in: " + filename);
          }
            try {
                // And finally, convert the picture to an image and display it
                bImages.put(Utils.videoPictureToImage(newPic));
            } catch (InterruptedException ex) {
                Logger.getLogger(ImageProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }


        }
      }
      else if (packet.getStreamIndex() == audioStreamId)
      {
        /*
         * We allocate a set of samples with the same number of channels as the
         * coder tells us is in this buffer.
         * 
         * We also pass in a buffer size (1024 in our example), although Xuggler
         * will probably allocate more space than just the 1024 (it's not important why).
         */
        IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
        /*
         * A packet can actually contain multiple sets of samples (or frames of samples
         * in audio-decoding speak).  So, we may need to call decode audio multiple
         * times at different offsets in the packet's data.  We capture that here.
         */
        int offset = 0;
        //System.out.println(packet.getSize()); 
         //System.out.println(packet.getTimeStamp() - prevStamp);
        //prevStamp = packet.getTimeStamp(); 
         
        /*
         * Keep going until we've processed all data
         */
        while(offset < packet.getSize())
        {
          int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
          if (bytesDecoded < 0)
            throw new RuntimeException("got error decoding audio in: " + filename);
          offset += bytesDecoded;
          /*
           * Some decoder will consume data in a packet, but will not be able to construct
           * a full set of samples yet.  Therefore you should always check if you
           * got a complete set of samples from the decoder
           */
          if (samples.isComplete())
          {
            try {
                bAudio.put(samples);
            } catch (InterruptedException ex) {
                Logger.getLogger(ImageProcessor.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
        }
      }
      else
      {
        /*
         * This packet isn't part of our video stream, so we just silently drop it.
         */
        do {} while(false);
      }
       
    }
    //System.out.print(System.currentTimeMillis() - mSystemVideoClockStartTime);
    
    /*
     * Technically since we're exiting anyway, these will be cleaned up by 
     * the garbage collector... but because we're nice people and want
     * to be invited places for Christmas, we're going to show how to clean up.
     */
    if (videoCoder != null)
    {
      videoCoder.close();
      videoCoder = null;
    }
    if (audioCoder != null)
    {
      audioCoder.close();
      audioCoder = null;
    }
    if (container !=null)
    {
      container.close();
      container = null;
    }
  }

    public boolean isReady() {
        return isReady;
    }
    
}
