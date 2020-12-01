/*
 * Name: Sandra Deng
 * McGill ID: 260770487
 */

package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.text.DecimalFormat;

/**
 * A simple box class. A box is defined by it's lower (@see min) and upper (@see max) corner.
 */
public class Box extends Intersectable {

    public Point3d max;
    public Point3d min;

    /**
     * Default constructor. Creates a 2x2x2 box centered at (0,0,0)
     */
    public Box() {
        super();
        this.max = new Point3d(1, 1, 1);
        this.min = new Point3d(-1, -1, -1);
    }

    @Override
    public void intersect(Ray ray, IntersectResult result) {
        // TODO: Objective 6: intersection of Ray with axis aligned box
        double[] minValues = tCalculation(this.min, ray.eyePoint, ray.viewDirection);
        double[] maxValues = tCalculation(this.max, ray.eyePoint, ray.viewDirection);

        double txLow = Math.min(minValues[0], maxValues[0]);
        double txHigh = Math.max(minValues[0], maxValues[0]);

        double tyLow = Math.min(minValues[1], maxValues[1]);
        double tyHigh = Math.max(minValues[1], maxValues[1]);

        double tzLow = Math.min(minValues[2], maxValues[2]);
        double tzHigh = Math.max(minValues[2], maxValues[2]);

        double tmin = Math.max(txLow, tyLow);
        tmin = Math.max(tmin, tzLow);
        double tmax = Math.min(txHigh, tyHigh);
        tmax = Math.min(tmax, tzHigh);
        if (tmin > tmax) {
            return;
        }

        if (tmin < 0 || tmin > result.t) {
            return;
        }

        result.t = tmin;
        result.p.scaleAdd(tmin, ray.viewDirection, ray.eyePoint);
        result.material = material;
        if (Math.round(result.p.x * 1e5) / 1e5 == min.x) {
            result.n = new Vector3d(-1, 0, 0);
        } else if (Math.round(result.p.x * 1e5) / 1e5 == max.x) {
            result.n = new Vector3d(1, 0, 0);
        } else if (Math.round(result.p.y * 1e5) / 1e5 == min.y) {
            result.n = new Vector3d(0, -1, 0);
        } else if (Math.round(result.p.y * 1e5) / 1e5 == max.y) {
            result.n = new Vector3d(0, 1, 0);
        } else if (Math.round(result.p.z * 1e5) / 1e5 == min.z) {
            result.n = new Vector3d(0, 0, -1);
        } else if (Math.round(result.p.z * 1e5) / 1e5 == max.z) {
            result.n = new Vector3d(0, 0, 1);
        }

    }

    private double[] tCalculation(Point3d boundary, Point3d eyePoint, Vector3d direction) {
        double x = (boundary.x - eyePoint.x) / direction.x;
        double y = (boundary.y - eyePoint.y) / direction.y;
        double z = (boundary.z - eyePoint.z) / direction.z;
        return new double[]{x, y, z};
    }

}
