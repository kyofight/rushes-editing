/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

/**
 *
 * @author kyo
 */
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import java.awt.*;
import java.nio.ShortBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * 
 * @author kyo
 */
public class VideoPlayer extends JPanel implements Runnable {

    /**
     * To avoid a warning... 
     */
    private SourceDataLine mLine;
    private long mSystemVideoClockStartTime;
    private long mFirstVideoTimestampInStream;
    private String url;
    IContainer container;
    IStreamCoder videoCoder;
    IStreamCoder audioCoder;
    private IVideoResampler resampler;
    private Image itd;
    private boolean isReady;
    private VideoInfo vinfo;
    private int playerHeight;
    private int playerWidth;
    /**
     * 
     */
    public final static int DEFAULT_ERROR_DUMP = 20;
    /**
     * 
     */
    public final static int DEFAULT_BEGIN_DUMP = 0;
    /**
     * 
     */
    public final static double DEFAULT_PICTURE_BASE = 1000000.0;
    private boolean isMute;
    private final JSlider soundSlider;
    private final JSlider durationSlider;
    private final JLabel durationLB;
    private final int x, y;
    private final JComboBox fileCombox;
    private long ePicTimeStamp;
    private ArrayBlockingQueue<Runnable> queue;
    private ThreadPoolExecutor threadPool;

    /**
     * 
     */
    public enum status {

        /**
         * 
         */
        PAUSE,
        /**
         * 
         */
        PLAY,
        /**
         * 
         */
        SEEK,
        /**
         * 
         */
        DESTROY
    };
    private status cstatus;
    private boolean stateTransition;
    private long pos;
    private double forward;
    /**
     * 
     */
    public boolean seekAndPlay;
    /**
     * 
     */
    public boolean isRunning;
    private int dropCount;
    private long targetPos;

    /**
     * 
     * @param url
     * @param vinfo
     * @param psize
     * @param isMute
     * @param soundSlider
     * @param durationSlider
     * @param durationLB
     * @param fileCombox
     * @throws InterruptedException
     */
    public VideoPlayer(String url, final VideoInfo vinfo, Dimension psize, boolean isMute, JSlider soundSlider, JSlider durationSlider, JLabel durationLB, JComboBox fileCombox) throws InterruptedException {
        this.url = url;
        this.isMute = isMute;
        this.soundSlider = soundSlider;
        this.durationSlider = durationSlider;
        this.durationLB = durationLB;
        this.vinfo = vinfo;
        this.fileCombox = fileCombox;

        // Let's make sure that we can actually convert video pixel formats.
        if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)) {
            throw new RuntimeException("you must install the GPL version of Xuggler (with IVideoResampler support) for this demo to work");
        }

        // Create a Xuggler container object
        container = IContainer.make();
        // Open up the container
        if (container.open(url, IContainer.Type.READ, null) < 0) {
            throw new IllegalArgumentException("could not open file: " + url);
        }

        int numStreams = container.getNumStreams();

        int videoStreamId = -1;
        videoCoder = null;
        int audioStreamId = -1;
        audioCoder = null;

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

        if (videoStreamId == -1 && audioStreamId == -1) {
            throw new RuntimeException("could not find audio or video stream in container: " + url);
        }


        durationSlider.setMaximum((int) vinfo.vduration);
        durationSlider.setMinimum((int) vinfo.vstartTime);

        //init playback
        setTimeLabel(vinfo.duration);
        durationSlider.setValue(0);

        //setting dimension
        int width = videoCoder.getWidth();
        int height = videoCoder.getHeight();

        double xScale = psize.getWidth() / (double) width;
        double yScale = psize.getHeight() / (double) height;
        double scale = Math.min(xScale, yScale);    // scale to fit
        playerWidth = (int) (scale * width);
        playerHeight = (int) (scale * height);

        this.x = (int) ((psize.getWidth() - playerWidth) / 2);
        this.y = (int) ((psize.getHeight() - playerHeight) / 2);

        this.setBackground(Color.black);


        //System.out.println(playerWidth+":"+playerHeight);
        resampler = IVideoResampler.make(playerWidth, playerHeight, videoCoder.getPixelType(),
                videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());

        if (resampler == null) {
            throw new RuntimeException("could not create color space resampler for: " + url);
        }

        if (videoCoder != null) {
            if (videoCoder.open() < 0) {
                throw new RuntimeException("could not open video decoder for container: " + url);
            }
        }

        if (audioCoder != null) {
            if (audioCoder.open() < 0) {
                throw new RuntimeException("could not open audio decoder for container: " + url);
            }

            try {
                openJavaSound();
            } catch (LineUnavailableException ex) {
                throw new RuntimeException("unable to open sound device on your system when playing back container: " + url);
            }
        }

        queue = new ArrayBlockingQueue<Runnable>(15);
        threadPool = new ThreadPoolExecutor(100, 100,
                2, TimeUnit.SECONDS, queue);

        //set video param.
        isReady = true;
        pos = 0;
        forward = 0;

        seek(0, true, 0, false);
    }

    /**
     * 
     */
    public void togglePlay() {
        if (cstatus == status.PAUSE) {
            play();
        } else if (cstatus == status.PLAY) {
            pause();
        }
    }

    private void setTimeLabel(long duration) {
        //System.out.println(duration);
        int secs = (int) (duration / DEFAULT_PICTURE_BASE);
        int hours = secs / 3600,
                remainder = secs % 3600,
                minutes = remainder / 60,
                seconds = remainder % 60;

        String disHour = (hours < 10 ? "0" : "") + hours,
                disMinu = (minutes < 10 ? "0" : "") + minutes,
                disSec = (seconds < 10 ? "0" : "") + seconds;
        durationLB.setText("   " + disHour + ":" + disMinu + ":" + disSec + "   ");
    }

    public void run() {
        while (cstatus != status.DESTROY) {
            if (isReady) {
                decode();
            }
        }
        closeAll();
        return;
    }

    /**
     * 
     */
    public void close() {
        isReady = false;
        destroy();
    }

    private void decode() {
        try {
            mFirstVideoTimestampInStream = Global.NO_PTS;
            mSystemVideoClockStartTime = 0;
            ePicTimeStamp = -1;

            int bytesDecoded;
            boolean startFlag = true;
            IPacket packet = IPacket.make();
            while (container.readNextPacket(packet) >= 0) {
                if (cstatus == status.DESTROY) {
                    return;
                }

                if (stateTransition) {
                    System.out.println(cstatus);
                    stateTransition = false;
                    if (cstatus == status.PAUSE) {
                        isRunning = false;
                        mLine.stop();
                        //this.paintImmediately();
                        synchronized (this) {
                            try {
                                wait();
                            } catch (Exception e) {
                            }
                        }
                        if (cstatus == status.PLAY) {
                            mFirstVideoTimestampInStream = Global.NO_PTS;
                            mSystemVideoClockStartTime = 0;
                            ePicTimeStamp = -1;

                            mLine.start();
                            isRunning = true;
                        }
                    } else if (cstatus == status.SEEK) {
                        isRunning = false;
                        mLine.stop();
                        mLine.flush();
                        threadPool.shutdownNow();
                        threadPool.awaitTermination(2, TimeUnit.SECONDS);
                        queue = new ArrayBlockingQueue<Runnable>(15);
                        threadPool = new ThreadPoolExecutor(100, 100,
                                2, TimeUnit.SECONDS, queue);


                        dropCount = DEFAULT_ERROR_DUMP;
                        int lag = 0;
                        long modifier = 0;

                        if (forward != 0) {
                            modifier = packet.getTimeStamp() - vinfo.vstartTime + (int) (forward * vinfo.vframeRate * vinfo.durationPacket);
                            forward = 0;
                        } else {
                            modifier = pos;
                        }

                        if (modifier < DEFAULT_ERROR_DUMP * vinfo.durationPacket) {
                            pos = 0;
                            dropCount = (int) ((double) modifier / (double) vinfo.durationPacket);
                            if (dropCount < DEFAULT_BEGIN_DUMP) {
                                dropCount = DEFAULT_BEGIN_DUMP;
                            }
                            targetPos = pos + dropCount * vinfo.durationPacket;
                        } else if (modifier >= vinfo.vduration) {
                            int bufferTime = (int) (vinfo.vframeRate * 1.5); //about 1 sec to play before last frame
                            lag = (int) ((DEFAULT_ERROR_DUMP + bufferTime) * vinfo.durationPacket);
                            pos = (vinfo.vduration - lag);
                            targetPos = vinfo.vduration - bufferTime * vinfo.durationPacket;
                        } else {
                            lag = (DEFAULT_ERROR_DUMP) * vinfo.durationPacket;
                            pos = (modifier - lag);
                            targetPos = modifier;
                        }

                        if (pos < 0) {
                            pos = 0;
                            dropCount = DEFAULT_BEGIN_DUMP;
                            targetPos = pos + dropCount * vinfo.durationPacket;
                        }

                        System.out.println(pos + ":" + vinfo.durationPacket);
                        startFlag = false;
                        container.seekKeyFrame(vinfo.vstream, Long.MIN_VALUE, pos, Long.MAX_VALUE, IContainer.SEEK_FLAG_ANY);
                        continue;
                    }
                }




                if (packet.getStreamIndex() == vinfo.vstream) {

                    IVideoPicture picture = IVideoPicture.make(vinfo.vformat,
                            vinfo.vwidth, vinfo.vheight);

                    try {
                        bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
                        if (bytesDecoded < 0) {
                            throw new RuntimeException();
                        }
                    } catch (Exception e) {
                        continue;
                    }

                    if (packet.getTimeStamp() < vinfo.vstartTime) {
                        continue;
                    }


                    if (cstatus == status.SEEK && !stateTransition && packet.getTimeStamp() < (targetPos + vinfo.vstartTime)) {
                        //System.out.println(packet.getTimeStamp() + ":" + (targetPos + vinfo.vstartTime));
                        continue;
                    }


                    if (picture.isComplete()) {
                        if (!startFlag) {
                            if (!picture.isKeyFrame()) {
                                continue;
                            } else {
                                startFlag = true;
                                mLine.start();
                                mFirstVideoTimestampInStream = Global.NO_PTS;
                                mSystemVideoClockStartTime = 0;
                                ePicTimeStamp = -1;

                            }
                        }

                        IVideoPicture newPic = IVideoPicture.make(picture.getPixelType(), playerWidth, playerHeight);
                        if (resampler != null) {
                            if (resampler.resample(newPic, picture) < 0) {
                                throw new RuntimeException("could not resample video from: " + url);
                            }
                        }

                        if (cstatus == status.PLAY) {

                            if (ePicTimeStamp == -1) {
                                ePicTimeStamp = newPic.getTimeStamp();
                            } else {
                                long tts = newPic.getTimeStamp();
                                if (tts > ePicTimeStamp) {
                                    ePicTimeStamp = tts;
                                    //lastPicTimeStamp = tts;
                                } else {
                                    ePicTimeStamp += vinfo.picDurationPacket;
                                    if (tts - ePicTimeStamp > 0) {
                                        ePicTimeStamp = tts;
                                        //lastPicTimeStamp = tts;
                                    }
                                }
                            }

                            long delay = millisecondsUntilTimeToDisplay(ePicTimeStamp);
                            if (delay < 0) {
                                //temp fix
                                mSystemVideoClockStartTime -= delay;
                                //System.out.println("delay :" + delay);
                            }
                            try {
                                if (delay > 0) {
                                    Thread.sleep(delay);
                                }
                            } catch (InterruptedException e) {
                                return;
                            }


                        }

                        setImage(Utils.videoPictureToImage(newPic));

                        if (cstatus == status.PLAY || (cstatus == status.SEEK && !stateTransition)) {
                            long pts = picture.getTimeStamp();
                            if (cstatus == status.PLAY) {
                                pts = ePicTimeStamp;
                            }

                            int cpacketTime = (int) Math.ceil(pts * vinfo.toPacketTB);
                            if (!durationSlider.getValueIsAdjusting()) {
                                synchronized (this) {
                                    durationSlider.setValue(cpacketTime);
                                }
                            }
                            long remain = (vinfo.duration - pts);
                            setTimeLabel(remain);
                        }

                        if (cstatus == status.SEEK && !stateTransition) {
                            if (!seekAndPlay) {
                                pause();
                            } else {
                                play();
                            }
                        }

                    }

                } else if (packet.getStreamIndex() == vinfo.astream) {

                    if (cstatus == status.SEEK && !stateTransition && packet.getTimeStamp() < (targetPos + vinfo.vstartTime)) {
                        continue;
                    }
                    if (packet.getTimeStamp() < vinfo.vstartTime) {
                        continue;
                    }

                    if (!startFlag) {
                        continue;
                    }

                    IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

                    int offset = 0;
                    while (offset < packet.getSize()) {

                        try {
                            bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                            if (bytesDecoded < 0) {
                                throw new RuntimeException();
                            }
                            offset += bytesDecoded;
                        } catch (Exception e) {
                            //System.out.println("aduio");
                            break;
                        }

                        if (samples.isComplete() && ePicTimeStamp != -1 && cstatus == status.PLAY) {
                            //System.out.println("a:" + samples.getTimeStamp() + ", p:" + ePicTimeStamp);
                            //synchroniz sound with video
                            long delaySample = millisecondsUntilTimeToDisplay(samples.getTimeStamp());

                            if (delaySample < 0) {
                                playJavaSound(samples);
                            } else {
                                threadPool.execute(new SoundSynchronizer(delaySample, samples));
                            }
                            //if (delaySample < 0) System.out.println("a_delay:" + delaySample);
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
        if (cstatus != status.DESTROY) {
            int totalVideo = fileCombox.getItemCount();
            if (totalVideo > 0) {
                if (totalVideo == 1) {
                    rewind();
                } else {
                    //next video
                    int nextIndex = fileCombox.getSelectedIndex() + 1;
                    if (nextIndex == totalVideo) {
                        nextIndex = 0;
                    }
                    fileCombox.setSelectedIndex(nextIndex);
                }
            }
        }
    }

    private class SoundSynchronizer implements Runnable {

        long ms;
        IAudioSamples samples;

        public SoundSynchronizer(long ms, IAudioSamples samples) {
            this.ms = ms;
            this.samples = samples;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(ms);
                if (cstatus == status.PLAY) {
                    playJavaSound(samples);
                }
            } catch (InterruptedException ex) {
                //Logger.getLogger(VideoPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * 
     */
    public void rewind() {
        closeAll();

        // Create a Xuggler container object
        container = IContainer.make();
        // Open up the container
        if (container.open(url, IContainer.Type.READ, null) < 0) {
            throw new IllegalArgumentException("could not open file: " + url);
        }

        int numStreams = container.getNumStreams();

        int videoStreamId = -1;
        videoCoder = null;
        int audioStreamId = -1;
        audioCoder = null;

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

        //init playback
        setTimeLabel(this.vinfo.duration);
        durationSlider.setValue(0);

        if (videoStreamId == -1 && audioStreamId == -1) {
            throw new RuntimeException("could not find audio or video stream in container: " + url);
        }

        if (videoCoder != null) {
            if (videoCoder.open() < 0) {
                throw new RuntimeException("could not open video decoder for container: " + url);
            }
        }

        if (audioCoder != null) {
            if (audioCoder.open() < 0) {
                throw new RuntimeException("could not open audio decoder for container: " + url);
            }

            try {
                openJavaSound();
            } catch (LineUnavailableException ex) {
                throw new RuntimeException("unable to open sound device on your system when playing back container: " + url);
            }
        }

        queue = new ArrayBlockingQueue<Runnable>(15);
        threadPool = new ThreadPoolExecutor(100, 100,
                2, TimeUnit.SECONDS, queue);
        //set video param.
        isReady = true;
        pos = 0;
        forward = 0;

        seek(0, false, 0, false);
    }

    /**
     * 
     */
    public void play() {
        if (cstatus != status.PLAY) {
            cstatus = status.PLAY;
            stateTransition = true;
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * 
     */
    public void destroy() {
        if (cstatus != status.DESTROY) {
            cstatus = status.DESTROY;
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * 
     */
    public void pause() {
        if (cstatus != status.PAUSE) {
            cstatus = status.PAUSE;
            stateTransition = true;
        }
    }

    /**
     * 
     * @param pos
     * @param play
     * @param forward
     * @param isForward
     */
    public final void seek(long pos, boolean play, int forward, boolean isForward) {
        if (cstatus != status.SEEK) {
            if (isForward) {
                this.forward = forward;
            } else {
                this.pos = pos;
            }
            seekAndPlay = play;
            cstatus = status.SEEK;
            stateTransition = true;
            synchronized (this) {
                notify();
            }
        }
    }

    void toggleSound(boolean isMute) {
        this.isMute = isMute;
    }

    private void closeAll() {
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
        closeJavaSound();
        threadPool.shutdown();
        isReady = false;
    }

    /**
     * 
     * @return
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * 
     * @param image
     */
    public void setImage(Image image) {
        SwingUtilities.invokeLater(new ImageRunnable(image));
    }

    @Override
    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (itd != null) {
            g.drawImage(itd, x, y, this);
        }
    }

    private long millisecondsUntilTimeToDisplay(long ts) {
        /**
         * We could just display the images as quickly as we decode them, but it turns
         * out we can decode a lot faster than you think.
         * 
         * So instead, the following code does a poor-man's version of trying to
         * match up the frame-rate requested for each IVideoPicture with the system
         * clock time on your computer.
         * 
         * Remember that all Xuggler IAudioSamples and IVideoPicture objects always
         * give timestamps in Microseconds, relative to the first decoded item.  If
         * instead you used the packet timestamps, they can be in different units depending
         * on your IContainer, and IStream and things can get hairy quickly.
         */
        long millisecondsToSleep = 0;
        if (mFirstVideoTimestampInStream == Global.NO_PTS) {
            // This is our first time through
            mFirstVideoTimestampInStream = ts;
            // get the starting clock time so we can hold up frames
            // until the right time.
            mSystemVideoClockStartTime = System.currentTimeMillis();
            millisecondsToSleep = 0;
        } else {
            long systemClockCurrentTime = System.currentTimeMillis();
            long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - mSystemVideoClockStartTime;
            // compute how long for this frame since the first frame in the stream.
            // remember that IVideoPicture and IAudioSamples timestamps are always in MICROSECONDS,
            // so we divide by 1000 to get milliseconds.
            long millisecondsStreamTimeSinceStartOfVideo = (ts - mFirstVideoTimestampInStream) / 1000;
            final long millisecondsTolerance = 50; // and we give ourselfs 50 ms of tolerance
            millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo
                    - (millisecondsClockTimeSinceStartofVideo + millisecondsTolerance));
        }
        return millisecondsToSleep;
    }

    private void openJavaSound() throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat((float) vinfo.asampleRate,
                (int) IAudioSamples.findSampleBitDepth(vinfo.aformat),
                vinfo.achannels,
                true, /* xuggler defaults to signed 16 bit samples */
                false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        mLine = (SourceDataLine) AudioSystem.getLine(info);

        mLine.open(audioFormat);
    }

    private void playJavaSound(IAudioSamples aSamples) {
        double vl = soundSlider.getValue() / 100.0;
        if (isMute) {
            vl = 0;
        }
        ShortBuffer buffer =
                aSamples.getByteBuffer().asShortBuffer();

        for (int i = 0; i < buffer.limit(); ++i) {
            buffer.put(i, (short) (buffer.get(i) * vl));
        }

        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());

        mLine.write(rawBytes, 0, aSamples.getSize());
    }

    private void closeJavaSound() {
        if (mLine != null) {
            mLine.drain();
            mLine.close();
            mLine = null;
        }
    }

    private class ImageRunnable implements Runnable {

        private final Image newImage;

        public ImageRunnable(Image newImage) {
            this.newImage = newImage;
        }

        public void run() {
            itd = newImage;
            repaint();
        }
    }

    /**
     * 
     * @param url
     * @param fileName
     * @param videoDirectory
     * @return
     */
    public static VideoInfo createVInfo(String url, String fileName, String videoDirectory) {
        // Let's make sure that we can actually convert video pixel formats.
        VideoInfo vinfo = null;

        // Create a Xuggler container object
        IContainer container = IContainer.make();
        // Open up the container
        if (container.open(url, IContainer.Type.READ, null) < 0) {
            throw new IllegalArgumentException("could not open file: " + url);
        }

        int numStreams = container.getNumStreams();

        vinfo = new VideoInfo(container.getNumStreams(), container.getDuration(), container.getFileSize(), container.getBitRate());
        vinfo.url = url;
        vinfo.fileName = fileName;
        vinfo.videoDirectory = videoDirectory;

        int videoStreamId = -1;
        IStreamCoder videoCoder = null;
        int audioStreamId = -1;
        IStreamCoder audioCoder = null;

        for (int i = 0; i < numStreams; i++) {
            // Find the stream object
            IStream stream = container.getStream(i);

            // Get the pre-configured decoder that can decode this stream;
            IStreamCoder coder = stream.getStreamCoder();

            if (videoStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
                //public CoderInfo(int stream, String type, String codec, int duration, int startTime, int timeBase, int coderTimeBase){
                vinfo.setVGeneralInfo(i, coder.getCodecType().toString(), coder.getCodec(), stream.getDuration(), stream.getStartTime(),
                        stream.getTimeBase().getDenominator(), coder.getTimeBase().getDenominator());
                //public void setVideoInfo(int rate, String format, int width, int height){
                vinfo.setVideoInfo(coder.getFrameRate().getDouble(), coder.getPixelType(), coder.getWidth(), coder.getHeight());
            } else if (audioStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamId = i;
                audioCoder = coder;
                //public CoderInfo(int stream, String type, String codec, int duration, int startTime, int timeBase, int coderTimeBase){
                vinfo.setAGeneralInfo(i, coder.getCodecType().toString(), coder.getCodec(), stream.getDuration(), stream.getStartTime(),
                        stream.getTimeBase().getDenominator(), coder.getTimeBase().getDenominator());
                //public void setAudioInfo(double rate, String format, int channels){
                vinfo.setAudioInfo(coder.getSampleRate(), coder.getSampleFormat(), coder.getChannels());
            }
        }

        if (videoStreamId == -1 && audioStreamId == -1) {
            throw new RuntimeException("could not find audio or video stream in container: " + url);
        }


        vinfo.toPacketTB = vinfo.vtimeBase / (double) DEFAULT_PICTURE_BASE; //packetTB/pictureTB
        vinfo.toPictureTB = DEFAULT_PICTURE_BASE / (double) vinfo.vtimeBase;

        //convert container duration to istrea duration as its bugged in some video format
        vinfo.vduration = (long) Math.ceil(vinfo.duration * vinfo.toPacketTB);

        vinfo.durationPacket = (int) Math.ceil(vinfo.vtimeBase / vinfo.vframeRate);
        vinfo.picDurationPacket = (int) Math.ceil(DEFAULT_PICTURE_BASE / vinfo.vframeRate);


        if (videoCoder != null) {
            if (videoCoder.open() < 0) {
                throw new RuntimeException("could not open video decoder for container: " + url);
            }
        }

        if (audioCoder != null && audioCoder.open() < 0) {
            throw new RuntimeException("could not open audio decoder for container: " + url);
        }

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

        return vinfo;
    }
}
