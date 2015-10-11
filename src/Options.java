/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import com.xuggle.xuggler.video.ConverterFactory;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.FFMPEGLocator;
import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 *
 * @author kyo
 */
public class Options implements Serializable {

    //int numThread;
    //int threshold;
    //int tolerance;
    int minDuration;
    String outputDir;
    ArrayList<BufferedImage> dumpImg;
    String formats[];
    String selectedFormat;
    String dumpPath;
    int chartWidth;
    int chartHeight;
    /**
     * 
     */
    public final static String algorithms[] = {"Naive Similarity", "Pixel Comparison"}; //ms-ssim
    /**
     * 
     */
    public int threshold[] = {1200, 100};
    /**
     * 
     */
    public int tolerance[] = {40, 40};
    /**
     * 
     */
    public int numThread[] = {3, 3};
    String alg;
    int algIndex;
    int keyframeSegments;
    int featuresInSimilarScene;

    /**
     * 
     */
    public Options() {
        //shot boundary
        this.algIndex = 0;
        this.alg = algorithms[this.algIndex];
//        this.numThread[] = {3, 3};
//        this.threshold[] = {1200, 1000};


        //junk
//        this.tolerance[] = {100, 100};
        this.minDuration = 300;
        this.dumpPath = new File("").getAbsolutePath() + System.getProperty("file.separator") + "dump" + System.getProperty("file.separator");

        //intershot
        this.keyframeSegments = 5;
        this.featuresInSimilarScene = 5;

        //output
        this.outputDir = "";//default is the same as video path directory
        Encoder encoder = new Encoder();
        
        try {
            formats = encoder.getSupportedEncodingFormats();
        } catch (EncoderException ex) {
            JOptionPane.showMessageDialog(null,
                ex.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex);
        }

        selectedFormat = "avi";
        this.chartWidth = 1920;
        this.chartHeight = 1080;

    }

    /**
     * 
     */
    public void readOptions() {
        ObjectInputStream inputStream = null;

        try {

            //Construct the ObjectInputStream object
            inputStream = new ObjectInputStream(new FileInputStream(new File("options.txt").getAbsolutePath()));

            Object obj = null;

            while ((obj = inputStream.readObject()) != null) {

                if (obj instanceof Options) {
                    Options op = (Options) obj;
                    //shot boundary
                    this.algIndex = op.algIndex;
                    this.alg = op.alg;
                    this.numThread = op.numThread;
                    this.threshold = op.threshold;

                    //junk
                    this.tolerance = op.tolerance;
                    this.minDuration = op.minDuration;
                    this.dumpPath = op.dumpPath;

                    //intershot
                    this.keyframeSegments = op.keyframeSegments;
                    this.featuresInSimilarScene = op.featuresInSimilarScene;

                    //output
                    this.outputDir = op.outputDir;//default is the same as video path directory
                    this.selectedFormat = op.selectedFormat;
                    this.chartWidth = op.chartWidth;
                    this.chartHeight = op.chartHeight;
                    break;
                }

            }

        } catch (EOFException ex) { //This exception will be caught when EOF is reached
            System.out.println("End of file reached.");
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(null,
                ex.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            //create a new one
            File f;
            f = new File("options.txt");
            if (!f.exists()) {
                try {
                    f.createNewFile();
                } catch (IOException ex1) {
                    Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                ex.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                ex.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } finally {
            //Close the ObjectInputStream
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null,
                ex.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    /**
     * 
     */
    public void readDumpImages() {
        dumpImg = new ArrayList<BufferedImage>();

        //read all dump images
        final ImageFilter imageFilter = new ImageFilter();
        final File dir = new File(dumpPath);
        //System.out.println("dir files:"+dir.listFiles().length);
        for (final File imgFile : dir.listFiles()) {
            if (imageFilter.accept(imgFile)) {
                BufferedImage img = null;
                //System.out.println("file:"+imgFile.getName());
                try {
                    img = ImageIO.read(new File(imgFile.getAbsolutePath()));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                e.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
                    //System.out.println(e.toString());
                }
                img = ConverterFactory.convertToType(img, BufferedImage.TYPE_3BYTE_BGR);
                if (img != null) {
                    dumpImg.add(img);
                }
            }
        }
//        System.out.println("num files:"+dumpImg.size());
//        for (BufferedImage dump : dumpImg) {
//            JFrame frame = new JFrame();
//            frame.getContentPane().add(dump);
//            
//        }
    }

    class ImageFilter {

        String PNG = "png";
        String BMP = "bmp";

        public boolean accept(File file) {
            if (file != null) {
                if (file.isDirectory()) {
                    return false;
                }
                String extension = getExtension(file);
                if (extension != null && isSupported(extension)) {
                    return true;
                }
            }
            return false;
        }

        private String getExtension(File file) {
            if (file != null) {
                String filename = file.getName();
                int dot = filename.lastIndexOf('.');
                if (dot > 0 && dot < filename.length() - 1) {
                    return filename.substring(dot + 1).toLowerCase();
                }
            }
            return null;
        }

        private boolean isSupported(String ext) {
            return ext.equalsIgnoreCase(PNG) || ext.equalsIgnoreCase(BMP);
        }
    }
}
