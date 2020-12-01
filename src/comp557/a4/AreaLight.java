package comp557.a4;

import javax.vecmath.Vector3d;

public class AreaLight extends Light {
    public Vector3d yDirection = new Vector3d(0, 1, 0);
    public Vector3d xDirection = new Vector3d(1, 0, 0);
    public int area = 4;
    public int samples = 9;

    public AreaLight() {
        super();
        type = "area";
    }

}
