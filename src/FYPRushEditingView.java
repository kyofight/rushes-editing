/*
 * FYPRushEditingView.java
 */
package fyprushediting;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * The application's main frame.
 */
public class FYPRushEditingView extends FrameView {

    private final JFrame thisFrame;
    private boolean frameDrag;

    /**
     * 
     * @param app
     */
    public FYPRushEditingView(SingleFrameApplication app) {
        super(app);

        String os = System.getProperty("os.name").toLowerCase();
        // Need a chmod?
        if (os.indexOf("windows") == -1) {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(new String[]{"/bin/chmod", "755",
                            new File("").getAbsolutePath()});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


//        Toolkit tk = Toolkit.getDefaultToolkit();
//        Dimension dimension = tk.getScreenSize();
        this.getFrame().setUndecorated(true);
        //this.getFrame().setSize(new Dimension(1280, 720));
//        this.getFrame().setResizable(false);
        frameSize = this.getFrame().getSize();
        thisFrame = this.getFrame();

        initComponents();
        statusPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        createEmptyThumbs();



//        InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//        inputMap.put(KeyStroke.getKeyStroke("pressed CONTROL"), "controlPressed");
//        inputMap.put(KeyStroke.getKeyStroke("released CONTROL"), "controlReleased");
//        ActionMap actionMap = mainPanel.getActionMap();
//        actionMap.put("controlPressed", new AbstractAction() {
//            public void actionPerformed(ActionEvent e) {
//                throw new UnsupportedOperationException("Not supported yet.");
//            }
//        });
//        actionMap.put("controlReleased", new AbstractAction() {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                System.out.println("released");
//            }
//        });

        long eventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.KEY_EVENT_MASK;

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            public void eventDispatched(AWTEvent e) {
                if (e instanceof MouseEvent) {
                    MouseEvent evt = (MouseEvent) e;
                    if (evt.getID() == MouseEvent.MOUSE_MOVED && frameDrag) {
                        titleLBMouseDragged(evt);
                    }
                } else if (e instanceof KeyEvent) {
                    KeyEvent evt = (KeyEvent) e;
                    if (evt.getID() == KeyEvent.KEY_PRESSED) {
                        if (evt.getKeyCode() == KeyEvent.VK_CONTROL) {
                            frameDrag = true;
                        }
                    } else if (evt.getID() == KeyEvent.KEY_RELEASED) {
                        if (evt.getKeyCode() == KeyEvent.VK_CONTROL) {
                            frameDrag = false;
                            prePt = null;
                        }
                    }
                }
            }
        }, eventMask);

        aboutbtn.setUI(new BasicButtonUI());
        maxbtn.setUI(new BasicButtonUI());
        exitbtn.setUI(new BasicButtonUI());
        videoTab.setUI(new BasicTabbedPaneUI());
        fileCombox.setUI(new BasicComboBoxUI());
        setSilderUI(soundSlider);
        setSilderUI(durationSlider);
        soundSlider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        durationSlider.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        chartPanel = new ChartPanel(null);
        chartPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createEmptyChart();

        chartPanel.addChartMouseListener(new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent cme) {
                if (chartFrame != null) {
                    chartFrame.dispose();
                }
                chartFrame = new ChartFrame("Frame Difference", cme.getChart());
                chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                chartFrame.setVisible(true);
                //chartFrame.pack();
                chartFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            public void chartMouseMoved(ChartMouseEvent cme) {
            }
        });


        fdPanel.setLayout(new BorderLayout());
        fdPanel.add(chartPanel, BorderLayout.CENTER);

        //init option frame
        //optionFrame = new OptionFrame();

        //ui init
        ResourceMap resourceMap = getResourceMap();
        initButtons();
        videoTab.setTitleAt(0, "General");
        videoTab.setTitleAt(1, "Video");
        videoTab.setTitleAt(2, "Audio");
        //videoTab.setTitleAt(3, "System Log");
        videoTab.setSelectedIndex(0);
        //videoTab.setTabPlacement(0);
        // status bar initialization - message timeout, idle icon and busy animation, etc

        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        statusAnimationLabel.setVisible(false);
        progressLB.setBackground(Color.red);
        progressBar.setVisible(true);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.setAutoUpdateForegroundTask(true);
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String) (evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer) (evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });


        mainPanel.setBackground(Color.gray);
        VideoPanel.setBackground(Color.black);
        controlPanel.setBackground(Color.darkGray);
        jPanel5.setBackground(Color.darkGray);
        statusPanel.setBackground(new Color(33, 33, 33));

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                reposition();
            }
        });

        comboxIndex = fileCombox.getSelectedIndex();
        videoPath = "";
    }

    private void setSilderUI(JSlider slider) {
        slider.setUI(new BasicSliderUI(slider) {
//                 // JSlider question: Position after leftclick - Stack Overflow
//                 // http://stackoverflow.com/questions/518471/jslider-question-position-after-leftclick
//                 protected void scrollDueToClickInTrack(int direction) {
//                     int value = slider.getValue();
//                     if (slider.getOrientation() == JSlider.HORIZONTAL) {
//                         value = this.valueForXPosition(slider.getMousePosition().x);
//                     } else if (slider.getOrientation() == JSlider.VERTICAL) {
//                         value = this.valueForYPosition(slider.getMousePosition().y);
//                     }
//                     slider.setValue(value);
//                 }

            @Override
            protected TrackListener createTrackListener(JSlider slider) {
                return new TrackListener() {

                    @Override
                    public void mousePressed(MouseEvent e) {
                        JSlider slider = (JSlider) e.getSource();
                        switch (slider.getOrientation()) {
                            case JSlider.VERTICAL:
                                slider.setValue(valueForYPosition(e.getY()));
                                break;
                            case JSlider.HORIZONTAL:
                                slider.setValue(valueForXPosition(e.getX()));
                                break;
                            default:
                                throw new IllegalArgumentException("orientation must be one of: VERTICAL, HORIZONTAL");
                        }
                        super.mousePressed(e); //isDragging = true;
                        super.mouseDragged(e);
                    }

                    @Override
                    public boolean shouldScroll(int direction) {
                        return false;
                    }
                };
            }
        });
//         slider.setSnapToTicks(false);
//         slider.setPaintTicks(true);
//         slider.setPaintLabels(true);
    }

    private void initButtons() {
        setIcon(openfile, "openfile.png", 30, 30);
        setIcon(optionbtn, "options_icon.png", 30, 30);
        setIcon(editbtn, "movie_edit.png", 30, 30);

        setIcon(firstbtn, "Button-First-icon.png", 50, 50);
        setIcon(pausebtn, "Button-Pause-icon.png", 50, 50);
        setIcon(playbtn, "Button-Play-icon.png", 50, 50);
        setIcon(fastbtn, "Button-Fast-Forward-icon.png", 50, 50);
        setIcon(lastbtn, "Button-Last-icon.png", 50, 50);
        setIcon(speaker, "speaker.png", 45, 45);
    }

    private void setIcon(JButton btn, String file, int width, int height) {
        ResourceMap resourceMap = getResourceMap();
        URL url = resourceMap.getClassLoader().getResource(resourceMap.getResourcesDir() + file);

        Image img = new ImageIcon(url).getImage();
        Image newimg = img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
        ImageIcon newIcon = new ImageIcon(newimg);
        btn.setIcon(newIcon);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        clips = new javax.swing.JScrollPane();
        scrollInternalPanel = new javax.swing.JPanel();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        jPanel1 = new javax.swing.JPanel();
        titleLB = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        exitbtn = new javax.swing.JButton();
        aboutbtn = new javax.swing.JButton();
        maxbtn = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        videoOuter = new javax.swing.JPanel();
        VideoPanel = new javax.swing.JPanel();
        controlPanel = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        durationSlider = new javax.swing.JSlider();
        durationLB = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        speaker = new javax.swing.JButton();
        soundSlider = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        firstbtn = new javax.swing.JButton();
        pausebtn = new javax.swing.JButton();
        playbtn = new javax.swing.JButton();
        fastbtn = new javax.swing.JButton();
        lastbtn = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        filler11 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        jPanel17 = new javax.swing.JPanel();
        openfile = new javax.swing.JButton();
        fileCombox = new javax.swing.JComboBox();
        optionbtn = new javax.swing.JButton();
        editbtn = new javax.swing.JButton();
        rightPanel = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        videoTab = new javax.swing.JTabbedPane();
        generalInfo = new javax.swing.JLabel();
        videoInfo = new javax.swing.JLabel();
        audioInfo = new javax.swing.JLabel();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 32767));
        jPanel12 = new javax.swing.JPanel();
        fdPanel = new javax.swing.JPanel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10), new java.awt.Dimension(10, 10));
        statusPanel = new javax.swing.JPanel();
        statusAnimationLabel = new javax.swing.JLabel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        progressLB = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        lbpadding = new javax.swing.JLabel();
        jDialog1 = new javax.swing.JDialog();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();

        mainPanel.setMaximumSize(new java.awt.Dimension(1920, 1080));
        mainPanel.setMinimumSize(new java.awt.Dimension(1280, 720));
        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setPreferredSize(new java.awt.Dimension(1280, 720));
        mainPanel.setLayout(new java.awt.BorderLayout());

        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(fyprushediting.FYPRushEditingApp.class).getContext().getResourceMap(FYPRushEditingView.class);
        jPanel14.setBackground(resourceMap.getColor("jPanel14.background")); // NOI18N
        jPanel14.setName("jPanel14"); // NOI18N
        jPanel14.setLayout(new java.awt.BorderLayout());

        jPanel15.setBackground(resourceMap.getColor("jPanel15.background")); // NOI18N
        jPanel15.setName("jPanel15"); // NOI18N
        jPanel15.setLayout(new java.awt.BorderLayout());

        clips.setBorder(new javax.swing.border.LineBorder(resourceMap.getColor("clips.border.lineColor"), 5, true)); // NOI18N
        clips.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        clips.setMaximumSize(new java.awt.Dimension(1118, 360));
        clips.setMinimumSize(new java.awt.Dimension(918, 100));
        clips.setName("clips"); // NOI18N
        clips.setOpaque(false);
        clips.setPreferredSize(new java.awt.Dimension(1118, 160));

        scrollInternalPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        scrollInternalPanel.setMaximumSize(new java.awt.Dimension(1108, 100));
        scrollInternalPanel.setMinimumSize(new java.awt.Dimension(1108, 60));
        scrollInternalPanel.setName("scrollInternalPanel"); // NOI18N
        scrollInternalPanel.setPreferredSize(new java.awt.Dimension(1108, 100));
        scrollInternalPanel.setRequestFocusEnabled(false);
        scrollInternalPanel.setVerifyInputWhenFocusTarget(false);

        javax.swing.GroupLayout scrollInternalPanelLayout = new javax.swing.GroupLayout(scrollInternalPanel);
        scrollInternalPanel.setLayout(scrollInternalPanelLayout);
        scrollInternalPanelLayout.setHorizontalGroup(
            scrollInternalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1250, Short.MAX_VALUE)
        );
        scrollInternalPanelLayout.setVerticalGroup(
            scrollInternalPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 150, Short.MAX_VALUE)
        );

        clips.setViewportView(scrollInternalPanel);

        jPanel15.add(clips, java.awt.BorderLayout.SOUTH);

        jPanel14.add(jPanel15, java.awt.BorderLayout.CENTER);

        filler4.setName("filler4"); // NOI18N
        jPanel14.add(filler4, java.awt.BorderLayout.SOUTH);

        filler8.setName("filler8"); // NOI18N
        jPanel14.add(filler8, java.awt.BorderLayout.WEST);

        filler9.setName("filler9"); // NOI18N
        jPanel14.add(filler9, java.awt.BorderLayout.EAST);

        mainPanel.add(jPanel14, java.awt.BorderLayout.SOUTH);

        jPanel1.setBackground(resourceMap.getColor("jPanel1.background")); // NOI18N
        jPanel1.setBorder(javax.swing.BorderFactory.createMatteBorder(2, 0, 2, 0, resourceMap.getColor("jPanel1.border.matteColor"))); // NOI18N
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new java.awt.BorderLayout());

        titleLB.setFont(resourceMap.getFont("titleLB.font")); // NOI18N
        titleLB.setForeground(resourceMap.getColor("titleLB.foreground")); // NOI18N
        titleLB.setIcon(resourceMap.getIcon("titleLB.icon")); // NOI18N
        titleLB.setText(resourceMap.getString("titleLB.text")); // NOI18N
        titleLB.setCursor(new java.awt.Cursor(java.awt.Cursor.MOVE_CURSOR));
        titleLB.setName("titleLB"); // NOI18N
        titleLB.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                titleLBMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                titleLBMouseReleased(evt);
            }
        });
        titleLB.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                titleLBMouseDragged(evt);
            }
        });
        jPanel1.add(titleLB, java.awt.BorderLayout.CENTER);

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N
        jPanel1.add(jLabel2, java.awt.BorderLayout.LINE_START);

        jPanel16.setBackground(resourceMap.getColor("jPanel16.background")); // NOI18N
        jPanel16.setName("jPanel16"); // NOI18N
        jPanel16.setLayout(new java.awt.BorderLayout());

        exitbtn.setBackground(resourceMap.getColor("exitbtn.background")); // NOI18N
        exitbtn.setFont(resourceMap.getFont("exitbtn.font")); // NOI18N
        exitbtn.setForeground(resourceMap.getColor("exitbtn.foreground")); // NOI18N
        exitbtn.setText(resourceMap.getString("exitbtn.text")); // NOI18N
        exitbtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED, resourceMap.getColor("exitbtn.border.highlightOuterColor"), resourceMap.getColor("exitbtn.border.highlightInnerColor"), resourceMap.getColor("exitbtn.border.shadowOuterColor"), resourceMap.getColor("exitbtn.border.shadowInnerColor"))); // NOI18N
        exitbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        exitbtn.setName("exitbtn"); // NOI18N
        exitbtn.setPreferredSize(new java.awt.Dimension(57, 21));
        exitbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitbtnActionPerformed(evt);
            }
        });
        jPanel16.add(exitbtn, java.awt.BorderLayout.EAST);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(fyprushediting.FYPRushEditingApp.class).getContext().getActionMap(FYPRushEditingView.class, this);
        aboutbtn.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutbtn.setBackground(resourceMap.getColor("aboutbtn.background")); // NOI18N
        aboutbtn.setFont(resourceMap.getFont("aboutbtn.font")); // NOI18N
        aboutbtn.setForeground(resourceMap.getColor("aboutbtn.foreground")); // NOI18N
        aboutbtn.setText(resourceMap.getString("aboutbtn.text")); // NOI18N
        aboutbtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED, resourceMap.getColor("aboutbtn.border.highlightOuterColor"), resourceMap.getColor("aboutbtn.border.highlightInnerColor"), resourceMap.getColor("aboutbtn.border.shadowOuterColor"), resourceMap.getColor("aboutbtn.border.shadowInnerColor"))); // NOI18N
        aboutbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        aboutbtn.setName("aboutbtn"); // NOI18N
        aboutbtn.setPreferredSize(new java.awt.Dimension(57, 19));
        jPanel16.add(aboutbtn, java.awt.BorderLayout.WEST);

        maxbtn.setBackground(resourceMap.getColor("maxbtn.background")); // NOI18N
        maxbtn.setFont(resourceMap.getFont("maxbtn.font")); // NOI18N
        maxbtn.setForeground(resourceMap.getColor("maxbtn.foreground")); // NOI18N
        maxbtn.setText(resourceMap.getString("maxbtn.text")); // NOI18N
        maxbtn.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED, resourceMap.getColor("maxbtn.border.highlightOuterColor"), resourceMap.getColor("maxbtn.border.highlightInnerColor"), resourceMap.getColor("maxbtn.border.shadowOuterColor"), resourceMap.getColor("maxbtn.border.shadowInnerColor"))); // NOI18N
        maxbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        maxbtn.setName("maxbtn"); // NOI18N
        maxbtn.setPreferredSize(new java.awt.Dimension(57, 17));
        maxbtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                maxbtnMouseClicked(evt);
            }
        });
        jPanel16.add(maxbtn, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel16, java.awt.BorderLayout.EAST);

        mainPanel.add(jPanel1, java.awt.BorderLayout.NORTH);

        jPanel6.setBackground(resourceMap.getColor("jPanel6.background")); // NOI18N
        jPanel6.setName("jPanel6"); // NOI18N
        jPanel6.setLayout(new java.awt.BorderLayout());

        jPanel7.setBackground(resourceMap.getColor("jPanel7.background")); // NOI18N
        jPanel7.setName("jPanel7"); // NOI18N
        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel9.setBackground(resourceMap.getColor("jPanel9.background")); // NOI18N
        jPanel9.setName("jPanel9"); // NOI18N
        jPanel9.setPreferredSize(new java.awt.Dimension(489, 365));
        jPanel9.setLayout(new java.awt.BorderLayout());

        videoOuter.setBorder(new javax.swing.border.LineBorder(resourceMap.getColor("videoOuter.border.lineColor"), 5, true)); // NOI18N
        videoOuter.setMaximumSize(new java.awt.Dimension(1024, 768));
        videoOuter.setMinimumSize(new java.awt.Dimension(436, 400));
        videoOuter.setName("videoOuter"); // NOI18N
        videoOuter.setOpaque(false);
        videoOuter.setPreferredSize(new java.awt.Dimension(600, 450));
        videoOuter.setLayout(new java.awt.BorderLayout());

        VideoPanel.setBackground(resourceMap.getColor("VideoPanel.background")); // NOI18N
        VideoPanel.setForeground(resourceMap.getColor("VideoPanel.foreground")); // NOI18N
        VideoPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        VideoPanel.setMaximumSize(new java.awt.Dimension(9999, 9999));
        VideoPanel.setMinimumSize(new java.awt.Dimension(600, 450));
        VideoPanel.setName("VideoPanel"); // NOI18N
        VideoPanel.setPreferredSize(new java.awt.Dimension(600, 450));
        VideoPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                VideoPanelMouseClicked(evt);
            }
        });
        VideoPanel.setLayout(new javax.swing.BoxLayout(VideoPanel, javax.swing.BoxLayout.LINE_AXIS));
        videoOuter.add(VideoPanel, java.awt.BorderLayout.CENTER);

        controlPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(0, 0, 0)));
        controlPanel.setMaximumSize(new java.awt.Dimension(702, 116));
        controlPanel.setMinimumSize(new java.awt.Dimension(702, 116));
        controlPanel.setName("controlPanel"); // NOI18N
        controlPanel.setPreferredSize(new java.awt.Dimension(702, 116));
        controlPanel.setLayout(new java.awt.BorderLayout());

        jPanel5.setBackground(resourceMap.getColor("jPanel5.background")); // NOI18N
        jPanel5.setName("jPanel5"); // NOI18N
        jPanel5.setLayout(new java.awt.BorderLayout());

        durationSlider.setBackground(resourceMap.getColor("durationSlider.background")); // NOI18N
        durationSlider.setValue(0);
        durationSlider.setName("durationSlider"); // NOI18N
        durationSlider.setPreferredSize(new java.awt.Dimension(500, 25));
        durationSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                durationSliderMouseReleased(evt);
            }
        });
        jPanel5.add(durationSlider, java.awt.BorderLayout.CENTER);

        durationLB.setBackground(resourceMap.getColor("durationLB.background")); // NOI18N
        durationLB.setFont(resourceMap.getFont("durationLB.font")); // NOI18N
        durationLB.setForeground(resourceMap.getColor("durationLB.foreground")); // NOI18N
        durationLB.setText(resourceMap.getString("durationLB.text")); // NOI18N
        durationLB.setToolTipText(resourceMap.getString("durationLB.toolTipText")); // NOI18N
        durationLB.setName("durationLB"); // NOI18N
        durationLB.setOpaque(true);
        jPanel5.add(durationLB, java.awt.BorderLayout.EAST);

        jLabel3.setBackground(resourceMap.getColor("jLabel3.background")); // NOI18N
        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jLabel3.setOpaque(true);
        jPanel5.add(jLabel3, java.awt.BorderLayout.WEST);

        jLabel5.setBackground(resourceMap.getColor("jLabel5.background")); // NOI18N
        jLabel5.setForeground(resourceMap.getColor("jLabel5.foreground")); // NOI18N
        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N
        jLabel5.setOpaque(true);
        jPanel5.add(jLabel5, java.awt.BorderLayout.NORTH);

        controlPanel.add(jPanel5, java.awt.BorderLayout.NORTH);

        jPanel2.setBackground(resourceMap.getColor("jPanel2.background")); // NOI18N
        jPanel2.setMinimumSize(new java.awt.Dimension(100, 50));
        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setPreferredSize(new java.awt.Dimension(696, 72));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel3.setBackground(resourceMap.getColor("jPanel3.background")); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N
        jPanel3.setPreferredSize(new java.awt.Dimension(232, 90));
        jPanel3.setLayout(new java.awt.BorderLayout());

        speaker.setIcon(resourceMap.getIcon("speaker.icon")); // NOI18N
        speaker.setText(resourceMap.getString("speaker.text")); // NOI18N
        speaker.setToolTipText(resourceMap.getString("speaker.toolTipText")); // NOI18N
        speaker.setBorder(new javax.swing.border.MatteBorder(null));
        speaker.setContentAreaFilled(false);
        speaker.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        speaker.setFocusPainted(false);
        speaker.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        speaker.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        speaker.setName("speaker"); // NOI18N
        speaker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speakerActionPerformed(evt);
            }
        });
        jPanel3.add(speaker, java.awt.BorderLayout.WEST);

        soundSlider.setBackground(resourceMap.getColor("soundSlider.background")); // NOI18N
        soundSlider.setName("soundSlider"); // NOI18N
        soundSlider.setPreferredSize(new java.awt.Dimension(100, 25));
        jPanel3.add(soundSlider, java.awt.BorderLayout.CENTER);

        jLabel4.setBackground(resourceMap.getColor("jLabel4.background")); // NOI18N
        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setToolTipText(resourceMap.getString("jLabel4.toolTipText")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N
        jLabel4.setOpaque(true);
        jPanel3.add(jLabel4, java.awt.BorderLayout.EAST);

        jPanel2.add(jPanel3, java.awt.BorderLayout.EAST);

        jPanel4.setBackground(resourceMap.getColor("jPanel4.background")); // NOI18N
        jPanel4.setName("jPanel4"); // NOI18N
        jPanel4.setLayout(new java.awt.GridLayout(1, 0));

        firstbtn.setIcon(resourceMap.getIcon("openfile.icon")); // NOI18N
        firstbtn.setToolTipText(resourceMap.getString("firstbtn.toolTipText")); // NOI18N
        firstbtn.setBorder(new javax.swing.border.MatteBorder(null));
        firstbtn.setContentAreaFilled(false);
        firstbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        firstbtn.setFocusPainted(false);
        firstbtn.setName("firstbtn"); // NOI18N
        firstbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firstbtnActionPerformed(evt);
            }
        });
        jPanel4.add(firstbtn);

        pausebtn.setIcon(resourceMap.getIcon("pausebtn.icon")); // NOI18N
        pausebtn.setToolTipText(resourceMap.getString("pausebtn.toolTipText")); // NOI18N
        pausebtn.setBorder(new javax.swing.border.MatteBorder(null));
        pausebtn.setContentAreaFilled(false);
        pausebtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        pausebtn.setFocusPainted(false);
        pausebtn.setName("pausebtn"); // NOI18N
        pausebtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pausebtnActionPerformed(evt);
            }
        });
        jPanel4.add(pausebtn);

        playbtn.setIcon(resourceMap.getIcon("playbtn.icon")); // NOI18N
        playbtn.setToolTipText(resourceMap.getString("playbtn.toolTipText")); // NOI18N
        playbtn.setBorder(new javax.swing.border.MatteBorder(null));
        playbtn.setContentAreaFilled(false);
        playbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        playbtn.setFocusPainted(false);
        playbtn.setName("playbtn"); // NOI18N
        playbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playbtnActionPerformed(evt);
            }
        });
        jPanel4.add(playbtn);

        fastbtn.setIcon(resourceMap.getIcon("lastbtn.icon")); // NOI18N
        fastbtn.setToolTipText(resourceMap.getString("fastbtn.toolTipText")); // NOI18N
        fastbtn.setBorder(new javax.swing.border.MatteBorder(null));
        fastbtn.setContentAreaFilled(false);
        fastbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        fastbtn.setFocusPainted(false);
        fastbtn.setName("fastbtn"); // NOI18N
        fastbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fastbtnActionPerformed(evt);
            }
        });
        jPanel4.add(fastbtn);

        lastbtn.setIcon(resourceMap.getIcon("lastbtn.icon")); // NOI18N
        lastbtn.setToolTipText(resourceMap.getString("lastbtn.toolTipText")); // NOI18N
        lastbtn.setBorder(new javax.swing.border.MatteBorder(null));
        lastbtn.setContentAreaFilled(false);
        lastbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lastbtn.setFocusPainted(false);
        lastbtn.setName("lastbtn"); // NOI18N
        lastbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lastbtnActionPerformed(evt);
            }
        });
        jPanel4.add(lastbtn);

        jPanel2.add(jPanel4, java.awt.BorderLayout.CENTER);

        controlPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        videoOuter.add(controlPanel, java.awt.BorderLayout.SOUTH);

        jPanel9.add(videoOuter, java.awt.BorderLayout.CENTER);

        jPanel7.add(jPanel9, java.awt.BorderLayout.CENTER);

        jPanel10.setBackground(resourceMap.getColor("jPanel10.background")); // NOI18N
        jPanel10.setName("jPanel10"); // NOI18N
        jPanel10.setPreferredSize(new java.awt.Dimension(489, 55));
        jPanel10.setLayout(new java.awt.BorderLayout());

        filler11.setName("filler11"); // NOI18N
        jPanel10.add(filler11, java.awt.BorderLayout.NORTH);

        jPanel17.setBackground(resourceMap.getColor("jPanel17.background")); // NOI18N
        jPanel17.setName("jPanel17"); // NOI18N
        jPanel17.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 5));

        openfile.setIcon(resourceMap.getIcon("openfile.icon")); // NOI18N
        openfile.setText(resourceMap.getString("openfile.text")); // NOI18N
        openfile.setToolTipText(resourceMap.getString("openfile.toolTipText")); // NOI18N
        openfile.setBorder(new javax.swing.border.MatteBorder(null));
        openfile.setContentAreaFilled(false);
        openfile.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        openfile.setFocusPainted(false);
        openfile.setName("openfile"); // NOI18N
        openfile.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                openfileMouseClicked(evt);
            }
        });
        jPanel17.add(openfile);

        fileCombox.setBackground(resourceMap.getColor("fileCombox.background")); // NOI18N
        fileCombox.setFont(resourceMap.getFont("fileCombox.font")); // NOI18N
        fileCombox.setBorder(null);
        fileCombox.setName("fileCombox"); // NOI18N
        fileCombox.setPreferredSize(new java.awt.Dimension(600, 30));
        fileCombox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                fileComboxItemStateChanged(evt);
            }
        });
        jPanel17.add(fileCombox);

        optionbtn.setAction(actionMap.get("showOptionFrame")); // NOI18N
        optionbtn.setBackground(resourceMap.getColor("optionbtn.background")); // NOI18N
        optionbtn.setIcon(resourceMap.getIcon("optionbtn.icon")); // NOI18N
        optionbtn.setText(resourceMap.getString("optionbtn.text")); // NOI18N
        optionbtn.setBorder(new javax.swing.border.MatteBorder(null));
        optionbtn.setContentAreaFilled(false);
        optionbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        optionbtn.setFocusPainted(false);
        optionbtn.setName("optionbtn"); // NOI18N
        jPanel17.add(optionbtn);

        editbtn.setBackground(resourceMap.getColor("editbtn.background")); // NOI18N
        editbtn.setIcon(resourceMap.getIcon("editbtn.icon")); // NOI18N
        editbtn.setText(resourceMap.getString("editbtn.text")); // NOI18N
        editbtn.setBorder(new javax.swing.border.MatteBorder(null));
        editbtn.setContentAreaFilled(false);
        editbtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        editbtn.setFocusPainted(false);
        editbtn.setName("editbtn"); // NOI18N
        editbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editbtnActionPerformed(evt);
            }
        });
        jPanel17.add(editbtn);

        jPanel10.add(jPanel17, java.awt.BorderLayout.WEST);

        jPanel7.add(jPanel10, java.awt.BorderLayout.NORTH);

        jPanel6.add(jPanel7, java.awt.BorderLayout.CENTER);

        rightPanel.setBackground(resourceMap.getColor("rightPanel.background")); // NOI18N
        rightPanel.setName("rightPanel"); // NOI18N
        rightPanel.setPreferredSize(new java.awt.Dimension(500, 400));
        rightPanel.setLayout(new java.awt.BorderLayout());

        jPanel11.setBackground(resourceMap.getColor("jPanel11.background")); // NOI18N
        jPanel11.setName("jPanel11"); // NOI18N
        jPanel11.setLayout(new java.awt.BorderLayout());

        jPanel13.setBackground(resourceMap.getColor("jPanel13.background")); // NOI18N
        jPanel13.setName("jPanel13"); // NOI18N
        jPanel13.setLayout(new java.awt.BorderLayout());

        videoTab.setBackground(resourceMap.getColor("videoTabs.background")); // NOI18N
        videoTab.setForeground(resourceMap.getColor("videoTabs.foreground")); // NOI18N
        videoTab.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        videoTab.setFont(resourceMap.getFont("videoTabs.font")); // NOI18N
        videoTab.setMaximumSize(new java.awt.Dimension(400, 573));
        videoTab.setMinimumSize(new java.awt.Dimension(262, 300));
        videoTab.setName("videoTabs"); // NOI18N
        videoTab.setPreferredSize(new java.awt.Dimension(362, 300));

        generalInfo.setFont(resourceMap.getFont("jLabel2.font")); // NOI18N
        generalInfo.setForeground(resourceMap.getColor("generalInfo.foreground")); // NOI18N
        generalInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        generalInfo.setText(resourceMap.getString("generalInfo.text")); // NOI18N
        generalInfo.setToolTipText(resourceMap.getString("generalInfo.toolTipText")); // NOI18N
        generalInfo.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        generalInfo.setName("generalInfo"); // NOI18N
        videoTab.addTab("tab2", generalInfo);

        videoInfo.setFont(resourceMap.getFont("videoInfo.font")); // NOI18N
        videoInfo.setForeground(resourceMap.getColor("videoInfo.foreground")); // NOI18N
        videoInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        videoInfo.setText(resourceMap.getString("videoInfo.text")); // NOI18N
        videoInfo.setName("videoInfo"); // NOI18N
        videoTab.addTab("tab1", videoInfo);

        audioInfo.setFont(resourceMap.getFont("audioInfo.font")); // NOI18N
        audioInfo.setForeground(resourceMap.getColor("audioInfo.foreground")); // NOI18N
        audioInfo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        audioInfo.setText(resourceMap.getString("audioInfo.text")); // NOI18N
        audioInfo.setName("audioInfo"); // NOI18N
        videoTab.addTab("tab3", audioInfo);

        jPanel13.add(videoTab, java.awt.BorderLayout.NORTH);
        videoTab.getAccessibleContext().setAccessibleName(resourceMap.getString("videoTabs.AccessibleContext.accessibleName")); // NOI18N
        videoTab.getAccessibleContext().setAccessibleDescription(resourceMap.getString("videoTabs.AccessibleContext.accessibleDescription")); // NOI18N
        videoTab.setBackground(Color.black);

        filler5.setName("filler5"); // NOI18N
        jPanel13.add(filler5, java.awt.BorderLayout.CENTER);

        jPanel11.add(jPanel13, java.awt.BorderLayout.NORTH);

        jPanel12.setBackground(resourceMap.getColor("jPanel12.background")); // NOI18N
        jPanel12.setName("jPanel12"); // NOI18N
        jPanel12.setLayout(new java.awt.BorderLayout());

        fdPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        fdPanel.setMaximumSize(new java.awt.Dimension(1400, 1900));
        fdPanel.setMinimumSize(new java.awt.Dimension(400, 200));
        fdPanel.setName("fdPanel"); // NOI18N
        fdPanel.setOpaque(false);
        fdPanel.setPreferredSize(new java.awt.Dimension(500, 300));

        javax.swing.GroupLayout fdPanelLayout = new javax.swing.GroupLayout(fdPanel);
        fdPanel.setLayout(fdPanelLayout);
        fdPanelLayout.setHorizontalGroup(
            fdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 470, Short.MAX_VALUE)
        );
        fdPanelLayout.setVerticalGroup(
            fdPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 432, Short.MAX_VALUE)
        );

        jPanel12.add(fdPanel, java.awt.BorderLayout.CENTER);

        jPanel11.add(jPanel12, java.awt.BorderLayout.CENTER);

        rightPanel.add(jPanel11, java.awt.BorderLayout.CENTER);

        filler1.setName("filler1"); // NOI18N
        rightPanel.add(filler1, java.awt.BorderLayout.WEST);

        filler2.setName("filler2"); // NOI18N
        rightPanel.add(filler2, java.awt.BorderLayout.EAST);

        filler3.setName("filler3"); // NOI18N
        rightPanel.add(filler3, java.awt.BorderLayout.NORTH);

        jPanel6.add(rightPanel, java.awt.BorderLayout.EAST);

        filler6.setName("filler6"); // NOI18N
        jPanel6.add(filler6, java.awt.BorderLayout.WEST);

        filler7.setName("filler7"); // NOI18N
        jPanel6.add(filler7, java.awt.BorderLayout.SOUTH);

        mainPanel.add(jPanel6, java.awt.BorderLayout.CENTER);

        statusPanel.setBackground(resourceMap.getColor("statusPanel.background")); // NOI18N
        statusPanel.setName("statusPanel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusAnimationLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N
        statusPanel.add(statusAnimationLabel);

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N
        statusPanel.add(statusPanelSeparator);

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N
        statusPanel.add(statusMessageLabel);

        progressLB.setFont(resourceMap.getFont("progressLB.font")); // NOI18N
        progressLB.setForeground(resourceMap.getColor("progressLB.foreground")); // NOI18N
        progressLB.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        progressLB.setText(resourceMap.getString("progressLB.text")); // NOI18N
        progressLB.setToolTipText(resourceMap.getString("progressLB.toolTipText")); // NOI18N
        progressLB.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        progressLB.setName("progressLB"); // NOI18N
        statusPanel.add(progressLB);

        progressBar.setName("progressBar"); // NOI18N
        progressBar.setString(resourceMap.getString("progressBar.string")); // NOI18N
        progressBar.setStringPainted(true);
        statusPanel.add(progressBar);
        progressBar.getAccessibleContext().setAccessibleDescription(resourceMap.getString("progressBar.AccessibleContext.accessibleDescription")); // NOI18N

        lbpadding.setText(resourceMap.getString("lbpadding.text")); // NOI18N
        lbpadding.setName("lbpadding"); // NOI18N
        statusPanel.add(lbpadding);

        jDialog1.setName("jDialog1"); // NOI18N

        javax.swing.GroupLayout jDialog1Layout = new javax.swing.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        jMenuBar1.setBackground(resourceMap.getColor("jMenuBar1.background")); // NOI18N
        jMenuBar1.setName("jMenuBar1"); // NOI18N

        jMenu1.setText(resourceMap.getString("jMenu1.text")); // NOI18N
        jMenu1.setName("jMenu1"); // NOI18N
        jMenuBar1.add(jMenu1);

        jMenu2.setText(resourceMap.getString("jMenu2.text")); // NOI18N
        jMenu2.setName("jMenu2"); // NOI18N
        jMenuBar1.add(jMenu2);

        setComponent(mainPanel);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

private void openfileMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_openfileMouseClicked
    // TODO add your handling code here:

    JFileChooser fileChooser = new JFileChooser();

    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    int result = fileChooser.showOpenDialog(thisFrame);

    File videoFile;
    // user clicked Cancel button on dialog
    if (result == JFileChooser.CANCEL_OPTION) {
        videoFile = null;
    } else {
        videoFile = fileChooser.getSelectedFile();
        if (controller != null && controller.isProcessing()) {
            result = JOptionPane.showConfirmDialog(VideoPanel,
                    "Video is processing, quitting now might cause unexpected error, do you want to continue?",
                    "Caution",
                    JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }
        loadVideo(videoFile, true);

    }
}//GEN-LAST:event_openfileMouseClicked

    private void loadVideo(File videoFile, boolean isReset) {
        try {
            if (isReset) {
                videoPath = videoFile.getPath();
                fileName = videoFile.getName();
                videoDirectory = videoFile.getParent();
                reset();
                vinfo = VideoPlayer.createVInfo(videoPath, fileName, videoDirectory);
                fileCombox.addItem(videoPath);

                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        progressBar.setValue(0);
                        progressBar.setString("Pending");
                        progressLB.setText("Pending for Process");
                    }
                });

                setVInfo(vinfo);
                player = new VideoPlayer(videoPath, vinfo, VideoPanel.getSize(), isMute, soundSlider, durationSlider, durationLB, fileCombox);
            } else {
                if (player != null) {
                    player.close();
                    VideoPanel.remove(player);
                }
                VideoInfo newVinfo = VideoPlayer.createVInfo(videoFile.getPath(), videoFile.getName(), videoFile.getParent());
                setVInfo(newVinfo);
                player = new VideoPlayer(videoFile.getPath(), newVinfo, VideoPanel.getSize(), isMute, soundSlider, durationSlider, durationLB, fileCombox);
            }


            VideoPanel.setLayout(new BorderLayout());
            VideoPanel.add(player, BorderLayout.CENTER);

            videoThread = new Thread(player);
            videoThread.start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(VideoPanel,
                    e.toString(),
                    "Errors",
                    JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(ProcessController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * 
     * @param vinfo
     */
    public void setVInfo(VideoInfo vinfo) {
        if (vinfo != null) {
            generalInfo.setText("<html>"
                    + "<table>"
                    + "<tr><td align='right'>File Name: </td><td>" + vinfo.fileName + "</td></tr>"
                    + "<tr><td align='right'>Num. Streams: </td><td>" + vinfo.numStreams + "</td></tr>"
                    + "<tr><td align='right'>File Size(bytes): </td><td>" + vinfo.fileSize + "</td></tr>"
                    + "<tr><td align='right'>BitRate: </td><td>" + vinfo.bitRate + "</td></tr>"
                    + "<tr><td align='right'>Duration: </td><td>" + getPictureTime(vinfo.duration, false) + "</td></tr>"
                    + "</table>"
                    + "</html>");

            videoInfo.setText("<html>"
                    + "<table border='0'>"
                    + "<tr><td align='right'>Type: </td><td>" + vinfo.vcodec.getLongName() + "</td></tr>"
                    + "<tr><td align='right'>Frame Rate: </td><td>" + vinfo.vframeRate + "</td></tr>"
                    //+ "<tr><td align='right'>Duration: </td><td>" + getPictureTime((long) (vinfo.vduration * vinfo.toPictureTB), false) + "</td></tr>"
                    + "<tr><td align='right'>Pixel Format: </td><td>" + vinfo.vformat + "</td></tr>"
                    + "<tr><td align='right'>Dimension: </td><td>" + vinfo.vwidth + " x " + vinfo.vheight + "</td></tr>"
                    + "<tr><td align='right'>Time Base: </td><td>1/" + vinfo.vtimeBase + "</td></tr>"
                    + "<tr><td align='right'>Codec: </td><td>" + vinfo.vtype + "</td></tr>"
                    + "</table>"
                    + "</html>");

            audioInfo.setText("<html>"
                    + "<table border='0'>"
                    + "<tr><td align='right'>Type: </td><td>" + vinfo.acodec.getLongName() + "</td></tr>"
                    + "<tr><td align='right'>Sample Rate: </td><td>" + vinfo.asampleRate + " Hz</td></tr>"
                    //+ "<tr><td align='right'>Duration: </td><td>" + getPictureTime((long) (vinfo.aduration / (double)vinfo.atimeBase), true) + "</td></tr>"
                    + "<tr><td align='right'>Audio Format: </td><td>" + vinfo.aformat + "</td></tr>"
                    + "<tr><td align='right'>Channels: </td><td>" + vinfo.achannels + "</td></tr>"
                    + "<tr><td align='right'>Time Base: </td><td>1/" + vinfo.atimeBase + "</td></tr>"
                    + "<tr><td align='right'>Codec: </td><td>" + vinfo.atype + "</td></tr>"
                    + "</table>"
                    + "</html>");
        }

    }

    private String getPictureTime(long duration, boolean isSec) {
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

    private void reset() {
        if (player != null) {
            closeProcesses();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(FYPRushEditingView.class.getName()).log(Level.SEVERE, null, ex);
            }

            player.close();
            VideoPanel.remove(player);
            fileCombox.removeAllItems();
            createEmptyThumbs();
            createEmptyChart();
        }
    }

    private void closeProcesses() {
        if (controller != null) {
            controller.shutDown();
            controller = null;
        }
    }

    private void createEmptyThumbs() {
        //clear file box
        // Get number of items
        int num = fileCombox.getItemCount();

        // Get items
        for (int i = 0; i < num; i++) {
            Object item = fileCombox.getItemAt(i);
            if (item.toString() == null ? vinfo.url != null : !item.toString().equals(vinfo.url)) {
                fileCombox.removeItem(item);
                num--;
                i--;
            }
        }

        scrollInternalPanel.removeAll();

        //create empty thumbnails
        int numRows = 1;
        this.scrollInternalPanel.setLayout(new GridLayout(numRows, ProcessController.colCount, ProcessController.wgap, ProcessController.wgap));

        this.scrollInternalPanel.setBackground(new Color(33, 33, 33));
        this.scrollInternalPanel.setPreferredSize(new Dimension(100, 100));

        int totalThumbs = numRows * ProcessController.colCount;
        JPanel thumbs[] = new JPanel[totalThumbs];
        for (int i = 0; i < totalThumbs; i++) {
            thumbs[i] = new JPanel();
            thumbs[i].setBackground(Color.darkGray);
            scrollInternalPanel.add(thumbs[i]);
        }

        scrollInternalPanel.doLayout();
        scrollInternalPanel.repaint();
        scrollInternalPanel.revalidate();
    }

    private void createEmptyChart() {

        //set chart panel
        JFreeChart chart = ChartFactory.createXYBarChart(
                "Frame Difference", // chart title
                "Timeline", // domain axis label
                true,
                "Distance", // range axis label
                new TimeSeriesCollection(), // data
                PlotOrientation.VERTICAL, // orientation
                false, // include legend
                true, // tooltips?
                false // URLs?
                );
        Color c = new Color(0, 200, 0);
        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.DARK_GRAY);
        chart.setBorderPaint(Color.ORANGE);
        Font font = new Font("SansSerif", Font.BOLD, 20);
        TextTitle title = new TextTitle("Frame Difference", font);
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
        domainAxis.setTickLabelsVisible(false);

        final ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setTickMarkPaint(Color.black);
        rangeAxis.setLabelPaint(Color.red);
        rangeAxis.setTickMarkPaint(c);
        rangeAxis.setTickLabelPaint(c);


        plot.setNoDataMessage("Video is not processed");
        plot.setNoDataMessagePaint(Color.white);
        plot.setNoDataMessageFont(font);
        chartPanel.setChart(chart);
    }

private void playbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playbtnActionPerformed

    if (player != null && player.isReady()) {
        player.play();
    }
}//GEN-LAST:event_playbtnActionPerformed

private void pausebtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pausebtnActionPerformed

    if (player != null && player.isReady()) {
        player.pause();
    }
}//GEN-LAST:event_pausebtnActionPerformed

private void firstbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firstbtnActionPerformed
    int totalVideo = fileCombox.getItemCount();
    if (totalVideo > 0) {
        if (totalVideo == 1) {
            if (player != null && player.isReady()) {
                player.seek(0, false, 0, false);
            }
        } else {
            //previous video
            int prevIndex = fileCombox.getSelectedIndex() - 1;
            if (prevIndex < 0) {
                prevIndex = totalVideo - 1;
            }
            fileCombox.setSelectedIndex(prevIndex);
        }
    }
}//GEN-LAST:event_firstbtnActionPerformed

private void lastbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lastbtnActionPerformed
    int totalVideo = fileCombox.getItemCount();
    if (totalVideo > 0) {
        if (totalVideo == 1) {
            if (player != null && player.isReady()) {
                player.seek(0, false, 0, false);
            }
        } else {
            //next video
            int nextIndex = fileCombox.getSelectedIndex() + 1;
            if (nextIndex == totalVideo) {
                nextIndex = 0;
            }
            fileCombox.setSelectedIndex(nextIndex);
        }
    }
}//GEN-LAST:event_lastbtnActionPerformed

private void speakerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speakerActionPerformed

    isMute = !isMute;
    if (isMute) {
        setIcon(speaker, "speaker_disabled.png", 45, 45);
    } else {
        setIcon(speaker, "speaker.png", 45, 45);
    }
    if (player != null && player.isReady()) {
        player.toggleSound(isMute);
    }
}//GEN-LAST:event_speakerActionPerformed

private void fastbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fastbtnActionPerformed
    if (player != null && player.isReady()) {
        player.seek(0, true, 5, true);
    }
}//GEN-LAST:event_fastbtnActionPerformed

private void durationSliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_durationSliderMouseReleased
// TODO add your handling code here:
    if (player != null && player.isReady()) {
        player.seek(durationSlider.getValue(), true, 0, false);
    }
}//GEN-LAST:event_durationSliderMouseReleased

private void editbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editbtnActionPerformed
// TODO add your handling code here:
    if (controller != null && controller.isProcessing()) {
        int result = JOptionPane.showConfirmDialog(VideoPanel,
                "Video is processing, quitting now might cause unexpected error, do you want to continue?",
                "Caution",
                JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.CANCEL_OPTION) {
            return;
        }
    }

    if (videoPath != null && !"".equals(videoPath)) {
        createEmptyThumbs();
        if (controller != null) {
            controller.shutDown();
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(FYPRushEditingView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        if (vinfo == null) {
//            vinfo = VideoPlayer.createVInfo(videoPath, fileName, videoDirectory);
//        }

        if (player != null) {
            player.pause();
        }

        options = new Options();
        options.readOptions();
        options.readDumpImages();
        chartPanel.getChart().getXYPlot().setNoDataMessage("Video is Processing");
        controller = new ProcessController(VideoPlayer.createVInfo(videoPath, fileName, videoDirectory), options, chartPanel, fileCombox, scrollInternalPanel, progressBar, progressLB);
        controller.start();
    } else {
        chartPanel.getChart().getXYPlot().setNoDataMessage("Video is not Processed");
        JOptionPane.showMessageDialog(VideoPanel,
                "Please select a video",
                "Errors",
                JOptionPane.ERROR_MESSAGE);
    }
}//GEN-LAST:event_editbtnActionPerformed

private void VideoPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_VideoPanelMouseClicked
// TODO add your handling code here:
    if (player != null && player.isReady()) {
        player.togglePlay();
    }
}//GEN-LAST:event_VideoPanelMouseClicked

private void exitbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitbtnActionPerformed
// TODO add your handling code here:
    try {
        // Perhaps ask user if they want to save any unsaved files first.
        if (controller != null && controller.isProcessing()) {
            int result = JOptionPane.showConfirmDialog(VideoPanel,
                    "Video is processing, quitting now might cause unexpected error, do you want to continue?",
                    "Caution",
                    JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        if (player != null) {
            player.close();
        }
        if (controller != null) {
            controller.shutDown();
            Thread.sleep(100);
        }
        System.exit(0);
    } catch (InterruptedException ex) {
        Logger.getLogger(FYPRushEditingView.class.getName()).log(Level.SEVERE, null, ex);
    }
}//GEN-LAST:event_exitbtnActionPerformed

private void fileComboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_fileComboxItemStateChanged
// TODO add your handling code here:
    if (evt.getStateChange() == ItemEvent.SELECTED && fileCombox.getItemCount() > 1) {
        //System.out.println(evt.getItem().toString());
        loadVideo(new File(evt.getItem().toString()), false);
        if (controller != null && !controller.isProcessing() && !fileCombox.getSelectedItem().toString().equals(vinfo.url)) {
            controller.setThumbPlayStatus(fileCombox.getSelectedIndex() - 1, true);
        }
        comboxIndex = fileCombox.getSelectedIndex();
        //System.out.println("index 1: "+comboxIndex);
    }
    if (evt.getStateChange() == ItemEvent.DESELECTED && fileCombox.getItemCount() > 1) {

        if (controller != null && !controller.isProcessing() && comboxIndex > 0) {
            controller.setThumbPlayStatus(comboxIndex - 1, false);
            //System.out.println("index 2: "+comboxIndex);
        }
    }
}//GEN-LAST:event_fileComboxItemStateChanged

private void titleLBMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_titleLBMousePressed
// TODO add your handling code here:
    frameDrag = true;
    prePt = new Point(evt.getXOnScreen(), evt.getYOnScreen());
}//GEN-LAST:event_titleLBMousePressed

private void titleLBMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_titleLBMouseReleased
// TODO add your handling code here:
    prePt = null;
    frameDrag = false;
}//GEN-LAST:event_titleLBMouseReleased

private void titleLBMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_titleLBMouseDragged
// TODO add your handling code here:
    if (frameDrag) {
        Point pt = evt.getLocationOnScreen();
        if (prePt != null) {
            Point curLoc = thisFrame.getLocation();
            Point differencePoint = new Point((int) (pt.getX() - prePt.getX()), (int) (pt.getY() - prePt.getY()));
            thisFrame.setLocation((int) (curLoc.getX() + differencePoint.getX()), (int) (curLoc.getY() + differencePoint.getY()));
        }
        prePt = pt;
    }
}//GEN-LAST:event_titleLBMouseDragged

    private void reposition() {
        GraphicsConfiguration gc = thisFrame.getGraphicsConfiguration();
        Insets insects = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        //System.out.println("insects:::" + insects);
        Dimension maxBoundsDefault = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension maxBounds = Toolkit.getDefaultToolkit().getScreenSize();
        //maxBounds.height = 450;

        if (maxBounds.height < LEAST_FRAME_MINI_HEIGHT) {
            maxBounds.height = LEAST_FRAME_MINI_HEIGHT;
        } else if (maxBounds.height < FRAME_MINI_HEIGHT) {
            maxBounds.height = FRAME_MINI_HEIGHT;
        }

        int w = (int) (maxBounds.height * DEFAULT_DIMENSION_RATIO);

        maxBounds.height -= (insects.top + insects.bottom);
        w -= (insects.left + insects.right);
        
        Dimension dimension = new Dimension(w, maxBounds.height);
        thisFrame.setSize(dimension);

        thisFrame.setLocation(new Point(insects.left + (int)((maxBoundsDefault.getWidth() - w)/2), insects.top));

        if (maxBounds.height < FRAME_MINI_HEIGHT) {
            int errorHeightTab = (int) ((FRAME_MINI_HEIGHT - maxBounds.height) * 0.35);
            int errorHeightClips = (int) ((FRAME_MINI_HEIGHT - maxBounds.height) * 0.15);
            videoTab.setPreferredSize(new Dimension(videoTab.getWidth(), videoTab.getHeight() - errorHeightTab));
            clips.setPreferredSize(new Dimension(clips.getWidth(), clips.getHeight() - errorHeightClips));
        }

        if (w < FRAME_MINI_WIDTH) {
            int errorWidthRightPanel = (int) ((FRAME_MINI_WIDTH - w) * 0.35);
            int errorWidthFileCombox = (int) ((FRAME_MINI_WIDTH - w) * 0.6);
            rightPanel.setPreferredSize(new Dimension(rightPanel.getWidth() - errorWidthRightPanel, rightPanel.getHeight()));
            fileCombox.setPreferredSize(new Dimension(fileCombox.getWidth() - errorWidthFileCombox, fileCombox.getHeight()));
        }
    }

private void maxbtnMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_maxbtnMouseClicked
// TODO add your handling code here:
    reposition();
}//GEN-LAST:event_maxbtnMouseClicked

    /**
     * 
     */
    @Action
    public void showOptionFrame() {
        if (optionFrame != null) {
            optionFrame.dispose();
            optionFrame = null;
        }
        JFrame mainFrame = FYPRushEditingApp.getApplication().getMainFrame();
        optionFrame = new OptionFrame(mainFrame, true);
        optionFrame.setLocationRelativeTo(mainFrame);
        FYPRushEditingApp.getApplication().show(optionFrame);
    }

    /**
     * 
     */
    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = FYPRushEditingApp.getApplication().getMainFrame();
            aboutBox = new FYPRushEditingAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        FYPRushEditingApp.getApplication().show(aboutBox);
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel VideoPanel;
    private javax.swing.JButton aboutbtn;
    private javax.swing.JLabel audioInfo;
    private javax.swing.JScrollPane clips;
    private javax.swing.JPanel controlPanel;
    private javax.swing.JLabel durationLB;
    private javax.swing.JSlider durationSlider;
    private javax.swing.JButton editbtn;
    private javax.swing.JButton exitbtn;
    private javax.swing.JButton fastbtn;
    private javax.swing.JPanel fdPanel;
    private javax.swing.JComboBox fileCombox;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler11;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.Box.Filler filler9;
    private javax.swing.JButton firstbtn;
    private javax.swing.JLabel generalInfo;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton lastbtn;
    private javax.swing.JLabel lbpadding;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton maxbtn;
    private javax.swing.JButton openfile;
    private javax.swing.JButton optionbtn;
    private javax.swing.JButton pausebtn;
    private javax.swing.JButton playbtn;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JLabel progressLB;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JPanel scrollInternalPanel;
    private javax.swing.JSlider soundSlider;
    private javax.swing.JButton speaker;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JLabel titleLB;
    private javax.swing.JLabel videoInfo;
    private javax.swing.JPanel videoOuter;
    private javax.swing.JTabbedPane videoTab;
    // End of variables declaration//GEN-END:variables
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    //my variables
    private VideoPlayer player;
    private String videoPath;
    private String fileName;
    private String videoDirectory;
    private VideoInfo vinfo;
    private Thread videoThread;
    private boolean isMute;
    private ProcessController controller;
    private Options options;
    private final ChartPanel chartPanel;
    private final Dimension frameSize;
    private ChartFrame chartFrame;
    private JDialog aboutBox;
    private JDialog optionFrame;
    private final static int FRAME_MINI_WIDTH = 1280;
    private final static int FRAME_MINI_HEIGHT = 1024;
    private final static int LEAST_FRAME_MINI_HEIGHT = 750;
    private final static double DEFAULT_DIMENSION_RATIO = FRAME_MINI_WIDTH / (double) FRAME_MINI_HEIGHT;
    private Point prePt;
    private int comboxIndex;
}
