/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting.algorithm;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

/**
 *
 * @author kyo
 */
public class NaiveSimilarityFinder {
    // The reference image "signature" (25 representative pixels, each in R,G,B).
    // We use instances of Color to make things simpler.

    private Color[][] signature;
    private int baseSize = 300;

    // Where are all the files?
    /**
     * 
     * @param preImg
     */
    public NaiveSimilarityFinder(RenderedImage preImg) {
        RenderedImage rd = rescale(preImg);
        signature = calcSignature(rd);
    }

    /*
     * This method rescales an image to 300,300 pixels using the JAI scale
     * operator.
     */
    private RenderedImage rescale(RenderedImage i) {
        float scaleW = ((float) baseSize) / i.getWidth();
        float scaleH = ((float) baseSize) / i.getHeight();
        // Scales the original image
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(i);
        pb.add(scaleW);
        pb.add(scaleH);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(new InterpolationNearest());
        // Creates a new, scaled image and uses it on the DisplayJAI component
        return JAI.create("scale", pb);
    }

    /*
     * This method calculates and returns signature vectors for the input image.
     */
    private Color[][] calcSignature(RenderedImage i) {
        // Get memory for the signature.
        Color[][] sig = new Color[5][5];
        // For each of the 25 signature values average the pixels around it.
        // Note that the coordinate of the central pixel is in proportions.
        float[] prop = new float[]{1f / 10f, 3f / 10f, 5f / 10f, 7f / 10f, 9f / 10f};
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                sig[x][y] = averageAround(i, prop[x], prop[y]);
            }
        }
        return sig;
    }

    /*
     * This method averages the pixel values around a central point and return the
     * average as an instance of Color. The point coordinates are proportional to
     * the image.
     */
    private Color averageAround(RenderedImage i, double px, double py) {
        // Get an iterator for the image.
        RandomIter iterator = RandomIterFactory.create(i, null);
        // Get memory for a pixel and for the accumulator.
        double[] pixel = new double[3];
        double[] accum = new double[3];
        // The size of the sampling area.
        int sampleSize = 15;
        int numPixels = 0;
        // Sample the pixels.
        for (double x = px * baseSize - sampleSize; x < px * baseSize + sampleSize; x++) {
            for (double y = py * baseSize - sampleSize; y < py * baseSize + sampleSize; y++) {
                iterator.getPixel((int) x, (int) y, pixel);
                accum[0] += pixel[0];
                accum[1] += pixel[1];
                accum[2] += pixel[2];
                numPixels++;
            }
        }
        // Average the accumulated values.
        accum[0] /= numPixels;
        accum[1] /= numPixels;
        accum[2] /= numPixels;
        return new Color((int) accum[0], (int) accum[1], (int) accum[2]);
    }

    /*
     * This method calculates the distance between the signatures of an image and
     * the reference one. The signatures for the image passed as the parameter are
     * calculated inside the method.
     */
    /**
     * 
     * @param other
     * @return
     */
    public double calcDistance(RenderedImage other) {
        RenderedImage rd = rescale(other);
        // Calculate the signature for that image.
        Color[][] sigOther = calcSignature(rd);
        // There are several ways to calculate distances between two vectors,
        // we will calculate the sum of the distances between the RGB values of
        // pixels in the same positions.
        double dist = 0;
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                int r1 = signature[x][y].getRed();
                int g1 = signature[x][y].getGreen();
                int b1 = signature[x][y].getBlue();
                int r2 = sigOther[x][y].getRed();
                int g2 = sigOther[x][y].getGreen();
                int b2 = sigOther[x][y].getBlue();
                double tempDist = Math.sqrt((r1 - r2) * (r1 - r2) + (g1 - g2)
                        * (g1 - g2) + (b1 - b2) * (b1 - b2));
                dist += tempDist;
            }
        }
        return dist;
    }
}
