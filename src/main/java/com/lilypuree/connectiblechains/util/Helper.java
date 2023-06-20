package com.lilypuree.connectiblechains.util;

import com.lilypuree.connectiblechains.ConnectibleChains;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Helper {
    public static ResourceLocation identifier(String name) {
        return new ResourceLocation(ConnectibleChains.MODID, name);
    }

    @Deprecated
    public static double drip(double x, double d) {
        double c = ConnectibleChains.runtimeConfig.getChainHangAmount();
        double b = -c / d;
        double a = c / (d * d);
        return (a * (x * x) + b * x);
    }

    /**
     * For geogebra:
     * a = 9
     * h = 0
     * d = 5
     * p1 = a * asinh( (h / (2*a)) * 1 / sinh(d / (2*a)) )
     * p2 = -a * cosh( (2*p1 - d) / (2*a) )
     * f(x) = p2 + a * cosh( (2*x + 2*p1 - d) / (2*a) )
     *
     * @param x from 0 to d
     * @param d length of the chain
     * @param h height at x=d
     * @return y
     */
    public static double drip2(double x, double d, double h) {
        double a = ConnectibleChains.runtimeConfig.getChainHangAmount();
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        double p2 = -a * Math.cosh((2D * p1 - d) / (2D * a));
        return p2 + a * Math.cosh((((2D * x) + (2D * p1)) - d) / (2D * a));
    }

    /**
     * Derivative of drip2
     * For geogebra:
     * f'(x) = sinh( (2*x + 2*p1 - d) / (2*a) )
     *
     * @param x from 0 to d
     * @param d length of the chain
     * @param h height at x=d
     * @return gradient at x
     */
    public static double drip2prime(double x, double d, double h) {
        double a = ConnectibleChains.runtimeConfig.getChainHangAmount();
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        return Math.sinh((2 * x + 2 * p1 - d) / (2 * a));
    }

    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    public static Vec3 middleOf(Vec3 a, Vec3 b) {
        double x = (a.x() - b.x()) / 2d + b.x();
        double y = (a.y() - b.y()) / 2d + b.y();
        double z = (a.z() - b.z()) / 2d + b.z();
        return new Vec3(x, y, z);
    }

    public static float distanceBetween(Vector3f a, Vector3f b) {
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        float dz = a.z() - b.z();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static float lengthOf(Vector3f v) {
        float x = v.x();
        float y = v.y();
        float z = v.z();
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Get the x/z offset from a chain to a fence
     *
     * @param start fence pos
     * @param end   fence pos
     * @return the x/z offset
     */
    public static Vector3f getChainOffset(Vec3 start, Vec3 end) {
        Vector3f offset = end.subtract(start).toVector3f();
        offset.set(offset.x(), 0, offset.z());
        offset.normalize();
        offset.normalize(2 / 16f);
        return offset;
    }
}
