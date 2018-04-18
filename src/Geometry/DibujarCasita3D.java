package Geometry;

import java.awt.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.JFrame;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import Math.Matrix4x4;
import Math.Vector4;
import Math.Projection;
import Math.Translation;
import Math.Uvn;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * This example reads the description of an object (a polygon) from a file
 * and draws it on a jPanel.
 * 
 * 
 * @author mzabala1
 */
public class DibujarCasita3D extends JPanel implements KeyListener {
    
    static JFrame frame;
    static DibujarCasita3D dc;
    public static final boolean DEBUG = true;
    static int contador = 0;

    /**
     * Original (untransformed) PolygonObject
     */
    PolygonObject po;
    /**
     * Transformed object to be drawn
     */
    PolygonObject transformedObject;
    
    /**
     * Current transformations.
     * This is the accumulation of transformations done to the object
     */
    static Matrix4x4 currentTransformation = new Matrix4x4();

    public static int FRAME_WIDTH = 900;
    public static int FRAME_HEIGHT = 600;
    
    public static int AXIS_SIZE = 20;

    Dimension size;
    Graphics2D g2d;
    /**
     * Distance to the projection plane.
     */
    int proyectionPlaneDistance;
    
    /**
     * Center of the object
     */
    double maxX;
    double minX;
    double maxY;
    double minY;
    double maxZ;
    double minZ;
    double centerX;
    double centerY;
    static double centerZ;
    
    /**
     * Position of the camera in spherical coordinates
     */
    static double theta = 0;
    static double phi = 0;
    static double radius = 500;
    
    /**
     * Increments
     */
    public static final double THETA_INCREMENT = Math.PI / 18d;
    public static final double PHI_INCREMENT = Math.PI / 18d;
    public static final double RADIUS_INCREMENT = 18d;
    
    /**
     * This method draws the object.
     * The graphics context is received in variable Graphics.
     * It is necessary to cast the graphics context into Graphics 2D in order
     * to use Java2D.
     * @param g Graphics context
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g2d = (Graphics2D) g;
        // Size of the window.
        size = getSize();
        
        // Draw the X axis
        g2d.setColor(Color.RED);
        drawOneLine(-DibujarCasita3D.AXIS_SIZE, 0, DibujarCasita3D.AXIS_SIZE, 0);

        // Draw the Y axis
        g2d.setColor(Color.GREEN);
        drawOneLine(0, -DibujarCasita3D.AXIS_SIZE, 0, DibujarCasita3D.AXIS_SIZE);

        // Draw the polygon object
        g2d.setColor(Color.BLUE);
        //po.drawObject(this);
        
        // Transform the object
        transformObject();
        
        // Apply UVN matrix
        applyUVN();
        
        // Apply projection
        applyProjection();

        // Draw the object
        transformedObject.drawObject(this);
        
    }
    
    /**
     * Apply the current transformation to the original object.
     * currentTransformation is the accumulation of the transforms that
     * the user has entered.
     */
    private void transformObject() {
        transformedObject = PolygonObject.transformObject(po, currentTransformation);
    }
    
    /**
     * Based on the position and orientation of the camera, create and apply
     * the UVN matrix.
     */
    private void applyUVN() {
        double yCamera = radius * Math.sin(phi) + centerY;
        double projectedR = radius * Math.cos(phi);
        double xCamera = projectedR * Math.sin(theta) + centerX;
        double zCamera = projectedR * Math.cos(theta) + centerZ;
        
        
        Vector4 cameraPos = new Vector4(xCamera, yCamera, zCamera);
        Vector4 objectCenter = new Vector4(centerX, centerY, centerZ);
        Vector4 V = new Vector4(0, 1, 0);

        Uvn uvnMat = new Uvn(cameraPos, objectCenter, V);
        
        transformedObject = PolygonObject.transformObject(transformedObject, uvnMat);
    }
    
    /**
     * Create and apply the projection matrix
     */
    private void applyProjection() {
        Projection proj = new Projection(- proyectionPlaneDistance);
        transformedObject = PolygonObject.transformObject(transformedObject, proj);
    }

    /**
     * This function draws one line on this JPanel.
     * A mapping is done in order to:
     * - Have the Y coordinate grow upwards
     * - Have the origin of the coordinate system in the middle of the panel
     *
     * @param x1 Starting x coordinate of the line to be drawn
     * @param y1 Starting y coordinate of the line to be drawn
     * @param x2 Ending x coordinate of the line to be drawn
     * @param y2 Ending x coordinate of the line to be drawn
     */
    public void drawOneLine(int x1, int y1, int x2, int y2) {

        x1 = x1 + size.width / 2;
        x2 = x2 + size.width / 2;

        y1 = size.height / 2 - y1;
        y2 = size.height / 2 - y2;

        g2d.drawLine(x1, y1, x2, y2);
    }

    /**
     * Read the description of the object from the given file
     * @param fileName Name of the file with the object description
     */
    public void readObjectDescription(String fileName) {
        Scanner in;
        po = new PolygonObject();
        try {
            in = new Scanner(new File(fileName));
            // Read the number of vertices
            int numVertices = in.nextInt();
            Vector4[] vertexArray = new Vector4[numVertices];
            // Read the vertices
            for (int i = 0; i < numVertices; i++) {
                // Read a vertex
                int x = in.nextInt();
                int y = in.nextInt();
                int z = in.nextInt();
                vertexArray[i] = new Vector4(x, y, z);
                if(i == 0) {
                    initializeMaxMin(vertexArray[i]);
                } else {
                    updateMaxMin(vertexArray[i]);
                }
            }
            // Compute the center of the object
            computeCenter();
            // Read the number of edges
            int numEdges = in.nextInt();
            // Read the edges
            for (int i = 0; i < numEdges; i++) {
                // Read an edge
                int start = in.nextInt();
                int end = in.nextInt();
                Edge edge = new Edge(vertexArray[start], vertexArray[end]);
                po.addEdge(edge);
            }
            // Read the Project Plane Distance to the virtual camera
            proyectionPlaneDistance = in.nextInt();
            radius = proyectionPlaneDistance;
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }

    }

    /**
     * Prepare to find the minimum and maximum corners of the object
     * @param v 
     */
    private void initializeMaxMin(Vector4 v) {
        minX = v.getX();
        maxX = v.getX();
        minY = v.getY();
        maxY = v.getY();
        minZ = v.getZ();
        maxZ = v.getZ();
    }
    
    /**
     * Update the minimum and maximum corners of the object
     * @param v 
     */
    private void updateMaxMin(Vector4 v) {
        if(v.getX() > maxX) maxX = v.getX();
        if(v.getY() > maxY) maxY = v.getY();
        if(v.getZ() > maxZ) maxZ = v.getZ();
        if(v.getX() < minX) minX = v.getX();
        if(v.getY() < minY) minY = v.getY();
        if(v.getZ() < minZ) minZ = v.getZ();
    }
    
    /**
     * Compute the middle of the object
     */
    private void computeCenter() {
        centerX = (minX + maxX) / 2;
        centerY = (minY + maxY) / 2;
        centerZ = (minZ + maxZ) / 2;
    }
    
    @Override
  public void keyReleased(KeyEvent ke) {
      System.out.println("Key Released");      
      repaint();
  }
  
    @Override
  public void keyPressed(KeyEvent ke) {
      System.out.println("Key Pressed");
      if(ke.getKeyCode() == KeyEvent.VK_A) {        // Left
        Translation trans = new Translation(-10, 0, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
      } else if(ke.getKeyCode() == KeyEvent.VK_D) { // Right
        Translation trans = new Translation(10, 0, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
      } else if(ke.getKeyCode() == KeyEvent.VK_W) { // Up
        Translation trans = new Translation(0, 10, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
      } else if(ke.getKeyCode() == KeyEvent.VK_S) { // Down
        Translation trans = new Translation(0, -10, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
      } else if(ke.getKeyCode() == KeyEvent.VK_R) { // Reset
        currentTransformation = new Matrix4x4();
      } else if(ke.getKeyCode() == KeyEvent.VK_J) { // change longitude
        theta -= THETA_INCREMENT;
        if(theta <= - Math.PI) theta = - Math.PI;
      } else if(ke.getKeyCode() == KeyEvent.VK_L) { // change longitude
        theta += THETA_INCREMENT;
        if(theta >= Math.PI) theta = Math.PI;
      } else if(ke.getKeyCode() == KeyEvent.VK_I) { // change latitude
        phi += PHI_INCREMENT;
        if(phi >= Math.PI / 2) phi =  Math.PI / 2 - PHI_INCREMENT;
      } else if(ke.getKeyCode() == KeyEvent.VK_K) { // change latitude
        phi -= PHI_INCREMENT;
        if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
      } else if(ke.getKeyCode() == KeyEvent.VK_DOWN) { // change latitude
        radius += RADIUS_INCREMENT;
        //if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
      } else if(ke.getKeyCode() == KeyEvent.VK_UP) { // change latitude
        radius -= RADIUS_INCREMENT;
        //if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
      } else if(ke.getKeyCode() == KeyEvent.VK_RIGHT) { // change latitude
        centerX += RADIUS_INCREMENT;
        //if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
      } else if(ke.getKeyCode() == KeyEvent.VK_LEFT) { // change latitude
        centerX -= RADIUS_INCREMENT;
        //if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
      }
  } 
  
    @Override
  public void keyTyped(KeyEvent ke) {
      System.out.println("Key Typed");
  }
  
    private static javax.swing.JButton atB;
    private static javax.swing.JButton bajB;
    private static javax.swing.JPanel botonesP;
    private static javax.swing.JButton derB;
    private static javax.swing.JButton dmB;
    public static javax.swing.JPanel graficoP;
    private static javax.swing.JButton izqB;
    private static javax.swing.JButton riB;
    private static javax.swing.JButton roB;
    private static javax.swing.JButton subB;
    private static javax.swing.JButton casa;
    private static javax.swing.JButton barco;
    private static javax.swing.JButton atras;
    private static javax.swing.JButton roJ;
    private static javax.swing.JButton roL;
    
    /**
     * Create the frame, create the panel, add the panel to the frame,
     * make everything vissible.
     * @param args 
     */
    public static void main(String[] args) {
        dc = new DibujarCasita3D();
      
        dc.readObjectDescription("Casa3D.txt");
        frame = new JFrame("Wire Frame Object");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(dc);
        // DibujarCasita will respond to the key events
        frame.addKeyListener(dc);
        frame.setLocationRelativeTo(null); 

        subB = new javax.swing.JButton();
        bajB = new javax.swing.JButton();
        izqB = new javax.swing.JButton();
        derB = new javax.swing.JButton();
        roB = new javax.swing.JButton();
        dmB = new javax.swing.JButton();
        atB = new javax.swing.JButton();
        riB = new javax.swing.JButton();
        casa = new javax.swing.JButton();
        barco = new javax.swing.JButton();
        atras = new javax.swing.JButton();
        roJ = new javax.swing.JButton();
        roL = new javax.swing.JButton();
        
        atras.setText("Volver");
        atras.setToolTipText("Volver al menu");
        atras.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        atras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                atrasActionPerformed(evt);
            }
        });
        
        casa.setText("Casa");
        casa.setToolTipText("Graficar casa");
        casa.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        casa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                casaActionPerformed(evt);
            }
        });

        barco.setText("Barco");
        barco.setToolTipText("Graficar barco");
        barco.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        barco.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                barcoActionPerformed(evt);
            }
        });

        subB.setText("Subir");
        subB.setToolTipText("Mueve hacia arriba el objeto");
        subB.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        subB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subBActionPerformed(evt);
            }
        });

        bajB.setText("Bajar");
        bajB.setToolTipText("Mueve hacia abajo el objeto");
        bajB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bajBActionPerformed(evt);
            }
        });

        izqB.setText("Izquierda");
        izqB.setToolTipText("Mueve hacia la izquierda el objeto");
        izqB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                izqBActionPerformed(evt);
            }
        });

        derB.setText("Derecha");
        derB.setToolTipText("Mueve hacia la derecha el objeto");
        derB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                derBActionPerformed(evt);
            }
        });

        roB.setText("Adelante");
        roB.setToolTipText("Rota el objeto hacia adelante");
        roB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roBActionPerformed(evt);
            }
        });

        dmB.setText("Acercar");
        dmB.setToolTipText("Acercarce al objeto");
        dmB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dmBActionPerformed(evt);
            }
        });

        atB.setText("Alejar");
        atB.setToolTipText("Alejarse del objeto");
        atB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                atBActionPerformed(evt);
            }
        });

        riB.setText("Atras");
        riB.setToolTipText("Rota el objeto hacia atras");
        riB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                riBActionPerformed(evt);
            }
        });
        
        roJ.setText("Rotar Derecha");
        roJ.setToolTipText("Rota el objeto hacia izq");
        roJ.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roJActionPerformed(evt);
            }
        });
        
        roL.setText("Rotar Izquierda");
        roL.setToolTipText("Rota el objeto hacia der");
        roL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                roLActionPerformed(evt);
            }
        });
        
        JPanel panelFecha = new JPanel();
        panelFecha.setLayout(new FlowLayout());
        panelFecha.add(subB);
        panelFecha.add(bajB);
        panelFecha.add(izqB);
        panelFecha.add(derB);
        panelFecha.add(roB);
        panelFecha.add(dmB);
        panelFecha.add(atB);
        panelFecha.add(riB);
        panelFecha.add(roJ);
        panelFecha.add(roL);
        
        JPanel panelGrafica = new JPanel();
        panelGrafica.setLayout(new FlowLayout());
        panelGrafica.add(new JLabel("Elija el objeto que desea graficar: "));
        panelGrafica.add(casa);
        panelGrafica.add(barco);
        
        JPanel panel = new JPanel();
        panelGrafica.setLayout(new FlowLayout());
        panelGrafica.add(atras);
        
        //frame.setSize(Main.FRAME_WIDTH, Main.FRAME_HEIGHT);
        //panelFecha.setSize(100,200);
        frame.setSize(1200,1200);
        frame.add(panelFecha, BorderLayout.NORTH);
        frame.add(panelGrafica, BorderLayout.SOUTH);
        frame.add(panel, BorderLayout.EAST);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        
        // Asignarle tamaño
        frame.setSize(DibujarCasita3D.FRAME_WIDTH, DibujarCasita3D.FRAME_HEIGHT);
        // Put the frame in the middle of the window
        frame.setLocationRelativeTo(null);
        // Show the frame
        frame.setVisible(true);
    }
    
    private static void atBActionPerformed(java.awt.event.ActionEvent evt) {                                   
        radius += RADIUS_INCREMENT;
        frame.repaint();
    }                                   

    private static void subBActionPerformed(java.awt.event.ActionEvent evt) {                                     
        Translation trans = new Translation(0, 10, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
        frame.repaint();
    }                                    

    private static void izqBActionPerformed(java.awt.event.ActionEvent evt) {                                     
        Translation trans = new Translation(-10, 0, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
        frame.repaint();
    }                                    

    private static void riBActionPerformed(java.awt.event.ActionEvent evt) {                                    
        phi += PHI_INCREMENT;
        //if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
        frame.repaint();
    }                                   

    private static void dmBActionPerformed(java.awt.event.ActionEvent evt) {                                    
        radius -= RADIUS_INCREMENT;
        //if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
        frame.repaint();
    }                                   

    private static void roBActionPerformed(java.awt.event.ActionEvent evt) {                                    
        phi -= PHI_INCREMENT;
        //if(phi <= - Math.PI / 2) phi = - Math.PI / 2 + PHI_INCREMENT;
        frame.repaint();
    }                                   

    private static void derBActionPerformed(java.awt.event.ActionEvent evt) {                                     
        Translation trans = new Translation(10, 0, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
        frame.repaint();
    }                                    

    private static void bajBActionPerformed(java.awt.event.ActionEvent evt) {                                     
        Translation trans = new Translation(0, -10, 0);
        currentTransformation = Matrix4x4.times(currentTransformation, trans);
        frame.repaint();
    }
    
        private static void roJActionPerformed(java.awt.event.ActionEvent evt) {                                     
        theta -= THETA_INCREMENT;
        frame.repaint();
    }
        
        private static void roLActionPerformed(java.awt.event.ActionEvent evt) {                                     
        theta += THETA_INCREMENT;
        frame.repaint();
    }
    
    private static void casaActionPerformed(java.awt.event.ActionEvent evt) {                                     
       dc.readObjectDescription("Casa3D.txt");
       frame.repaint();
    }                                    

    private static void barcoActionPerformed(java.awt.event.ActionEvent evt) {                                     
        dc.readObjectDescription("Barco3D.txt");
        frame.repaint();
    }
    
    private static void atrasActionPerformed(java.awt.event.ActionEvent evt) {                                     
        frame.setVisible(false);
        Principal p = new Principal();
    }
}
