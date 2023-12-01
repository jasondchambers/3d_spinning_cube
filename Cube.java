/**
 * A Java port of the "Code-It-Yourself! 3D Graphics Engine Part #1 - Triangles & Projection"
 * From @javidx9.
 * 
 * The link to the video is https://www.youtube.com/watch?v=ih20l3pJoeU
 * 
 * If you are interested in learning 3D graphics, javidx9's videos are very approachable.
 * This code is not optimized - it is for educational purposes only. There are optimizations
 * (for example, collapsing the matrix multiplications into one) that are intentionally left
 * out to enable the student to step through.
 * 
 * I wrote this code in Java because I don't run Windows at home (just Mac and Linux). I have nothing
 * against Windows or C++ - I was just too lazy to spin up a virtual Windows machine. I went with Java
 * not because Java is my favourite language (it isn't), but because I wanted to play around with some 
 * of the more recent language features (e.g. Record). Also, Java is widely used in both education and
 * industry. The intent of this exercise was to focus on the foundational math and for the language,
 * frameworks and libraries to not get in the way.
 * 
 * If you want to see this code in action, check out my video https://www.youtube.com/watch?v=yy9ZhE9-b3I
 * 
 * I also recommend this video to discover the power of the matrix https://www.youtube.com/watch?v=vQ60rFwh2ig
 * 
 * (c) Jason Chambers 2023
 **/
import java.util.Arrays;
import java.util.List;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Timer;

record Vec3d(float x, float y, float z) {}
record Triangle(Vec3d p1, Vec3d p2, Vec3d p3) {}
record Mesh(List<Triangle> tris) {}

class MatrixOperations {

    static Vec3d multiplyMatrixVector(Vec3d in, float[][] matrix) {
        float x = in.x() * matrix[0][0] + in.y() * matrix[1][0] + in.z() * matrix[2][0] + matrix[3][0];
        float y = in.x() * matrix[0][1] + in.y() * matrix[1][1] + in.z() * matrix[2][1] + matrix[3][1];
        float z = in.x() * matrix[0][2] + in.y() * matrix[1][2] + in.z() * matrix[2][2] + matrix[3][2];
        float w = in.x() * matrix[0][3] + in.y() * matrix[1][3] + in.z() * matrix[2][3] + matrix[3][3];
        if (w != 0.0f) {
            x /= w;
            y /= w;
            z /= w;
        }
        return new Vec3d(x, y, z);
    }
}

class ProjectionMatrix {
    private float screenWidth;
    private float screenHeight;
    private float fNear;
    private float fFar;
    private float fFov;
    private float fAspectRatio;
    private float fFovRad;
    private float[][] matProj;

    ProjectionMatrix(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        fNear = 0.1f;
        fFar = 1000.0f;
        fFov = 90.0f;
        fAspectRatio = screenHeight / screenWidth;
        fFovRad = 1.0f / (float)Math.tan(fFov * 0.5f / 180.0f * 3.13159f);
        matProj = new float[4][4];
        matProj[0][0] = fAspectRatio * fFovRad; matProj[0][1] = 0.0f;    matProj[0][2] = 0.0f;                              matProj[0][3] = 0.0f;
        matProj[1][0] = 0.0f;                   matProj[1][1] = fFovRad; matProj[1][2] = 0.0f;                              matProj[1][3] = 0.0f;
        matProj[2][0] = 0.0f;                   matProj[2][1] = 0.0f;    matProj[2][2] = fFar / (fFar - fNear);             matProj[2][3] = 1.0f;
        matProj[3][0] = 0.0f;                   matProj[3][1] = 0.0f;    matProj[3][2] = ( -fFar * fNear) / (fFar - fNear); matProj[3][3] = 0.0f;
    }
    
    float[][] getMatProj() {
        return matProj;
    }

    public Triangle translate(Triangle in) {
        Vec3d translated_p1 = new Vec3d(in.p1().x(), in.p1().y(), in.p1().z() + 3.0f);
        Vec3d translated_p2 = new Vec3d(in.p2().x(), in.p2().y(), in.p2().z() + 3.0f);
        Vec3d translated_p3 = new Vec3d(in.p3().x(), in.p3().y(), in.p3().z() + 3.0f);
        return new Triangle(translated_p1, translated_p2, translated_p3);
    }

    public Vec3d scaleIntoView(Vec3d in) {
        float scaled_x = in.x();
        float scaled_y = in.y();
        
        scaled_x += 1.0f; 
        scaled_y += 1.0f;

        scaled_x *= 0.5f * (float) screenWidth;
        scaled_y *= 0.5f * (float) screenHeight;
        return new Vec3d(scaled_x, scaled_y, in.z());
    }
}

public class Cube extends Frame implements ActionListener {
    private Mesh meshCube = new Mesh(Arrays.asList(

        // SOUTH
        new Triangle(new Vec3d(0.0f,0.0f,0.0f), new Vec3d(0.0f,1.0f,0.0f), new Vec3d(1.0f,1.0f,0.0f)),
        new Triangle(new Vec3d(0.0f,0.0f,0.0f), new Vec3d(1.0f,1.0f,0.0f), new Vec3d(1.0f,0.0f,0.0f)),

        // EAST
        new Triangle(new Vec3d(1.0f,0.0f,0.0f), new Vec3d(1.0f,1.0f,0.0f), new Vec3d(1.0f,1.0f,1.0f)),
        new Triangle(new Vec3d(1.0f,0.0f,0.0f), new Vec3d(1.0f,1.0f,1.0f), new Vec3d(1.0f,0.0f,1.0f)),

        // NORTH
        new Triangle(new Vec3d(1.0f,0.0f,1.0f), new Vec3d(1.0f,1.0f,1.0f), new Vec3d(0.0f,1.0f,1.0f)),
        new Triangle(new Vec3d(1.0f,0.0f,1.0f), new Vec3d(0.0f,1.0f,1.0f), new Vec3d(0.0f,0.0f,1.0f)),

        // WEST
        new Triangle(new Vec3d(0.0f,0.0f,1.0f), new Vec3d(0.0f,1.0f,1.0f), new Vec3d(0.0f,1.0f,0.0f)),
        new Triangle(new Vec3d(0.0f,0.0f,1.0f), new Vec3d(0.0f,1.0f,0.0f), new Vec3d(0.0f,0.0f,0.0f)),

        // TOP
        new Triangle(new Vec3d(0,1,0), new Vec3d(0,1,1), new Vec3d(1,1,1)),
        new Triangle(new Vec3d(0,1,0), new Vec3d(1,1,1), new Vec3d(1,1,0)),

        // BOTTOM
        new Triangle(new Vec3d(1,0,1), new Vec3d(0,0,1), new Vec3d(0,0,0)),
        new Triangle(new Vec3d(1,0,1), new Vec3d(0,0,0), new Vec3d(1,0,0))
    ));
    private Timer timer;
    private float fTheta;

    public Cube() {
        setTitle("3D Demo");
        setSize(400, 300);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // Set up a timer to update the animation at regular intervals
        timer = new Timer(100, this); 
        timer.start();
    }

    void drawTriangle(Graphics g, Triangle tri) {
        Graphics2D g2d = (Graphics2D) g;
 
        int x1 = (int)tri.p1().x(); int y1 = (int)tri.p1().y();
        int x2 = (int)tri.p2().x(); int y2 = (int)tri.p2().y();
        int x3 = (int)tri.p3().x(); int y3 = (int)tri.p3().y();

        g2d.drawLine(x1, y1, x2, y2);
        g2d.drawLine(x2, y2, x3, y3);
        g2d.drawLine(x3, y3, x1, y1);
    }

    public void paint(Graphics g) {
        System.out.println("In paint()");
        Graphics2D g2d = (Graphics2D) g;

        // Clear the previous frame
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(Color.BLUE);

        fTheta += 0.1f;

        // Rotate the object on the Z axis
        float[][] matRotZ = new float[4][4];
        matRotZ[0][0] = (float)Math.cos(fTheta);
        matRotZ[0][1] = (float)Math.sin(fTheta);
        matRotZ[1][0] = -(float)Math.sin(fTheta);
        matRotZ[1][1] = (float)Math.cos(fTheta);
        matRotZ[2][2] = 1;
        matRotZ[3][3] = 1;

        // Rotate the object on the X axis
        float[][] matRotX = new float[4][4];
        matRotX[0][0] = 1;
        matRotX[1][1] = (float)Math.cos(fTheta * 0.5f);
        matRotX[1][2] = (float)Math.sin(fTheta * 0.5f);
        matRotX[2][1] = -(float)Math.sin(fTheta * 0.5f);
        matRotX[2][2] = (float)Math.cos(fTheta * 0.5f);
        matRotX[3][3] = 1;

        ProjectionMatrix projectionMatrix = new ProjectionMatrix(this.getWidth(), this.getHeight());
        for (Triangle tri : this.meshCube.tris()) {

            // Rotate the object on the Z axis
            Vec3d rotatedZP1 = MatrixOperations.multiplyMatrixVector(tri.p1(), matRotZ);
            Vec3d rotatedZP2 = MatrixOperations.multiplyMatrixVector(tri.p2(), matRotZ);
            Vec3d rotatedZP3 = MatrixOperations.multiplyMatrixVector(tri.p3(), matRotZ);
            Triangle triRotatedZ = new Triangle(rotatedZP1, rotatedZP2, rotatedZP3);

            // Rotate the object on the X axis
            Vec3d rotatedZXP1 = MatrixOperations.multiplyMatrixVector(triRotatedZ.p1(), matRotX);
            Vec3d rotatedZXP2 = MatrixOperations.multiplyMatrixVector(triRotatedZ.p2(), matRotX);
            Vec3d rotatedZXP3 = MatrixOperations.multiplyMatrixVector(triRotatedZ.p3(), matRotX);
            Triangle triRotatedZX = new Triangle(rotatedZXP1, rotatedZXP2, rotatedZXP3);

            // Ensure the triangle is in front of the viewer
            tri = projectionMatrix.translate(triRotatedZX);

            // Project from 3D to 2D
            Vec3d projectedP1 = MatrixOperations.multiplyMatrixVector(tri.p1(), projectionMatrix.getMatProj());
            Vec3d projectedP2 = MatrixOperations.multiplyMatrixVector(tri.p2(), projectionMatrix.getMatProj());
            Vec3d projectedP3 = MatrixOperations.multiplyMatrixVector(tri.p3(), projectionMatrix.getMatProj());

            // Scale it up
            Vec3d scaledP1 = projectionMatrix.scaleIntoView(projectedP1);
            Vec3d scaledP2 = projectionMatrix.scaleIntoView(projectedP2);
            Vec3d scaledP3 = projectionMatrix.scaleIntoView(projectedP3);
            Triangle triProjectedAndScaled = new Triangle(scaledP1, scaledP2, scaledP3);
            System.out.println(triProjectedAndScaled);

            // Now draw it
            drawTriangle(g, triProjectedAndScaled);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint(); // Request a repaint to show changes
    }

    public static void main(String[] args) {
        Cube cube = new Cube();
        cube.setVisible(true);
    }
}
