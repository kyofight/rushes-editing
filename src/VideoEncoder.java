/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import fyprushediting.ProcessController.ImagePanel;
import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;
import it.sauronsoftware.jave.VideoAttributes;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author kyo
 */
public class VideoEncoder extends Thread {

    private final VideoInfo vinfo;
    private final Options options;
    private long startTS;
    private long endTS;
    private final JComboBox fileCombox;
    private final JPanel scrollInternalPanel;
    private final String outPath;
    private IContainer container;
    private IStreamCoder videoCoder;
    private long lagStart;
    private final int id;
    private final String fullPath;
    private final IVideoResampler resampler;
    private final int thumbnailWidth;
    private final int thumbnailHeight;
    private final Dimension ThumbSize;
    private final ImagePanel thumb;
    private final String outputFileName;
    private final JProgressBar pb;
    private final JLabel plb;
    private final int totalSegments;
    private BufferedImage startImage;
    private BufferedImage startImagePlay;

    VideoEncoder(int totalSegments, int id, ImagePanel thumb, long startTS, long endTS, Dimension ThumbSize,
            String outPath, VideoInfo vinfo, Options options, JComboBox fileCombox, JPanel scrollInternalPanel, JProgressBar pb, JLabel plb) {
        this.id = id;
        this.vinfo = vinfo;
        this.options = options;
        this.startTS = startTS;
        this.endTS = endTS;
        this.outPath = outPath;
        this.ThumbSize = ThumbSize;
        this.thumb = thumb;
        this.pb = pb;
        this.plb = plb;
        this.totalSegments = totalSegments;

        this.fileCombox = fileCombox;
        this.scrollInternalPanel = scrollInternalPanel;


        this.lagStart = startTS - VideoPlayer.DEFAULT_ERROR_DUMP * vinfo.durationPacket;
        if (lagStart < 0) {
            this.lagStart = startTS;
        }


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

        if (videoStreamId == -1) {
            throw new RuntimeException("could not find audio or video stream in container: " + vinfo.url);
        }

        if (videoCoder != null) {
            if (videoCoder.open() < 0) {
                throw new RuntimeException("could not open video decoder for container: " + vinfo.url);
            }
        }


        double width = ThumbSize.getWidth();
        double height = ThumbSize.getHeight();

        double xScale = width / (double) vinfo.vwidth;
        double yScale = height / (double) vinfo.vheight;
        double scale = Math.min(xScale, yScale);    // scale to fit
        thumbnailWidth = (int) (scale * vinfo.vwidth);
        thumbnailHeight = (int) (scale * vinfo.vheight);
        resampler = IVideoResampler.make(thumbnailWidth, thumbnailHeight, videoCoder.getPixelType(),
                videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());

        if (resampler == null) {
            throw new RuntimeException("could not create color space resampler for: " + vinfo.url);
        }


        String ext = vinfo.fileName.substring(vinfo.fileName.lastIndexOf("."));
        fullPath = outPath + System.getProperty("file.separator") + "Scene_" + id + "" + ext;
        outputFileName = "Scene_" + id + "" + ext;
    }

    @Override
    public void run() {
        boolean flag = true;
        IPacket packet = IPacket.make();

        IVideoPicture picture = null;
        double packetTS = 0.0;
        //get 1st thumbnail
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
                    if (packet.getTimeStamp() < (startTS)) {
                        //System.out.println(packet.getTimeStamp() + ":" + (targetPos + vinfo.vstartTime));
                        continue;
                    }

                    if (picture != null && picture.isComplete()) {

                        if (!picture.isKeyFrame()) {
                            continue;
                        }

                        IVideoPicture newPic = IVideoPicture.make(picture.getPixelType(), thumbnailWidth, thumbnailHeight);
                        if (resampler != null) {
                            if (resampler.resample(newPic, picture) < 0) {
                                throw new RuntimeException("could not resample video from: " + vinfo.url);
                            }
                        }

                        startImage = Utils.videoPictureToImage(newPic);
                        startImagePlay = Utils.videoPictureToImage(newPic);
                        packetTS = packet.getTimeBase().getValue();
                        startTS = packet.getTimeStamp();
                        break;
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

        /************************** error encoding duration, temp fix! ***************************/
        int secDuration = (int) (vinfo.durationPacket * vinfo.vframeRate * 0.5);
//        this.startTS -= secDuration;
        this.endTS -= secDuration;
//        if(this.startTS<0) this.startTS = 0;
        if (this.endTS < 0) {
            this.endTS = 0;
        }


        /********************** Encoder ********************/
        File source = new File(vinfo.url);
        File target = new File(fullPath);
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec(AudioAttributes.DIRECT_STREAM_COPY);
        VideoAttributes video = new VideoAttributes();
        video.setCodec(VideoAttributes.DIRECT_STREAM_COPY);
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat(options.selectedFormat);
        attrs.setAudioAttributes(audio);
        attrs.setVideoAttributes(video);
        float startOffset = (float) (startTS * packetTS);
        float endOffset = (float) ((endTS - startTS) * packetTS);
        //System.out.println("start encoding:"+packetTS+", end:"+packetTS);
        attrs.setOffset(startOffset);
        attrs.setDuration(endOffset);
        Encoder encoder = new Encoder();

        try {
//            String ef[] = encoder.getSupportedEncodingFormats();
//            for(int i=0; i<ef.length; i++) System.out.println(ef[i]);
            encoder.encode(source, target, attrs);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(null,
                    ex.toString(),
                    "Errors",
                    JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(VideoEncoder.class.getName()).log(Level.SEVERE, null, ex);
            if (Thread.interrupted()) {
                closeAll();
                // We've been interrupted: no more crunching.
                return;
            }
        } catch (InputFormatException ex) {
            JOptionPane.showMessageDialog(null,
                    ex.toString(),
                    "Errors",
                    JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(VideoEncoder.class.getName()).log(Level.SEVERE, null, ex);
            if (Thread.interrupted()) {
                closeAll();
                // We've been interrupted: no more crunching.
                return;
            }
        } catch (EncoderException ex) {
            JOptionPane.showMessageDialog(null,
                    ex.toString(),
                    "Errors",
                    JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(VideoEncoder.class.getName()).log(Level.SEVERE, null, ex);
            if (Thread.interrupted()) {
                closeAll();
                // We've been interrupted: no more crunching.
                return;
            }
        }

        if (Thread.interrupted()) {
            closeAll();
            // We've been interrupted: no more crunching.
            return;
        }

        synchronized (this) {
            ProcessController.overallProgress += 100;
        }

        setProgress();

        closeAll();
    }

    private String getPictureTime(long duration) {
        //System.out.println(duration);
        int secs = (int) (duration * vinfo.toPictureTB / VideoPlayer.DEFAULT_PICTURE_BASE);
        int hours = secs / 3600,
                remainder = secs % 3600,
                minutes = remainder / 60,
                seconds = remainder % 60,
                ms = (int) (duration * vinfo.toPictureTB / VideoPlayer.DEFAULT_PICTURE_BASE * 1000) % 1000;

        String disHour = (hours < 10 ? "0" : "") + hours,
                disMinu = (minutes < 10 ? "0" : "") + minutes,
                disSec = (seconds < 10 ? "0" : "") + seconds,
                disMs = (ms < 100 ? (ms < 10 ? "00" : "0") : "") + ms;
        return disHour + ":" + disMinu + ":" + disSec + "." + disMs;
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

    /**
     * 
     */
    public void addThumbAndCombox() {
        thumb.drawThumb(startImage, startImagePlay, (thumb.getWidth() - thumbnailWidth) / 2, (thumb.getHeight() - thumbnailHeight) / 2,
                outputFileName, getPictureTime(endTS - startTS), fullPath);
        thumb.repaint();
        //for thread safe
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                fileCombox.addItem(fullPath);
            }
        });
    }

    /**
     * 
     */
    public void setProgress() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                synchronized (this) {
                    int percent = (int) Math.ceil(ProcessController.overallProgress / (double) totalSegments);
                    pb.setValue(percent);
                    pb.setString(percent + "%");
                    if (percent >= 100) {
                        pb.setString("Pending");
                        plb.setText("Process Complete");
                    }
                }
            }
        });
    }
}
