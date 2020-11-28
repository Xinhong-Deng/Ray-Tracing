package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A simple sphere class.
 */
public class Sphere extends Intersectable {
    
	/** Radius of the sphere. */
	public double radius = 1;
    
	/** Location of the sphere center. */
	public Point3d center = new Point3d( 0, 0, 0 );
    
    /**
     * Default constructor
     */
    public Sphere() {
    	super();
    }
    
    /**
     * Creates a sphere with the request radius and center. 
     * 
     * @param radius
     * @param center
     * @param material
     */
    public Sphere( double radius, Point3d center, Material material ) {
    	super();
    	this.radius = radius;
    	this.center = center;
    	this.material = material;
    }
    
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    
        // TODO: Objective 2: intersection of ray with sphere
	    Vector3d pVector = new Vector3d(ray.eyePoint);
	    double dDotP = ray.viewDirection.dot(pVector);
		double dDotD = ray.viewDirection.dot(ray.viewDirection);
		double pDotP = pVector.dot(pVector);

		if (Math.pow(dDotP, 2) - dDotD * (pDotP - this.radius) < 0) {
			return;
		}
		double sqartResult = Math.sqrt(Math.pow(dDotP, 2) - dDotD * (pDotP - this.radius));
		double t1 = (-dDotP + sqartResult) / dDotD;
		double t2 = (-dDotP - sqartResult) / dDotD;

		if (t1 < 0 && t2 < 0) {
			return;
		}

		double t = 0;
		if (t1 < 0) {
			t = t2;
		} else if (t2 < 0) {
			t = t1;
		} else {
			t = Math.min(t1, t2);
		}

		result.t = t;
		result.p.scaleAdd(t, ray.viewDirection, ray.eyePoint);
		result.material = this.material;
		result.n.sub(result.p, center);
		result.n.normalize();
    }
    
}
