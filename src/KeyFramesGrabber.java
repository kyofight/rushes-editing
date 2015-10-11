/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

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
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author kyo
 */
public class KeyFramesGrabber extends Thread {

    private final JProgressBar pb;
    private final JLabel plb;
    private final VideoInfo vinfo;
    private final Options options;
    
    public final int id;
    public final ArrayList<BufferedImage> keyframes;
    public final long startTS;
    public final long endTS;
    
    private IContainer container;
    private IStreamCoder videoCoder;
    private IVideoResampler resampler;
    private final ArrayList<Long> keyframeTime;
    private final ArrayList<Long> seekKeyframeTime;
    private final int totalSegments;
    private final double durationKF;

    public KeyFramesGrabber(int totalSegments, int id, long startTS, long endTS, VideoInfo vinfo, Options options, JProgressBar pb, JLabel plb) {
        this.pb = pb;
        this.plb = plb;

        this.vinfo = vinfo;
        this.id = id;
        this.options = options;
        this.keyframes = new ArrayList<BufferedImage>();
        this.keyframeTime = new ArrayList<Long>();
        this.seekKeyframeTime = new ArrayList<Long>();
        this.startTS = startTS;
        this.endTS = endTS;
        this.totalSegments = totalSegments;

        long durationSegment = (long) Math.ceil((endTS - startTS) / (double) options.keyframeSegments);



        for (long startSegmentTime = startTS + durationSegment; startSegmentTime < endTS; startSegmentTime += durationSegment) {
            long lagStart = startTS - VideoPlayer.DEFAULT_ERROR_DUMP * vinfo.durationPacket;
            if (lagStart < 0) {
                lagStart = startTS;
            }

            this.seekKeyframeTime.add(lagStart);
            this.keyframeTime.add(startSegmentTime);
        }
        
        durationKF = Math.ceil(1/(double)keyframeTime.size() * 100) ;

        // Create a Xuggler container object
        container = IContainer.make();
        // Open up the container
        if (container.open(vinfo.url, IContainer.Type.READ, null) < 0) {
            throw new IllegalArgumentException("could not open file: " + vinfo.url);
        }

        int numStreams = container.getNumStreams();

        int videoStreamId = -1;

        for (int i = 0; i < numStreams; i++) {
            // Find the stream object
            IStream stream = container.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            if (videoStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
            }
        }

        resampler = null;
        if (videoCoder.getPixelType() != IPixelFormat.Type.RGB24) {
            // if this stream is not in BGR24, we're going to need to
            // convert it.  The VideoResampler does that for us.
            resampler = IVideoResampler.make(videoCoder.getWidth(),
                    videoCoder.getHeight(), IPixelFormat.Type.RGB24,
                    videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
            if (resampler == null) {
                throw new RuntimeException("could not create color space "
                        + "resampler for: " + vinfo.url);
            }
        }


        if (videoStreamId == -1) {
            throw new RuntimeException("could not find audio or video stream in container: " + vinfo.url);
        }

        if (videoCoder != null) {
            if (videoCoder.open() < 0) {
                throw new RuntimeException("could not open video decoder for container: " + vinfo.url);
            }
        }

    }

    @Override
    public void run() {
        boolean flag = true;
        IPacket packet = IPacket.make();

        IVideoPicture picture = null;
        double packetTS = 0.0;

        long targetPos = -1;
        long lagStart = -1;


        //get 1st thumbnail
        try {
            while (container.readNextPacket(packet) >= 0) {
                if (Thread.interrupted()) {
                    closeAll();
                    // We've been interrupted: no more crunching.
                    return;
                }

                if (flag && packet.getTimeStamp() >= vinfo.vstartTime) {
                    if (seekKeyframeTime.isEmpty() && keyframeTime.isEmpty()) {
                        break;
                    }
                    lagStart = seekKeyframeTime.remove(0);
                    targetPos = keyframeTime.remove(0);

                    container.seekKeyFrame(vinfo.vstream, Long.MIN_VALUE, lagStart, Long.MAX_VALUE, IContainer.SEEK_FLAG_ANY);
                    flag = false;
                    continue;
                }


                if (packet.getStreamIndex() == vinfo.vstream) {
                    picture = IVideoPicture.make(vinfo.vformat,
                            vinfo.vwidth, vinfo.vheight);

                    try {
                        int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
                        if (bytesDecoded < 0) {
                            throw new RuntimeException();
                        }
                    } catch (Exception es) {
                        continue;
                    }

                    // pending to start
                    if (packet.getTimeStamp() < targetPos) {
                        //System.out.println(packet.getTimeStamp() + ":" + (targetPos + vinfo.vstartTime));
                        continue;
                    }

                    if (picture != null && picture.isComplete()) {
                        IVideoPicture newPic = picture;
                        
                        if (resampler != null) {
                            // we must resample
                            newPic = IVideoPicture.make(resampler.getOutputPixelFormat(),
                                    picture.getWidth(), picture.getHeight());
                            if (resampler.resample(newPic, picture) < 0) {
                                throw new RuntimeException("could not resample video from: "
                                        + vinfo.url);
                            }
                        }
                        if (newPic.getPixelType() != IPixelFormat.Type.RGB24) {
                            throw new RuntimeException("could not decode video"
                                    + " as BGR 24 bit data in: " + vinfo.url);
                        }

                        keyframes.add(Utils.videoPictureToImage(newPic));
                        synchronized (this) {
                            ProcessController.overallProgress += durationKF;
                        }
                        setProgress();
                        flag = true;
                    }

                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                e.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            //System.out.println(e.toString());
        }

        if (Thread.interrupted()) {
            closeAll();
            // We've been interrupted: no more crunching.
            return;
        }

        closeAll();

    }

    public void closeAll() {
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (container != null) {
            container.close();
            container = null;
        }
    }

    public void setProgress() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                synchronized (this) {
                    int percent = (int) Math.ceil(ProcessController.overallProgress / (double) totalSegments);
                    pb.setValue(percent);
                    pb.setString(percent + "%");
                }
            }
        });
    }
}
