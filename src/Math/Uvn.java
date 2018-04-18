/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Math;

/**
 *
 * @author htrefftz
 */
public class Uvn extends Matrix4x4  {
    Vector4 from;
    Vector4 lookAt;
    Vector4 up;

    public Uvn() {
        super();
    }
    
    public Uvn(Vector4 from, Vector4 lookAt, Vector4 up) {
        super();
        Vector4 n,u,v = new Vector4();
        n = Vector4.minus(Vector4.subtract(lookAt, from));
        n.normalize();
        u = Vector4.crossProduct(up, n);
        u.normalize();
        v = Vector4.crossProduct(n, u);
        double x, y, z;
        x = Vector4.dotProduct(Vector4.minus(u), from);
        y = Vector4.dotProduct(Vector4.minus(v), from);
        z = Vector4.dotProduct(Vector4.minus(n), from);
        
        matrix[0][0] = u.getX();matrix[0][1] = u.getY();matrix[0][2] = u.getZ();matrix[0][3] = x;
        matrix[1][0] = v.getX();matrix[1][1] = v.getY();matrix[1][2] = v.getZ();matrix[1][3] = y;
        matrix[2][0] = n.getX();matrix[2][1] = n.getY();matrix[2][2] = n.getZ();matrix[2][3] = z;
        
        
    }
    
    
}
