package org.dave.compactmachines3.utility;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.util.EnumFacing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class PropertyUtils {
    public static PropertyDirection createDirection(String name) {
        try {
            // Try static method create(String)
            Method m = PropertyDirection.class.getMethod("create", String.class);
            return (PropertyDirection) m.invoke(null, name);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }

        try {
            // Try create(String, EnumFacing.Plane)
            Class<?> planeClass = Class.forName("net.minecraft.util.EnumFacing$Plane");
            Method m = PropertyDirection.class.getMethod("create", String.class, planeClass);
            Object plane = EnumFacing.Plane.HORIZONTAL;
            return (PropertyDirection) m.invoke(null, name, plane);
        } catch (Exception ignored) {
        }

        try {
            // Try create(String, EnumFacing[])
            Method m = PropertyDirection.class.getMethod("create", String.class, EnumFacing[].class);
            return (PropertyDirection) m.invoke(null, name, (Object) EnumFacing.values());
        } catch (Exception ignored) {
        }

        try {
            // Try create(String, Iterable)
            Method m = PropertyDirection.class.getMethod("create", String.class, Iterable.class);
            return (PropertyDirection) m.invoke(null, name, Arrays.asList(EnumFacing.values()));
        } catch (Exception ignored) {
        }

        // Fallback: try the no-reflection direct call (may fail at runtime if method absent)
        try {
            return PropertyDirection.create(name);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to create PropertyDirection for " + name, t);
        }
    }
}
