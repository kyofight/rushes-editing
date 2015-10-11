/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import fyprushediting.algorithm.NaiveSimilarityFinder;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import fyprushediting.algorithm.PixelComparison;
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
public class Processor extends Thread {

    /**
     * 
     */
    public ArrayList<Double> frameDiff;
    /**
     * 
     */
    public ArrayList<Long> tmeline;
    /**
     * 
     */
    public ArrayList<Long> cutTS;
    private double progressDuration;
    private final long end;
    private final long start;
    private final VideoInfo vinfo;
    private final int id;
    private final Options options;
    private IContainer container;
    private IStreamCoder videoCoder;
    private IStreamCoder audioCoder;
    private IVideoResampler resampler;
    private boolean tillEnd;
    private boolean checkStart;
    private boolean checkEnd;
    private long lagStart;
    private final JProgressBar pb;
    private final JLabel plb;

    /**
     * 
     * @param vinfo
     * @param options
     * @param id
     * @param pb
     * @param plb
     */
    public Processor(VideoInfo vinfo, Options options, int id, JProgressBar pb, JLabel plb) {

        this.pb = pb;
        this.plb = plb;

        this.vinfo = vinfo;
        this.id = id;
        this.options = options;
        this.frameDiff = new ArrayList<Double>();
        this.tmeline = new ArrayList<Long>();
        this.cutTS = new ArrayList<Long>();

        int segment = (int) (vinfo.vduration / options.numThread[options.algIndex]);
        this.start = id * segment;
        this.end = (id + 1) * segment;

        lagStart = start - VideoPlayer.DEFAULT_ERROR_DUMP * vinfo.durationPacket;
        if (lagStart < 0) {
            lagStart = start;
        }

        if ((id + 1) == options.numThread[options.algIndex]) {
            tillEnd = true;
        }

        //start thread
        if (id == 0) {
            checkStart = true; //to compare first frame logic check
        }
        //end thread, can be both for a thread
        if ((id + 1) == options.numThread[options.algIndex]) {
            checkEnd = true; //to compare last frame logic check
        }

        //progress for each packet: approximation
        this.progressDuration = vinfo.durationPacket / (double) (end - start) * 100;

        // Create a Xuggler container object
        container = IContainer.make();
        // Open up the container
        if (container.open(vinfo.url, IContainer.Type.READ, null) < 0) {
            throw new IllegalArgumentException("could not open file: " + vinfo.url);
        }

        int numStreams = container.getNumStreams();

        int videoStreamId = -1;
        int audioStreamId = -1;

        for (int i = 0; i < numStreams; i++) {
            // Find the stream object
            IStream stream = container.getStream(i);
            // Get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            if (videoStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
            } else if (audioStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamId = i;
                audioCoder = coder;
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


        if (videoStreamId == -1 && audioStreamId == -1) {
            throw new RuntimeException("could not find audio or video stream in container: " + vinfo.url);
        }

        if (videoCoder != null) {
            if (videoCoder.open() < 0) {
                throw new RuntimeException("could not open video decoder for container: " + vinfo.url);
            }
        }

        if (audioCoder != null && audioCoder.open() < 0) {
            throw new RuntimeException("could not open audio decoder for container: " + vinfo.url);
        }
    }

    
    
    
    @Override
    public void run() {
        IPacket packet = IPacket.make();
        int bytesDecoded;

        BufferedImage preImage = null;
        BufferedImage ppreImage = null;
        long ptimeStamp = -1;
        long pptimeStamp = -1;

        boolean flag = true;
        boolean pflag = true;
        boolean isDumpFrame = false;
        double ptolerance = options.threshold[options.algIndex] * options.tolerance[options.algIndex] / 100;
        double tolerance;
        
        
       
        try {
            while (container.readNextPacket(packet) >= 0) {
                if (Thread.interrupted()) {
                    closeAll();
                    // We've been interrupted: no more crunching.
                    return;
                }


                if (flag && packet.getTimeStamp() >= vinfo.vstartTime) {
                    container.seekKeyFrame(vinfo.vstream, Long.MIN_VALUE, lagStart, Long.MAX_VALUE, IContainer.SEEK_FLAG_ANY);
                    flag = false;
                    continue;
                }

                if (packet.getStreamIndex() == vinfo.vstream) {
                    if (packet.getTimeStamp() >= (start + vinfo.vstartTime) && packet.getTimeStamp() <= (end + vinfo.vstartTime)) {
                        synchronized (this) {
                            ProcessController.overallProgress += progressDuration;
                        }
                        setProgress();
                    }

                    IVideoPicture picture = IVideoPicture.make(vinfo.vformat,
                            vinfo.vwidth, vinfo.vheight);

                    try {
                        bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
                        if (bytesDecoded < 0) {
                            throw new RuntimeException();
                        }
                    } catch (Exception es) {
                        continue;
                    }

                    // pending to start
                    if (packet.getTimeStamp() < (start + vinfo.vstartTime)) {
                        //System.out.println(packet.getTimeStamp() + ":" + (targetPos + vinfo.vstartTime));
                        continue;
                    }

                    if (picture.isComplete()) {
                        if (pflag) {
                            //System.out.println("start : " + packet.getTimeStamp() + ", id: " + id);
                            //if(!picture.isKeyFrame()) continue;
                            pflag = false;
                        }

                        IVideoPicture newPic = picture;
                        /*
                         * If the resampler is not null, that means we didn't get the
                         * video in BGR24 format and
                         * need to convert it into BGR24 format.
                         */
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


                        BufferedImage javaImage = Utils.videoPictureToImage(newPic);
                        tmeline.add(packet.getTimeStamp());

                        if (checkStart) {
                            //System.out.println("Check Start: "+packet.getTimeStamp());
                            isDumpFrame = false;
                            for (BufferedImage dump : options.dumpImg) {
                                tolerance = getFrameDifference(javaImage, dump);
                                //System.out.println("Check Start tolerance: " + tolerance);
                                if (tolerance < ptolerance) {
                                    //dump frame
                                    isDumpFrame = true;
                                    break;
                                } else {
                                    //normal frame
                                    //keep looping
                                }
                            }
                            if (!isDumpFrame) {
                                cutTS.add(packet.getTimeStamp());
                            }
                            frameDiff.add(0.0);
                            checkStart = false;
                        }

                        double fd = 0;
                        if (preImage != null) {
                            fd = getFrameDifference(javaImage, preImage);
                            frameDiff.add(fd);

                            if (fd > options.threshold[options.algIndex]) {
                                //System.out.println("> Thresold: " + packet.getTimeStamp());
                                isDumpFrame = false;
                                for (BufferedImage dump : options.dumpImg) {
                                    tolerance = getFrameDifference(javaImage, dump);
                                    if (tolerance < ptolerance) {
                                        //dump frame
                                        //skip
                                        isDumpFrame = true;
                                        break;
                                    } else {
                                    }
                                }
                                if (!isDumpFrame) {
                                    //current frame is normal frame, so we check previous frame
                                    boolean preDump = false;
                                    for (BufferedImage dump2 : options.dumpImg) {
                                        tolerance = getFrameDifference(preImage, dump2);
                                        if (tolerance < ptolerance) {
                                            //dump frame
                                            //skip
                                            preDump = true;
                                            break;
                                        } else {
                                        }
                                    }
                                    if (!preDump) {
                                        cutTS.add(ptimeStamp);
                                    }
                                    cutTS.add(packet.getTimeStamp());
                                } else {
                                    isDumpFrame = false;
                                    //check previous image if dump skip, else add
                                    for (BufferedImage dumpx : options.dumpImg) {
                                        tolerance = getFrameDifference(preImage, dumpx);
                                        if (tolerance < ptolerance) {
                                            //dump frame
                                            //skip
                                            isDumpFrame = true;
                                            break;
                                        } else {
                                        }
                                    }
                                    if (!isDumpFrame) {
                                        //System.out.println("Dump before color:" + packet.getTimeStamp());
                                        cutTS.add(ptimeStamp);
                                    }
                                }
                            }
                        }

                        ppreImage = preImage;
                        preImage = javaImage;
                        pptimeStamp = ptimeStamp;
                        ptimeStamp = packet.getTimeStamp();

                        //pending to exit
                        if (packet.getTimeStamp() >= (end + vinfo.vstartTime) && !tillEnd) {
//                            System.out.println("exit : " + packet.getTimeStamp() + ", id: " + id);
                            break;
                        }
                    }
                } else {
                    do {
                    } while (false);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                e.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            //System.out.println(e.toString());
        }
        if (checkEnd) {
            if (cutTS.indexOf(ptimeStamp) == -1) {
//            System.out.println("Check End Ref: "+(preImage==ppreImage));
//            System.out.println("Check End: "+ptimeStamp);
                isDumpFrame = false;
                for (BufferedImage dumpn : options.dumpImg) {
                    tolerance = getFrameDifference(ppreImage, dumpn);
                    //System.out.println("Check End tolerance: " + tolerance);
                    if (tolerance < ptolerance) {
                        //dump frame
                        //skip
                        isDumpFrame = true;
                        break;
                    } else {
                    }
                }
                if (!isDumpFrame) {
                    cutTS.add(ptimeStamp);
                }
            }
        }
//        System.out.println("exited id: " + packet.getTimeStamp() +", size: "+frameDiff.size());
        closeAll();
    }

    private double getFrameDifference(BufferedImage javaImage, BufferedImage preImage) {
        double fd = 0.0;
        if (options.alg.equals("Naive Similarity")) {
            /* naive */
            NaiveSimilarityFinder finder = new NaiveSimilarityFinder(preImage);
            fd = finder.calcDistance(javaImage);
        } else if (options.alg.equals("Pixel Comparison")) {
            /*pixel comparison*/
            PixelComparison pc = new PixelComparison();
            fd = pc.compare(javaImage, preImage);
        }
        
        return fd;
    }

    public void closeAll() {
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (audioCoder != null) {
            audioCoder.close();
            audioCoder = null;
        }
        if (container != null) {
            container.close();
            container = null;
        }
    }

    private void setProgress() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                synchronized (this) {
                    int percent = (int)(ProcessController.overallProgress / (double) options.numThread[options.algIndex]);
                    pb.setValue(percent);
                    pb.setString(percent + "%");
                }
            }
        });
    }
}
