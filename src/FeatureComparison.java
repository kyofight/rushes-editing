/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.AbstractModel;
import mpicbg.models.AffineModel2D;
import mpicbg.models.HomographyModel2D;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.models.RigidModel2D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.models.TranslationModel2D;

/**
 *
 * @author kyo
 */
public class FeatureComparison {
    
    private ImagePlus imp1;
    private ImagePlus imp2;
    
    final private List< Feature > fs1 = new ArrayList< Feature >();
    final private List< Feature > fs2 = new ArrayList< Feature >();;
    
    static private class Param
    {   
        final public FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();
        
        /**
         * Closest/next closest neighbour distance ratio
         */
        public float rod = 0.92f;
        
        public boolean useGeometricConsensusFilter = true;
        
        /**
         * Maximal allowed alignment error in px
         */
        public float maxEpsilon = 25.0f;
        
        /**
         * Inlier/candidates ratio
         */
        public float minInlierRatio = 0.05f;
        
        /**
         * Minimal absolute number of inliers
         */
        public int minNumInliers = 7;
        
        /**
         * Implemeted transformation models for choice
         */
        final static public String[] modelStrings = new String[]{ "Translation", "Rigid", "Similarity", "Affine", "Perspective" };
        public int modelIndex = 2;
    }
    
    final static private Param p = new Param();
    
    public FeatureComparison()
    {

    }
    
    public int compare( BufferedImage javaImage, BufferedImage preImage )
    {
        // cleanup
        fs1.clear();
        fs2.clear();
        
        if ( IJ.versionLessThan( "1.40" ) ) return 0;
        
        imp1 = new ImagePlus("img1", javaImage);
        imp2 = new ImagePlus("img2", preImage);
 
        FloatArray2DSIFT sift = new FloatArray2DSIFT( p.sift );
        SIFT ijSIFT = new SIFT( sift );

        ijSIFT.extractFeatures( imp1.getProcessor(), fs1 );
        ijSIFT.extractFeatures( imp2.getProcessor(), fs2 );

        final List< PointMatch > candidates = new ArrayList< PointMatch >();
        FeatureTransform.matchFeatures( fs1, fs2, candidates, p.rod );

        
        if ( p.useGeometricConsensusFilter )
        {
            List< PointMatch > inliers = new ArrayList< PointMatch >();
            
            AbstractModel< ? > model;
            switch ( p.modelIndex )
            {
            case 0:
                model = new TranslationModel2D();
                break;
            case 1:
                model = new RigidModel2D();
                break;
            case 2:
                model = new SimilarityModel2D();
                break;
            case 3:
                model = new AffineModel2D();
                break;
            case 4:
                model = new HomographyModel2D();
                break;
            default:
                return 0;
            }
            
            boolean modelFound;
            try
            {
                modelFound = model.filterRansac(
                        candidates,
                        inliers,
                        1000,
                        p.maxEpsilon,
                        p.minInlierRatio,
                        p.minNumInliers );
            }
            catch ( NotEnoughDataPointsException e )
            {
                modelFound = false;
            }

            if ( modelFound )
            {
                int x1[] = new int[ inliers.size() ];
                int y1[] = new int[ inliers.size() ];
                int x2[] = new int[ inliers.size() ];
                int y2[] = new int[ inliers.size() ];
                
                int i = 0;
                
                for ( PointMatch m : inliers )
                {
                    float[] m_p1 = m.getP1().getL(); 
                    float[] m_p2 = m.getP2().getL();
                    
                    x1[ i ] = Math.round( m_p1[ 0 ] );
                    y1[ i ] = Math.round( m_p1[ 1 ] );
                    x2[ i ] = Math.round( m_p2[ 0 ] );
                    y2[ i ] = Math.round( m_p2[ 1 ] );
                    
                    ++i;
                }
            
                PointRoi pr1 = new PointRoi( x1, y1, inliers.size() );
                PointRoi pr2 = new PointRoi( x2, y2, inliers.size() );
                
                imp1.setRoi( pr1 );
                imp2.setRoi( pr2 );
                
                PointMatch.apply( inliers, model );
                
                return inliers.size();
            } else {
                return 0;
            }
        }else{
            return 0;
        }
    }

}
