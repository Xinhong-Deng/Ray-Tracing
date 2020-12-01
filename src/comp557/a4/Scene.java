package comp557.a4;

import java.util.*;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3d;
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

    private final int MAX_DEPTH = 3;
    private final double REFLECTION_RATIO = 0.8;
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
        Random generator = new Random(System.currentTimeMillis());
        for ( int j = 0; j < h && !render.isDone(); j++ ) {
            for ( int i = 0; i < w && !render.isDone(); i++ ) {
            	int r = 0;
            	int g = 0;
            	int b = 0;
                int a = 255;

                // TODO: Objective 1: generate a ray (use the generateRay method)
                // TODO: Objective 2: test for intersection with scene surfaces - merge with objective 3 in rayTracing()
                // TODO: Objective 3: compute the shaded result for the intersection point (perhaps requiring shadow rays)

                // uniform antialiasing
                for (int nsy = 0; nsy < render.samples; nsy ++) {
                    for (int nsx = 0; nsx < render.samples; nsx ++) {
                        double ry = (nsy + 0.5) / render.samples - 0.5;
                        double rx = (nsx + 0.5) / render.samples - 0.5;

                        Ray ray = new Ray();
                        generateRay(i, j, new double[]{rx, ry}, cam, ray);

                        IntersectResult intersectResult = new IntersectResult();
                        double[] result = rayTracing(ray, intersectResult, cam, 0, new Color3f(render.bgcolor));

                        r += Math.min(255, (int)((result[0]) * 255));
                        g += Math.min(255, (int)((result[1]) * 255));
                        b += Math.min(255, (int)((result[2]) * 255));
                    }

                }

            	r /= Math.pow(render.samples, 2);
                g /= Math.pow(render.samples, 2);
                b /= Math.pow(render.samples, 2);

                if (render.jitter) {
                    r += (int)((generator.nextDouble() - 0.5) * 2);
                    g += (int)((generator.nextDouble() - 0.5) * 2);
                    b += (int)((generator.nextDouble() - 0.5) * 2);
                }

                int argb = (a<<24 | r<<16 | g<<8 | b);
                // update the render image
                render.setPixel(i, j, argb);

            }
        }
        
        // save the final render image
        render.save();
        
        // wait for render viewer to close
        render.waitDone();
        
    }

    private double[] rayTracing(Ray ray, IntersectResult intersectResult, Camera camera, int depth, Color3f bgColor) {
        double[] result = new double[]{bgColor.x, bgColor.y, bgColor.z};
        if (depth > MAX_DEPTH) {
            return result;
        }

        for (Intersectable surface : surfaceList) {
            surface.intersect(ray, intersectResult);
        }

        if (intersectResult.t >= Double.POSITIVE_INFINITY) {
            return result;
        }

        if (intersectResult.material.name.equals("mirror")) {
            // mirror reflection
            result[0] = 0;
            result[1] = 0;
            result[2] = 0;
            Vector3d reflectionDirection = new Vector3d();
            Vector3d temp = new Vector3d(intersectResult.n);
            temp.scale(2 * intersectResult.n.dot(ray.viewDirection));
            reflectionDirection.sub(ray.viewDirection, temp);

            Ray reflectionRay = new Ray(intersectResult.p, reflectionDirection);
            IntersectResult intersectResult1 = new IntersectResult();
            double[] tempResult = rayTracing(reflectionRay, intersectResult1, camera, depth + 1, bgColor);
            result[0] += REFLECTION_RATIO * tempResult[0];
            result[1] += REFLECTION_RATIO * tempResult[1];
            result[2] += REFLECTION_RATIO * tempResult[2];
        } else {

            // diffusion
            result[0] = intersectResult.material.diffuse.x * ambient.x;
            result[1] = intersectResult.material.diffuse.y * ambient.y;
            result[2] = intersectResult.material.diffuse.z * ambient.z;
            for (Light light : lights.values()) {
                double[] lightResult = new double[]{0, 0, 0};
                switch (light.type) {
                    case "point":
                        lightResult = pointLightCalculation(intersectResult, light, camera);
                        break;
                    case "area":
                        lightResult = areaLightCalculation(intersectResult, (AreaLight) light, camera);
                        break;
                }
                result[0] += lightResult[0];
                result[1] += lightResult[1];
                result[2] += lightResult[2];
            }
        }
        return result;
    }

    private double[] pointLightCalculation(IntersectResult intersectResult, Light light, Camera camera) {
        if (surfaceList.get(0) instanceof SceneNode) {
            IntersectResult shadowResult = new IntersectResult();
            Ray shadowRay = new Ray();
            if (inShadow(intersectResult, light, (SceneNode) surfaceList.get(0), shadowResult, shadowRay)) {
                return new double[]{0, 0, 0};
            }
        }

        return lightCalculation(light, camera, intersectResult);

    }

    private double[] areaLightCalculation(IntersectResult intersectResult, AreaLight light, Camera camera) {
        double[] lightResult = new double[]{0, 0, 0};
        for (int ys = 0; ys < light.samples; ys++) {
            for (int xs = 0; xs < light.samples; xs++) {
                Vector3d samplingLightPoint = new Vector3d(light.from);
                double xOffset = ((0.5 + xs)/light.samples - 0.5 ) * Math.sqrt(light.area);
                double yOffset = ((0.5 + ys)/light.samples - 0.5 ) * Math.sqrt(light.area);
                samplingLightPoint.scaleAdd(xOffset, light.xDirection, samplingLightPoint);
                samplingLightPoint.scaleAdd(yOffset, light.yDirection, samplingLightPoint);
                Light samplingLight = new Light(light);
                samplingLight.from = new Point3d(samplingLightPoint);
                double[] temp = pointLightCalculation(intersectResult, samplingLight, camera);
                lightResult[0] += temp[0];
                lightResult[1] += temp[1];
                lightResult[2] += temp[2];
            }

        }
        lightResult[0] /= Math.pow(light.samples, 2);
        lightResult[1] /= Math.pow(light.samples, 2);
        lightResult[2] /= Math.pow(light.samples, 2);
        return lightResult;
    }

    private double[] lightCalculation(Light light, Camera camera, IntersectResult intersectResult) {
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

        double r = lightCalculationChannel('r', light, lightDirection, material, intersectResult.n, halfVector);
        double g = lightCalculationChannel('g', light, lightDirection, material, intersectResult.n, halfVector);
        double b = lightCalculationChannel('b', light, lightDirection, material, intersectResult.n, halfVector);
        return new double[]{r, g, b};
    }

    private double lightCalculationChannel(char channel, Light light, Vector3d lightDirection, Material material, Vector3d normal, Vector3d halfVector) {
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
        return lambertian + blinnphong;
    }

    private double lambertian(float diffuse, double power, float lightColor, Vector3d normal, Vector3d lightDirection) {
        return diffuse * power * lightColor * Math.max(0, normal.dot(lightDirection));
    }

    private double blinnphong(float specularCoef, double power, float lightColor, Vector3d normal, Vector3d halfVector, float shininess) {
        return specularCoef * power * lightColor * Math.pow(Math.max(0, normal.dot(halfVector)), shininess);
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
        s.scaleAdd((double)i - xViewPlaneAdjust + offset[0], u, cam.from);
        // j highest at the top, lowest at the bottom, 0 at the center
        s.scaleAdd((double)- j + yViewPlaneAdjust + offset[1], v, s);
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
        double offset = 0.00001;
        Vector3d pointOfIntersect = new Vector3d(result.p);

        Vector3d direction = new Vector3d();
		direction.sub(light.from, pointOfIntersect);
		double tLight = direction.length() - offset;
		direction.normalize();
		shadowRay.viewDirection = direction;

        // offset the eyepoint to avoid self-shadowing
        shadowRay.eyePoint = new Point3d();
        shadowRay.eyePoint.scaleAdd(offset, shadowRay.viewDirection, pointOfIntersect);


		boolean inShadow = false;
		for (Intersectable object : root.children) {
		    object.intersect(shadowRay, shadowResult);
		    if (shadowResult.t < tLight) {
		        // light ray occlude by object
                // but the shadowResult is not the closet intersection
                inShadow = true;
                break;
            }
        }

		return inShadow;
	}    
}
