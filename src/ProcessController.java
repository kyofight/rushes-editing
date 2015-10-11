/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartPanel;

/**
 *
 * @author kyo
 */
public class ProcessController extends Thread {

    double progressPercent;
    ArrayList<Double> frameDiff;
    ArrayList<Long> timeline;
    ArrayList<Long> cutTS;
//    private final static int BAD_FRAME_FLAG = -2;
//    private final static int CUT_FLAG = -1;
    private final VideoInfo vinfo;
    private final Options options;
    private ChartPanel chartPanel;
    private Processor processes[];
    private JComboBox fileCombox;
    private JPanel scrollInternalPanel;
    private Dimension ThumbSize;
    private KeyFramesGrabber kfGrabber[];
    /**
     * 
     */
    public static final int wgap = 10; //thumbnail gaps
    /**
     * 
     */
    public static final int colCount = 5; //thumbnail row count of thumbnails
    private ImagePanel thumbs[];
    private FDChart chartThread;
    private VideoEncoder[] encoders;
    private final JProgressBar pb;
    private final JLabel plb;
    /**
     * 
     */
    public static Double overallProgress;
    private boolean isProcessing;
    private String outputDir;
    private final int featureThreshold;

    ProcessController(VideoInfo vinfo, Options options, ChartPanel chartPanel, JComboBox fileCombox, JPanel scrollInternalPanel, JProgressBar pb, JLabel plb) {
        this.vinfo = vinfo;
        this.options = options;
        this.chartPanel = chartPanel;
        this.fileCombox = fileCombox;
        this.scrollInternalPanel = scrollInternalPanel;
        this.isProcessing = true;
        this.outputDir = "";
        this.pb = pb;
        this.plb = plb;
        ProcessController.overallProgress = 0.0;
        featureThreshold = options.keyframeSegments * options.keyframeSegments * options.featuresInSimilarScene;


        this.frameDiff = new ArrayList<Double>();
        this.timeline = new ArrayList<Long>();
        this.cutTS = new ArrayList<Long>();
    }

    void shutDown() {
        chartPanel.getChart().getXYPlot().setNoDataMessage("Video is not Processed");
        this.interrupt();

        if (processes != null) {
            for (int i = 0; i < processes.length; i++) {
                if (processes[i] != null) {
                    processes[i].interrupt();
                }
            }
        }

        if (chartThread != null) {
            chartThread.interrupt();
        }

        if (kfGrabber != null) {
            for (int i = 0; i < kfGrabber.length; i++) {
                if (kfGrabber[i] != null) {
                    kfGrabber[i].interrupt();
                }
            }
        }

        if (encoders != null) {
            for (int i = 0; i < encoders.length; i++) {
                if (encoders[i] != null) {
                    encoders[i].interrupt();
                }
            }
        }
    }

    /**
     * 
     * @return
     */
    public synchronized boolean isProcessing() {
        return isProcessing;
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        long calTime = 0;
        long encodeTime = 0;
        int sizeWithJunk = 0;
        String segInfo = "";
        long intershotTime = 0;
        try {

            ProcessController.overallProgress = 0.0;
            pb.setValue(0);
            pb.setString("0%");
            plb.setText("Initializing Decoding");

            processes = new Processor[options.numThread[options.algIndex]];
            for (int i = 0; i < options.numThread[options.algIndex]; i++) {
                processes[i] = new Processor(vinfo, options, i, pb, plb);
                processes[i].setPriority(Thread.MAX_PRIORITY - i);
                processes[i].start();
            }

            pb.setString("0%");
            plb.setText("Shot Boundary Detection / Junk Frame Removal");

            for (int i = 0; i < options.numThread[options.algIndex]; i++) {
                try {
                    processes[i].join();
                    frameDiff.addAll(processes[i].frameDiff);
                    //System.out.println("i :"+i +", "+ frameDiff.size());
                    if (i != (options.numThread[options.algIndex] - 1)) {
                        processes[i].tmeline.remove(processes[i].tmeline.size() - 1);
                    }
                    timeline.addAll(processes[i].tmeline);
                    cutTS.addAll(processes[i].cutTS);
                } catch (Exception ex) {
                    Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, ex);
                    isProcessing = false;
                    return;
                }
            }

            calTime = System.currentTimeMillis() - time;



            //System.out.println("timeline size:"+timeline.size());
        /* debug use */
//        ArrayList<Double> frameDiffE = new ArrayList<Double>();
//        options.numThread = 1;
//        Processor processesE = new Processor(vinfo, options, 0);
//        processesE.start();
//        try {
//            processesE.join();
//            frameDiffE.addAll(processesE.frameDiff);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        for(int i=0; i<frameDiffE.size(); i++){
//            if(frameDiffE.get(i).compareTo(frameDiff.get(i)) !=0){
//                System.out.println("Diff index: "+i +", Value: "+frameDiff.get(i)+" - "+frameDiffE.get(i));
//            }
//        }
//        int k = 0;
//        for(int i=0; i<frameDiff.size(); i++){
//            if(frameDiff.get(i) <= 10){
//                System.out.println("small :"+frameDiff.get(i) +" index"+i);
//            }
//        }
//        System.out.println("cutTS size: " + cutTS.size());

            //remove duplicate for the case three consecutive cut with color, color, color frames 
//        Set setItems = new LinkedHashSet(cutTS);
//        cutTS.clear();
//        cutTS.addAll(setItems);
            System.out.println("cutTS size: " + cutTS.size());

            int size = cutTS.size();
            if (size % 2 != 0) {
                cutTS.remove(size - 1);
            }

//        for (int i = 0; i < cutTS.size(); i++) {
//            System.out.println("cut :" + cutTS.get(i));
//        }
//        System.out.println("cut size: " + cutTS.size());
        /* debug use end */

            //System.out.println("cutTS size: " + cutTS.size());
//        
            //remove duration > options.minDuration
            int secDuration = (int) (vinfo.durationPacket * vinfo.vframeRate * options.minDuration / 1000.0f);
            //System.out.println(vinfo.durationPacket+" D "+vinfo.vframeRate);
            //System.out.println("framze rate dur: " + secDuration);
            long startTS = -1;
            long endTS = -1;
            sizeWithJunk = cutTS.size();
            for (int i = 0; i < cutTS.size(); i++) {
                if (i % 2 == 0) {
                    startTS = cutTS.get(i);
                } else {
                    endTS = cutTS.get(i);
                }
                if (startTS != -1 && endTS != -1) {
                    int duration = (int) (endTS - startTS);
                    //System.out.println(duration+" C "+secDuration);

                    if (secDuration >= duration) {
                        //System.out.println(secDuration+ " | " + duration);
                        cutTS.remove(i);
                        cutTS.remove(i - 1);
                        i -= 2;
                        size -= 2;
                    }//else
                    //System.out.println(secDuration+ " | " + duration + ", " +startTS + "," + endTS);
                    startTS = -1;
                    endTS = -1;
                }
            }

            //System.out.println("cutTS size: " + cutTS.size());
            if (Thread.interrupted()) {
                // We've been interrupted: no more crunching.
                isProcessing = false;
                return;
            }

            int cd = 0;
            
            String outputFolder = vinfo.fileName + "_" + options.alg.replaceAll(" ", ".");
            String outputPath = options.outputDir;
            //System.out.println("outpath:" + outputPath);
            if ("".equals(outputPath)) {
                outputPath = vinfo.videoDirectory;
            }
            outputDir = outputPath + System.getProperty("file.separator") + outputFolder;
            
            
            String tmpDir = outputDir;
            do{
                cd++;
                outputDir = tmpDir + "_" + cd;
            }while(new File(outputDir).exists());
            
            File outFile = new File(outputDir);
            if (!outFile.mkdir()) {
                throw new RuntimeException("could not create output directory " + outputDir);
            }

            if (cutTS.size() > 0) {
                if (Thread.interrupted()) {
                    // We've been interrupted: no more crunching.
                    isProcessing = false;
                    return;
                }

                //intershot redundancy starting time
                time = System.currentTimeMillis();
                final int totalSeg = cutTS.size() / 2;

                ProcessController.overallProgress = 0.0;
                pb.setValue(0);
                pb.setString("0%");
                plb.setText("Inter-Shot Redundancy Removal (Keyframes Grabbing)");

                if (totalSeg > 1) {

                    /********** start of clustering ********/
                    ArrayList<Integer> shotSequences = new ArrayList<Integer>();
                    ArrayList<Integer> representativeSequence = new ArrayList<Integer>();

                    kfGrabber = new KeyFramesGrabber[cutTS.size() / 2];
                    //encode video with segments of threads
                    for (int i = 0; i < cutTS.size(); i++) {
                        if (i % 2 == 0) {
                            startTS = cutTS.get(i);
                        } else {
                            endTS = cutTS.get(i);
                        }
                        if (startTS != -1 && endTS != -1) {
                            kfGrabber[i / 2] = new KeyFramesGrabber(cutTS.size() / 2, (i / 2), startTS, endTS, vinfo, options, pb, plb);
                            kfGrabber[i / 2].start();
                            startTS = -1;
                            endTS = -1;
                        }
                    }

                    for (int i = 0; i < totalSeg; i++) {
                        try {
                            kfGrabber[i].join();
                            shotSequences.add(kfGrabber[i].id);
                            //System.out.println("kf:" + kfGrabber[i].keyframes.size());
                        } catch (Exception ex) {
                            Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, ex);
                            isProcessing = false;
                            return;
                        }
                    }

                    ProcessController.overallProgress = 0.0;
                    pb.setValue(0);
                    pb.setString("0%");
                    plb.setText("Inter-Shot Redundancy Removal (InterShot Clustering)");
                    //clustering
                    FeatureComparison fc = new FeatureComparison();

                    ListIterator<Integer> seqLitr = shotSequences.listIterator();
                    while (seqLitr.hasNext()) {
                        Integer seq = seqLitr.next();
                        //System.out.println("first" + seq );
                        seqLitr.remove();

                        ArrayList<BufferedImage> currentShotKeyframes = kfGrabber[seq].keyframes;
                        int csize = currentShotKeyframes.size();
                        int currentClusterLargestSequence = kfGrabber[seq].id;

                        setProgress(shotSequences.size(), totalSeg);

                        /*** compare with other shots ***/
                        ListIterator<Integer> seqLitrInner = shotSequences.listIterator();
                        while (seqLitrInner.hasNext()) {
                            Integer oseq = seqLitrInner.next();
                            //System.out.println("second" + oseq );

                            ArrayList<BufferedImage> otherShotKeyframes = kfGrabber[oseq].keyframes;
                            int osize = otherShotKeyframes.size();
                            //store number of matching feature points
                            int totalMatches = 0;

                            for (int c = 0; c < csize; c++) {
                                for (int o = 0; o < osize; o++) {
                                    totalMatches += fc.compare(currentShotKeyframes.get(c), otherShotKeyframes.get(o));
                                }
                            }

                            System.out.println("feature match: (" + seq + "," + oseq + ") " + totalMatches);

                            if (totalMatches > featureThreshold) {
                                if (oseq > currentClusterLargestSequence) {
                                    currentClusterLargestSequence = oseq;
                                }
                                seqLitrInner.remove();
                                //reset reference
                                seqLitr = shotSequences.listIterator();
                            }

                            setProgress(shotSequences.size(), totalSeg);
                        }

                        representativeSequence.add(currentClusterLargestSequence);

                    }

                    System.out.println(representativeSequence.toString());


                    //filter the segments
                    cutTS.clear();
                    int rcSize = representativeSequence.size();
                    for (int i = 0; i < rcSize; i++) {
                        long rstart = kfGrabber[representativeSequence.get(i)].startTS;
                        long rend = kfGrabber[representativeSequence.get(i)].endTS;
                        cutTS.add(rstart);
                        cutTS.add(rend);
                    }
                }

                intershotTime = System.currentTimeMillis() - time;
                /********** end of clustering ********/
                //generate chart
                chartThread = new FDChart(outputDir, options, frameDiff, cutTS, timeline, vinfo, this.chartPanel);
                chartThread.start();


                //encode starting time
                time = System.currentTimeMillis();

                pb.setValue(0);
                pb.setString("0%");
                plb.setText("Initializing Encoding");

                scrollInternalPanel.removeAll();

                int thumbHeight = scrollInternalPanel.getHeight();
                int thumbWidth = (scrollInternalPanel.getWidth() - (colCount * wgap)) / colCount;

                ThumbSize = new Dimension(thumbWidth, thumbHeight);

                int numRows = (int) Math.ceil(cutTS.size() / (double) (2 * colCount));
                this.scrollInternalPanel.setLayout(new GridLayout(numRows, colCount, wgap, wgap));

                int totalThumbs = numRows * colCount;
                thumbs = new ImagePanel[totalThumbs];
                for (int i = 0; i < totalThumbs; i++) {
                    thumbs[i] = new ImagePanel();
                    scrollInternalPanel.add(thumbs[i]);
                }

                this.scrollInternalPanel.setBackground(new Color(33, 33, 33));
                this.scrollInternalPanel.setPreferredSize(new Dimension(scrollInternalPanel.getWidth() - (colCount * wgap), thumbHeight * numRows));

                scrollInternalPanel.doLayout();
                scrollInternalPanel.repaint();
                scrollInternalPanel.revalidate();

                if (Thread.interrupted()) {
                    // We've been interrupted: no more crunching.
                    isProcessing = false;
                    return;
                }

                ProcessController.overallProgress = 0.0;
                pb.setValue(0);
                pb.setString("0%");
                plb.setText("Encoding Scenes");

                int totalReprensetativeSeg = cutTS.size() / 2;
                encoders = new VideoEncoder[cutTS.size() / 2];
                //encode video with segments of threads
                for (int i = 0; i < cutTS.size(); i++) {
                    if (i % 2 == 0) {
                        startTS = cutTS.get(i);
                    } else {
                        endTS = cutTS.get(i);
                    }
                    if (startTS != -1 && endTS != -1) {
                        encoders[i / 2] = new VideoEncoder(cutTS.size() / 2, (i / 2) + 1, thumbs[i / 2], startTS, endTS, ThumbSize, outputDir, vinfo, options, fileCombox, scrollInternalPanel, pb, plb);
                        encoders[i / 2].start();

                        segInfo += "Scene " + ((i / 2) + 1) + "-> Start: " + getPictureTime((long) (startTS * vinfo.toPictureTB)) + ", End: " + getPictureTime((long) (endTS * vinfo.toPictureTB)) + "\r\n";

                        startTS = -1;
                        endTS = -1;
                    }
                }


                for (int i = 0; i < totalReprensetativeSeg; i++) {
                    try {
                        encoders[i].join();
                        encoders[i].addThumbAndCombox();
                        encoders[i].setProgress();
                    } catch (Exception ex) {
                        Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, ex);
                        isProcessing = false;
                        return;
                    }
                }

                encodeTime = System.currentTimeMillis() - time;

            } else {
                pb.setValue(0);
                pb.setString("Pending");
                plb.setText("Process Complete");

                chartPanel.getChart().getXYPlot().setNoDataMessage("No segement is found");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    e.toString(),
                    "Errors",
                    JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, e);
        }


        System.out.println(System.currentTimeMillis() - time);

        if (Thread.interrupted()) {
            // We've been interrupted: no more crunching.
            isProcessing = false;
            return;
        }

        //writing log file
        if (!"".equals(outputDir) && new File(outputDir).exists()) {
            try {
                String fileLocation = outputDir + "/log.txt";
                //set up file output stream
                FileOutputStream outputFile = new FileOutputStream(fileLocation);
                PrintStream file = new PrintStream(outputFile);


                DecimalFormat df = new DecimalFormat("#.###");
                double calTimeSec = (calTime / 1000.0);
                double encodeTimeSec = (encodeTime / 1000.0);
                double interTimeSec = (intershotTime / 1000.0);

                file.println(""
                        + "Date: " + new SimpleDateFormat("yyyy.MMMMM.dd,  H:mm:ss").format(new Date()) + "\r\n\r\n"
                        + "=========================Process File Information=============================\r\n"
                        + "File Path: " + vinfo.videoDirectory + "\r\n"
                        + "File Name: " + vinfo.fileName + "\r\n"
                        + "File Size(bytes): " + vinfo.fileSize + "\r\n"
                        + "Num. of Streams: " + vinfo.numStreams + "\r\n"
                        + "BitRate: " + vinfo.bitRate + "\r\n"
                        + "Duration: " + getPictureTime(vinfo.duration) + "\r\n"
                        + "Frame Size: " + vinfo.vwidth + " x " + vinfo.vheight + "\r\n\r\n\r\n"
                        + "========================Process Information===================================\r\n"
                        + "Number of Representative Clips found: " + (cutTS.size() / 2) + "\r\n"
                        + "Number of Junk Shots dumped: " + ((sizeWithJunk - cutTS.size()) / 2) + "\r\n"
                        + "Algorithm: " + options.alg + "\r\n"
                        + "Threshold: " + options.threshold[options.algIndex] + "\r\n"
                        + "Threshold (Features Matching): " + featureThreshold + "\r\n"
                        + "Number of Threads: " + options.numThread[options.algIndex] + "\r\n"
                        + "Junk Shots duration (ms): " + options.minDuration + "\r\n"
                        + "Junk Frame Matching Tolerance: " + options.tolerance[options.algIndex] + "%\r\n\r\n\r\n"
                        + "========================Process Time Information===================================\r\n"
                        + "Total Time: " + df.format(calTimeSec + interTimeSec + encodeTimeSec) + "(s)\r\n"
                        + ">> Shot Boundary Detection/Junk Frames Removal: " + df.format(calTimeSec) + "(s)\r\n"
                        + ">> Inter-Shot Redundancy Removal " + df.format(interTimeSec) + "(s)\r\n"
                        + ">> Encoding:" + df.format(encodeTimeSec) + "(s)\r\n\r\n\r\n"
                        + "========================Representative Clips Information===================================\r\n"
                        + segInfo);
                try {
                    //close file
                    outputFile.close();
                    file.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null,
                            ex.toString(),
                            "Errors",
                            JOptionPane.ERROR_MESSAGE);
                    Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(null,
                        ex.toString(),
                        "Errors",
                        JOptionPane.ERROR_MESSAGE);
                Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        isProcessing = false;
    }

    private String getPictureTime(long duration) {
        //System.out.println(duration);
        int secs = (int) (duration / VideoPlayer.DEFAULT_PICTURE_BASE);
        int hours = secs / 3600,
                remainder = secs % 3600,
                minutes = remainder / 60,
                seconds = remainder % 60,
                ms = (int) (duration / VideoPlayer.DEFAULT_PICTURE_BASE * 1000) % 1000;

        String disHour = (hours < 10 ? "0" : "") + hours,
                disMinu = (minutes < 10 ? "0" : "") + minutes,
                disSec = (seconds < 10 ? "0" : "") + seconds,
                disMs = (ms < 100 ? (ms < 10 ? "00" : "0") : "") + ms;
        return disHour + ":" + disMinu + ":" + disSec + "." + disMs;
    }

    /**
     * 
     * @param i
     * @param active
     */
    public void setThumbPlayStatus(int i, boolean active) {
        try {
            if (thumbs[i] != null) {
                thumbs[i].setActive(active);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                e.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    public void setProgress(final int ccs, final int totalSeg) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                synchronized (ProcessController.this) {
                    ProcessController.overallProgress = (1 - ccs / (double) totalSeg) * 100;
                    int percent = (int) Math.ceil(ProcessController.overallProgress);
                    pb.setValue(percent);
                    pb.setString(percent + "%");
                }
            }
        });
    }

    /**
     * 
     */
    public class ImagePanel extends JPanel implements MouseMotionListener, MouseListener {

        private BufferedImage image;
        private BufferedImage imageWithPlay;
        private BufferedImage imgToDraw;
        Font f = new Font("SansSerif", Font.BOLD, 20);
        FontMetrics fm;
        int fh, ascent;
        int space;
        Dimension d;
        private BufferedImage playIcon;
        private int x, y;
//        private final JPopupMenu tooltipFrame;
        //private String tooltip;
        private String title;
        private String duration;
        private final static int tooltipHeight = 40;
        private final static int tooltipHeightPadding = 10;
        private String path;
        private boolean isActive;

        /**
         * 
         */
        public ImagePanel() {
//            tooltipFrame = new JPopupMenu();
//            tooltipFrame.setLayout(new BorderLayout());
//            tooltipFrame.setVisible(false);
//            tooltipFrame.setBackground(Color.DARK_GRAY);
            this.setBackground(Color.darkGray);
            this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.isActive = false;
        }

        /**
         * 
         * @param image
         * @param imageWithPlay
         * @param x
         * @param y
         * @param title
         * @param duration
         * @param path
         */
        public void drawThumb(BufferedImage image, BufferedImage imageWithPlay, int x, int y, String title, String duration, String path) {
            this.addMouseListener(this);
            this.addMouseMotionListener(this);

            this.path = path;
            this.x = x;
            this.y = y;
            this.image = image;
            this.imageWithPlay = imageWithPlay;
            this.title = title;
            this.duration = duration;
            //tooltipFrame.add(new JLabel(tooltip), BorderLayout.CENTER);


            URL url = getClass().getResource("/fyprushediting/resources/Button-Play-icon.png");
            try {
                playIcon = ImageIO.read(url);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                e.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
                throw new RuntimeException("Could not open file");
            }

            RescaleOp rescaleOp = new RescaleOp(0.3f, 15, null);
            rescaleOp.filter(imageWithPlay, imageWithPlay);

            // get the graphics for the image
            Graphics2D g = imageWithPlay.createGraphics();

            int w = this.image.getWidth();
            int h = this.image.getHeight();

            g.drawImage(playIcon, (w - playIcon.getWidth()) / 2, (h - playIcon.getHeight()) / 2, null);

            drawString2(g);

            drawString2(image.createGraphics());


            imgToDraw = this.image;

            repaint();
        }

        /**
         * 
         * @param g
         */
        public void drawString2(Graphics g) {
            int w = this.image.getWidth();
            int h = this.image.getHeight();

            Rectangle2D bounds = new Rectangle2D.Float(0, 0, w, 20);
            Color color = new Color(0.1f, 0.1f, 0.1f, 0.7f); //Red 
            g.setColor(color);
            g.fillRect(0, 0, w, tooltipHeight);
            g.fillRect(0, h - tooltipHeight, w, tooltipHeight);

            g.setFont(f);
            fm = g.getFontMetrics();
            int titleWidth = fm.stringWidth(title);
            int durationWidth = fm.stringWidth(duration);
            g.setColor(Color.green);
            g.drawString(title, (w - titleWidth) / 2, tooltipHeight - tooltipHeightPadding);
            g.setColor(Color.green);
            g.drawString(duration, (w - durationWidth) / 2, this.image.getHeight() - tooltipHeightPadding);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imgToDraw != null) {
                g.drawImage(imgToDraw, x, y, this); // see javadoc for more info on the parameters
            }
        }

        public synchronized void mouseClicked(MouseEvent e) {
            fileCombox.setSelectedItem(path);
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            //tooltipFrame.setVisible(true);
            if (!this.isActive) {
                imgToDraw = imageWithPlay;
                repaint();
            }
        }

        public void mouseExited(MouseEvent e) {
            //tooltipFrame.setVisible(false);
            if (!this.isActive) {
                imgToDraw = image;
                repaint();
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            //tooltipFrame.setLocation(e.getXOnScreen(),e.getYOnScreen()-tooltipFrame.getHeight());
        }

        private void setActive(boolean b) {
            this.isActive = b;
            if (this.isActive) {
                imgToDraw = imageWithPlay;
            } else {
                imgToDraw = image;
            }
            repaint();
        }
    }
}
