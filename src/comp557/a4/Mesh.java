/*
 * Name: Sandra Deng
 * McGill ID: 260770487
 */

package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.HashMap;
import java.util.Map;

public class Mesh extends Intersectable {
	
	/** Static map storing all meshes by name */
	public static Map<String,Mesh> meshMap = new HashMap<String,Mesh>();
	
	/**  Name for this mesh, to allow re-use of a polygon soup across Mesh objects */
	public String name = "";
	
	/**
	 * The polygon soup.
	 */
	public PolygonSoup soup;

	public Mesh() {
		super();
		this.soup = null;
	}			
		
	@Override
	public void intersect(Ray ray, IntersectResult result) {
		
		// TODO: Objective 7: ray triangle intersection for meshes
		// each face is a triangle
		for (int[] face : soup.faceList) {
			Vector3d ab = new Vector3d();
			ab.sub(soup.vertexList.get(face[1]).p, soup.vertexList.get(face[0]).p);
			Vector3d bc = new Vector3d();
			bc.sub(soup.vertexList.get(face[2]).p, soup.vertexList.get(face[1]).p);
			Vector3d ca = new Vector3d();
			ca.sub(soup.vertexList.get(face[0]).p, soup.vertexList.get(face[2]).p);
			Vector3d normal = new Vector3d();
			normal.cross(ab, bc);
			normal.normalize();

			if (ray.viewDirection.dot(normal) == 0) {
				// no intersection
				continue;
			}

			Vector3d ea = new Vector3d();
			ea.sub(soup.vertexList.get(face[0]).p, ray.eyePoint);
			double t = ea.dot(normal) / ray.viewDirection.dot(normal);
			if (t < 1e-9) {
				// t cannot be less than 0
				continue;
			}

			Vector3d x = new Vector3d();
			x.scaleAdd(t, ray.viewDirection, ray.eyePoint);
			Vector3d ax = new Vector3d();
			ax.sub(x, soup.vertexList.get(face[0]).p);
			Vector3d bx = new Vector3d();
			bx.sub(x, soup.vertexList.get(face[1]).p);
			Vector3d cx = new Vector3d();
			cx.sub(x, soup.vertexList.get(face[2]).p);

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
					result.material = material;
					result.p = new Point3d(x);
				}
			}
		}
	}

}
