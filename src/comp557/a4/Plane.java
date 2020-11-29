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

        if (material2 == null) {
            result.material = material;
            return;
        }

        // todo: need to fix the color!!
        int xOffset = ((int) (Math.abs(result.p.x)/2)) * 2 + 1;
        int zOffset = ((int) (Math.abs(result.p.z)/2)) * 2 + 1;
        double shiftedX = result.p.x;
        double shiftedZ = result.p.z;
        if (result.p.x < 0) {
            shiftedX += xOffset;
        } else {
            shiftedX -= xOffset;
        }
        if (result.p.z < 0) {
            shiftedZ += zOffset;
        } else {
            shiftedZ -= zOffset;
        }

        if (( shiftedX > 0 && shiftedZ > 0) || (shiftedX < 0 && shiftedZ < 0)) {
            result.material = material;
        } else {
            result.material = material2;
        }

//        int xOffset = ((int) (Math.abs(result.p.x)/2)) * 2 + 1;
//        int zOffset = ((int) (Math.abs(result.p.z)/2)) * 2 + 1;
//        double shiftedX = result.p.x;
//        double shiftedZ = result.p.z;
//        if (result.p.x < 0) {
//            shiftedX += xOffset;
//        } else {
//            shiftedX -= xOffset;
//        }
//        if (result.p.z < 0) {
//            shiftedZ += zOffset;
//        } else {
//            shiftedZ -= zOffset;
//        }
//
//        if (( shiftedX > 0 && shiftedZ > 0) || (shiftedX < 0 && shiftedZ < 0)) {
//            result.material = material;
//        } else {
//            result.material = material2;
//        }

    }
    
}
