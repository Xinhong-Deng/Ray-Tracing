/*
 * Name: Sandra Deng
 * McGill ID: 260770487
 */

package comp557.a4;

import javax.vecmath.Vector3d;

/**
 * Class for a plane at y=0.
 * 
 * This surface can have two materials.  If both are defined, a 1x1 tile checker 
 * board pattern should be generated on the plane using the two materials.
 */
public class Plane extends Intersectable {
    
	/** The second material, if non-null is used to produce a checker board pattern. */
	Material material2;
	
	/** The plane normal is the y direction */
	public static final Vector3d n = new Vector3d( 0, 1, 0 );
    
    /**
     * Default constructor
     */
    public Plane() {
    	super();
    }

        
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    
        // TODO: Objective 4: intersection of ray with plane
    	// the plane pass through (0, 0, 0) with normal (0, 1, 0)
        double dDotN = ray.viewDirection.dot(n);
        if (Math.abs(dDotN) < 1e-9) {
            // no intersection
            return;
        }

        Vector3d lineOnPlane = new Vector3d(ray.eyePoint);
        lineOnPlane.negate();
        double t = lineOnPlane.dot(n) / dDotN;
        if (t < 1e-9 || t > result.t) {
            return;
        }

        result.t = t;
        result.p.scaleAdd(result.t, ray.viewDirection, ray.eyePoint);
        result.n = new Vector3d(n);
        result.material = material;
        if (material2 == null) {
            return;
        }

        if ((Math.floor(result.p.x) + Math.floor(result.p.z))%2 != 0) {
            result.material = material2;
        }

    }
    
}
