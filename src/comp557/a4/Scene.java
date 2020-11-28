package comp557.a4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;

/**
 * Simple scene loader based on XML file format.
 */
public class Scene {
    
    /** List of surfaces in the scene */
    public List<Intersectable> surfaceList = new ArrayList<Intersectable>();
	
	/** All scene lights */
	public Map<String,Light> lights = new HashMap<String,Light>();

    /** Contains information about how to render the scene */
    public Render render;
    
    /** The ambient light colour */
    public Color3f ambient = new Color3f();

    /** 
     * Default constructor.
     */
    public Scene() {
    	this.render = new Render();
    }
    
    /**
     * renders the scene
     */
    public void render(boolean showPanel) {
 
        Camera cam = render.camera; 
        int w = cam.imageSize.width;
        int h = cam.imageSize.height;
        
        render.init(w, h, showPanel);
        
        for ( int j = 0; j < h && !render.isDone(); j++ ) {
            for ( int i = 0; i < w && !render.isDone(); i++ ) {
            	
                // TODO: Objective 1: generate a ray (use the generateRay method)
                Ray ray = new Ray();
                generateRay(i, j, new double[]{0, 0}, cam, ray);
            	
                // TODO: Objective 2: test for intersection with scene surfaces
                IntersectResult intersectResult = new IntersectResult();
                for (Intersectable surface : surfaceList) {
                    IntersectResult temp = new IntersectResult();
                    surface.intersect(ray, temp);
                    if (temp.t < intersectResult.t) {
                        intersectResult = temp;
                    }
                }

            	
                // TODO: Objective 3: compute the shaded result for the intersection point (perhaps requiring shadow rays)
                
            	// Here is an example of how to calculate the pixel value.
                Color3f c = new Color3f(render.bgcolor);
                int r = (int)(255*c.x);
                int g = (int)(255*c.y);
                int b = (int)(255*c.z);
                int a = 255;
                int argb = (a<<24 | r<<16 | g<<8 | b);
                if (intersectResult.t != Double.POSITIVE_INFINITY) {
                    // todo: alpha not calculated?
                    r = 0;
                    g = 0;
                    b = 0;
                    for (Light light : lights.values()) {
                        int[] lightResult = lightCalculation(light, cam, intersectResult);
                        r += lightResult[0];
                        g += lightResult[1];
                        b += lightResult[2];
                    }
                    r = Math.min(255, r);
                    g = Math.min(255, g);
                    b = Math.min(255, b);

                    argb = (a<<24 | r<<16 | g<<8 | b);
                }
                // update the render image
                render.setPixel(i, j, argb);

            }
        }
        
        // save the final render image
        render.save();
        
        // wait for render viewer to close
        render.waitDone();
        
    }

    private int[] lightCalculation(Light light, Camera camera, IntersectResult intersectResult) {
        Vector3d lightDirection = new Vector3d();
        lightDirection.sub(light.from, intersectResult.p);
        lightDirection.normalize();
        Vector3d cameraDirection = new Vector3d();
        cameraDirection.sub(camera.from, intersectResult.p);
        cameraDirection.normalize();
        Vector3d halfVector = new Vector3d();
        halfVector.add(lightDirection, cameraDirection);
        halfVector.normalize();
        Material material = intersectResult.material;

        int r = lightCalculationChannel('r', light, lightDirection, material, intersectResult.n, halfVector);
        int g = lightCalculationChannel('g', light, lightDirection, material, intersectResult.n, halfVector);
        int b = lightCalculationChannel('b', light, lightDirection, material, intersectResult.n, halfVector);
        return new int[]{r, g, b};
    }

    private int lightCalculationChannel(char channel, Light light, Vector3d lightDirection, Material material, Vector3d normal, Vector3d halfVector) {
        float diffuse = 0;
        float lightColor = 0;
        float specularCoef = 0;
        switch (channel) {
            case 'r':
                diffuse = material.diffuse.x;
                lightColor = light.color.x;
                specularCoef = material.specular.x;
                break;
            case 'g':
                diffuse = material.diffuse.y;
                lightColor = light.color.y;
                specularCoef = material.specular.y;
                break;
            case 'b':
                diffuse = material.diffuse.z;
                lightColor = light.color.z;
                specularCoef = material.specular.z;
                break;
        }

        double lambertian = lambertian(diffuse, light.power, lightColor, normal, lightDirection);
        double blinnphong = blinnphong(specularCoef, light.power, lightColor, normal, halfVector, material.shinyness);
        return (int) (255 * (lambertian + blinnphong));
    }

    private double lambertian(float diffuse, double power, float lightColor, Vector3d normal, Vector3d lightDirection) {
        return diffuse * power * lightColor * Math.max(0, normal.dot(lightDirection));
    }

    private double blinnphong(float specularCoef, double power, float lightColor, Vector3d normal, Vector3d halfVector, float shininess) {
        return specularCoef * power * lightColor * Math.pow(Math.max(0, normal.dot(halfVector)), shininess);
    }

    private int ambient() {
        // todo: ambient
        return 0;
    }
    /**
     * Generate a ray through pixel (i,j).
     * 
     * @param i The pixel row.
     * @param j The pixel column.
     * @param offset The offset from the center of the pixel, in the range [-0.5,+0.5] for each coordinate. 
     * @param cam The camera.
     * @param ray Contains the generated ray.
     */
	public static void generateRay(final int i, final int j, final double[] offset, final Camera cam, Ray ray) {
		
		// TODO: Objective 1: generate rays given the provided parmeters
        Vector3d w = new Vector3d();
        w.sub(cam.from, cam.to);
        w.normalize();
        Vector3d u = new Vector3d();
        u.cross(cam.up, w);
        u.normalize();
        Vector3d v = new Vector3d();
        v.cross(w, u);
        v.normalize();
        double d = cam.imageSize.height / Math.tan(cam.fovy/2) /2;
        double xViewPlaneAdjust = cam.imageSize.width/2;
        double yViewPlaneAdjust = cam.imageSize.height/2;

        Vector3d s = new Vector3d();
        // x highest at the right, lowest at the left, 0 at the center
        s.scaleAdd(i - xViewPlaneAdjust + offset[0], u, cam.from);
        // j highest at the top, lowest at the bottom, 0 at the center
        s.scaleAdd(- j + yViewPlaneAdjust + offset[1], v, s);
        s.scaleAdd(-d, w, s);
        Vector3d direction = new Vector3d();
        direction.sub(s, cam.to);
        ray.set(cam.from, direction);
	}

	/**
	 * Shoot a shadow ray in the scene and get the result.
	 * 
	 * @param result Intersection result from raytracing. 
	 * @param light The light to check for visibility.
	 * @param root The scene node.
	 * @param shadowResult Contains the result of a shadow ray test.
	 * @param shadowRay Contains the shadow ray used to test for visibility.
	 * 
	 * @return True if a point is in shadow, false otherwise. 
	 */
	public static boolean inShadow(final IntersectResult result, final Light light, final SceneNode root, IntersectResult shadowResult, Ray shadowRay) {
		
		// TODO: Objective 5: check for shdows and use it in your lighting computation
		
		return false;
	}    
}
