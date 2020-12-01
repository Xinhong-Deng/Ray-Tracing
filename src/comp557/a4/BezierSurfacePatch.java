package comp557.a4;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BezierSurfacePatch extends Intersectable {

    // think about how to convert the control points to triangular faces
    private Matrix4d coordinatePatch[][];
    private int SUBDIVISION = 12;
    private Point3d vertices[][][];
    private List<Point3d[]> faces = new ArrayList<>();


    public BezierSurfacePatch(String file) {
        super();
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            int numPatches = Integer.parseInt(input.readLine());
            String[] controls;
            int[][][] controlQuads = new int[numPatches][4][4];
            for (int i = 0; i < numPatches; i++) {
                controls = input.readLine().split(",");
                for (int j = 0; j < 4; j++) {
                    for (int k = 0; k < 4; k++) {
                        controlQuads[i][j][k] = Integer.parseInt(controls[j * 4 + k]) - 1;
                    }
                }
            }
            int numPoints = Integer.parseInt(input.readLine());
            float[][] coords = new float[numPoints][3];
            for (int i = 0; i < numPoints; i++) {
                controls = input.readLine().split(",");
                for (int j = 0; j < 3; j++) {
                    coords[i][j] = Float.parseFloat(controls[j]);
                }
            }
            coordinatePatch = new Matrix4d[controlQuads.length][3];
            for (int i = 0; i < controlQuads.length; i++) {
                for (int j = 0; j < 3; j++) {
                    coordinatePatch[i][j] = new Matrix4d();
                    for (int k = 0; k < 4; k++) {
                        for (int l = 0; l < 4; l++) {
                            coordinatePatch[i][j].setElement(k, l, coords[controlQuads[i][k][l]][j]);
                        }
                    }
                }
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        vertices = new Point3d[coordinatePatch.length][SUBDIVISION][SUBDIVISION];

        // get all surface point
        for (int patch = 0; patch < coordinatePatch.length; patch++) {
            int N = SUBDIVISION;
            Vector3d coord, normal;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    double s1 = (double) i / (N - 1);
                    double t1 = (double) j / (N - 1);
                    coord = evaluateCoordinate(s1, t1, patch);
                    vertices[patch][i][j] = new Point3d(coord.x, coord.y, coord.z);
                }
            }
        }

        // construct faces
        for (int patch = 0; patch < coordinatePatch.length; patch++) {
            int N = SUBDIVISION;
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (i < N -1 &&  j < N - 1) {
                        faces.add(new Point3d[]{vertices[patch][i][j],
                                vertices[patch][i+1][j],
                                vertices[patch][i][j+1]
                                });
                    }

                    if (i > 0 && j > 0) {
                        faces.add(new Point3d[]{vertices[patch][i][j],
                                vertices[patch][i-1][j],
                                vertices[patch][i][j-1]});
                    }
                }
            }
        }

    }

    private double getBerstein(int n, int i, double t) {
        // since order for each parameter is 3, use pre-calculated combination values to improve speed
        Map<Integer, Integer[]> combinationValues = new HashMap<>();
        combinationValues.put(3, new Integer[]{1, 3, 3, 1});
        combinationValues.put(2, new Integer[]{1, 2, 1});
        return combinationValues.get(n)[i] * Math.pow(t, i) * Math.pow((1 - t), (n - i));
    }

    /**
     * returns the xyz coordinates of the Bezier mesh at the parametric point (s,t)
     */
    private Vector3d evaluateCoordinate(double s, double t, int patch) {
        Vector3d result = new Vector3d();
        for (int i = 0; i < 4; i++) {
            Vector3d rowResult = new Vector3d();
            for (int j = 0; j < 4; j++) {
                Matrix4d[] currentPatch = coordinatePatch[patch];
                Vector3d temp = new Vector3d(currentPatch[0].getElement(i, j),
                        currentPatch[1].getElement(i, j),
                        currentPatch[2].getElement(i, j));
                temp.scale(getBerstein(3, j, t));
                rowResult.add(temp);
            }
            rowResult.scale(getBerstein(3, i, s));
            result.add(rowResult);
        }
        return result;
    }


    @Override
    public void intersect(Ray ray, IntersectResult result) {
        for (Point3d[] face : faces) {
            Vector3d ab = new Vector3d();
            ab.sub(face[1], face[0]);
            Vector3d bc = new Vector3d();
            bc.sub(face[2], face[1]);
            Vector3d ca = new Vector3d();
            ca.sub(face[0], face[2]);
            Vector3d normal = new Vector3d();
            normal.cross(ab, bc);
            normal.normalize();

            if (ray.viewDirection.dot(normal) == 0) {
                // no intersection
                continue;
            }

            Vector3d ea = new Vector3d();
            ea.sub(face[0], ray.eyePoint);
            double t = ea.dot(normal) / ray.viewDirection.dot(normal);
            if (t < 1e-9) {
                // t cannot be less than 0
                continue;
            }

            Vector3d x = new Vector3d();
            x.scaleAdd(t, ray.viewDirection, ray.eyePoint);
            Vector3d ax = new Vector3d();
            ax.sub(x, face[0]);
            Vector3d bx = new Vector3d();
            bx.sub(x, face[1]);
            Vector3d cx = new Vector3d();
            cx.sub(x, face[2]);

            Vector3d temp1 = new Vector3d();
            temp1.cross(ab, ax);
            Vector3d temp2 = new Vector3d();
            temp2.cross(bc, bx);
            Vector3d temp3 = new Vector3d();
            temp3.cross(ca, cx);
            if (temp1.dot(normal) > 0 && temp2.dot(normal) > 0 && temp3.dot(normal) > 0) {
                if (t < result.t) {
                    result.t = t;
                    result.n.set(normal);
                    result.n.negate();
                    result.material = material;
                    result.p = new Point3d(x);
                }
            }
        }
    }
}
