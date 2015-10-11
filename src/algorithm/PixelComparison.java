/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting.algorithm;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;

/**
 *
 * @author kyo
 */
public class PixelComparison {

    /**
     * 
     * @param javaImage
     * @param preImage
     * @return
     */
    public double compare(BufferedImage javaImage, BufferedImage preImage) {
        double fd=0.0;
        int w = javaImage.getWidth(),h = javaImage.getHeight();
        javaImage = rescale(javaImage, w, h);
        preImage = rescale(preImage, w, h);
        
        Raster raster = javaImage.getRaster();
        Raster praster = preImage.getRaster();
        int width = javaImage.getWidth();
        int height = javaImage.getHeight();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int sample = raster.getSample(i, j, 0) + raster.getSample(i, j, 1) + raster.getSample(i, j, 2);
                int psample = praster.getSample(i, j, 0) + praster.getSample(i, j, 1) + praster.getSample(i, j, 2);
                fd += Math.abs(sample - psample);
            }
        }
        return Math.abs(fd) / (width * height);
    }
    
    private BufferedImage rescale(BufferedImage i, int w, int h) {   
        float scaleW = ((float) w) / i.getWidth();
        float scaleH = ((float) h) / i.getHeight();
        // Scales the original image
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(i);
        pb.add(scaleW);
        pb.add(scaleH);
        pb.add(0.0F);
        pb.add(0.0F);
        pb.add(new InterpolationNearest());
        // Creates a new, scaled image and uses it on the DisplayJAI component
        return JAI.create("scale", pb).getAsBufferedImage();
    }
    
}
