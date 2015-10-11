/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fyprushediting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

/**
 *
 * @author kyo
 */
public class FDChart extends Thread {

    private final ArrayList<Double> diff;
    private final ArrayList<Long> cutTS;
    private final ArrayList<Long> timeline;
    private final int thresold;
    private ChartPanel cp;
    private final VideoInfo vinfo;
    private TimeSeriesCollection dataset;
    private double maxFD;
    private final String outputDir;
    private final Options options;

    /**
     * 
     * @param outputDir
     * @param options
     * @param diff
     * @param cutTS
     * @param timeline
     * @param vinfo
     * @param cp
     */
    public FDChart(String outputDir, Options options, ArrayList<Double> diff, ArrayList<Long> cutTS, ArrayList<Long> timeline, VideoInfo vinfo, ChartPanel cp) {
        this.outputDir = outputDir;
        this.diff = diff;
        this.cutTS = cutTS;
        this.timeline = timeline;
        this.thresold = options.threshold[options.algIndex];
        this.vinfo = vinfo;
        this.cp = cp;
        this.options = options;
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

    /**
     * Returns a sample dataset.
     * 
     * @return The dataset.
     */
    private TimeSeriesCollection createDataset() {

        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeries dumpSeries = new TimeSeries("Junk\nFrames", Millisecond.class);
        dataset.addSeries(dumpSeries);
        TimeSeries series = null;



        int counter = 0;//including 1st frame
        int scounter = 0;
        long ctime;
        Long startSegment;
        Long endSegment = new Long(-1);
        String category = "";
        boolean isEnd = true;

        try {
            startSegment = cutTS.get(counter);
        } catch (Exception e) {
            startSegment = new Long(-1);
        }
        maxFD = -1;
        for (Double fd : diff) {
            if (Thread.interrupted()) {
                // We've been interrupted: no more crunching.
                return null;
            }

            if (fd > maxFD) {
                maxFD = fd;
            }
            ctime = timeline.get(scounter);
            Millisecond ms = new Millisecond((int) (ctime * vinfo.toPictureTB / 1000), 0, 0, 0, 1, 1, 2000);
            if (startSegment == ctime) {
                counter++;
                try {
                    //System.out.println("cut start: "+startSegment );
                    endSegment = cutTS.get(counter);
                    //System.out.println("cut end: "+endSegment );

                    //start of segment
                    category = "Scene " + ((counter + 1) / 2) + "\nS " + getPictureTime(startSegment) + "\nE " + getPictureTime(endSegment);

                    series = new TimeSeries(category, Millisecond.class);

                    isEnd = false;
                } catch (Exception e) {
                    endSegment = new Long(-1);

                    isEnd = true;
                }

                if (series != null) {
                    series.add(ms, new Double(fd));
                }

            } else if (endSegment == ctime) {

                //next start segment
                counter++;
                try {
                    startSegment = cutTS.get(counter);
                } catch (Exception e) {
                    startSegment = new Long(-1);
                }


                if (series != null) {
                    series.add(ms, new Double(fd));
                    dataset.addSeries(series);
                }

                series = null;

                isEnd = true;
            } else if (isEnd) {
                dumpSeries.add(ms, new Double(fd));
            } else {
                if (series != null) {
                    series.add(ms, new Double(fd));
                }
            }

            scounter++;
        }

        //System.out.println("dataset size: " + series.getAllowDuplicateXValues());

        try {
            if (series != null) {
                dataset.addSeries(series);
            }
        } catch (Exception e) {
        }



        return dataset;
    }

    @Override
    public void run() {
        dataset = createDataset();
        if (Thread.interrupted()) {
            // We've been interrupted: no more crunching.
            return;
        }
        // create the chart...
        JFreeChart chart = ChartFactory.createXYBarChart(
                "Frame Difference of " + vinfo.fileName, // chart title
                "Timeline", // domain axis label
                true,
                "Distance", // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips?
                false // URLs?
                );
        Color c = new Color(0, 200, 0);
        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.DARK_GRAY);
        chart.setBorderPaint(Color.ORANGE);
        Font font = new Font("SansSerif", Font.BOLD, 20);
        TextTitle title = new TextTitle("Frame Difference of " + vinfo.fileName, font);
        title.setPaint(Color.green);
        chart.setTitle(title);

        final XYPlot plot = chart.getXYPlot();
        plot.setOutlineStroke(new BasicStroke(2, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_ROUND));
        plot.setOutlinePaint(Color.ORANGE);


        plot.setBackgroundPaint(Color.black);
        plot.setBackgroundAlpha(0.5f);
        plot.setDomainGridlinePaint(Color.BLUE);
        plot.setRangeGridlinePaint(Color.BLUE);

        final ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickMarkPaint(Color.green);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setLabelPaint(Color.RED);
        domainAxis.setTickMarkPaint(c);
        domainAxis.setTickLabelPaint(c);

        final ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setTickMarkPaint(Color.black);
        rangeAxis.setLabelPaint(Color.red);
        rangeAxis.setTickMarkPaint(c);
        rangeAxis.setTickLabelPaint(c);

//
        // get a reference to the plot for further customisation...
        int gap = (int) Math.ceil(maxFD / 1000);
        final IntervalMarker target = new IntervalMarker(this.thresold - gap, this.thresold + gap);
        target.setLabel("Thresold");
        target.setLabelFont(new Font("SansSerif", Font.BOLD, 15));
        target.setLabelPaint(Color.white);
        target.setLabelAnchor(RectangleAnchor.LEFT);
        target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
        target.setAlpha(1.0f);
        target.setPaint(c);
        plot.addRangeMarker(target, Layer.FOREGROUND);
        if(this.thresold > maxFD){
            rangeAxis.setRange(0.0, (double)(this.thresold + this.thresold * options.tolerance[options.algIndex] / 100));
        }


        XYBarRenderer rend = (XYBarRenderer) plot.getRenderer();
        rend.setBarPainter(new StandardXYBarPainter());
        rend.setShadowVisible(false);
        //dump white => others green gradual
//        rend.setSeriesPaint(0, Color.white);
//        for(int i=1; i<dataset.getSeriesCount(); i++){
//            rend.setSeriesPaint(i, new Color(0,255-i*5,0));
//        }

        //        final IntervalMarker seg = new IntervalMarker(50, 80);
        //        seg.setLabel("Thresold");
        //        seg.setLabelFont(new Font("SansSerif", Font.BOLD, 15));
        //        seg.setLabelAnchor(RectangleAnchor.LEFT);
        //        seg.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
        //        seg.setAlpha(0.2f);
        //        seg.setPaint(Color.lightGray);
        //        plot.addDomainMarker(seg, Layer.FOREGROUND);


        try {
            ChartUtilities.saveChartAsJPEG(new File(outputDir+"/FDChart.jpg"), chart, options.chartWidth, options.chartHeight);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                ex.toString(),
                "Errors",
                JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(FDChart.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        synchronized(this){
            cp.setChart(chart);
        }
    }
}
