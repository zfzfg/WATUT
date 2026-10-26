package com.corosus.watut.client.emf;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

/**
 * Talks to EMF Compat Core only when those classes are on the runtime classpath.
 * The core jar is not shipped inside WATUT.
 */
final class EmfPoseBridge {

    static final String SOURCE = "watut";

    private static final int UNTRIED = 0;
    private static final int READY = 1;
    private static final int ABSENT = 2;

    private static int state = UNTRIED;
    private static Constructor<?> snapshotPart;
    private static Constructor<?> snapshotRotation;
    private static Method savePoses;
    private static Method clearPoses;
    private static Method localFirstPerson;

    private EmfPoseBridge() {
    }

    static boolean available() {
        if (state == UNTRIED) {
            bind();
        }
        return state == READY;
    }

    static boolean isLocalPlayerInFirstPerson(UUID uuid) {
        if (!available()) {
            return false;
        }
        try {
            return (boolean) localFirstPerson.invoke(null, uuid);
        } catch (ReflectiveOperationException e) {
            state = ABSENT;
            return false;
        }
    }

    static Object snapshot(ModelPart part, boolean rotationOnly) {
        try {
            return rotationOnly ? snapshotRotation.newInstance(part, true) : snapshotPart.newInstance(part);
        } catch (ReflectiveOperationException e) {
            state = ABSENT;
            return null;
        }
    }

    static void save(UUID uuid, Object leftArm, Object rightArm, Map<String, Object> parts, Vector3f bodyBase) {
        if (!available()) {
            return;
        }
        try {
            savePoses.invoke(null, uuid, SOURCE, leftArm, rightArm, parts, bodyBase);
        } catch (ReflectiveOperationException e) {
            state = ABSENT;
        }
    }

    static void clear(UUID uuid) {
        if (!available()) {
            return;
        }
        try {
            clearPoses.invoke(null, uuid, SOURCE);
        } catch (ReflectiveOperationException e) {
            state = ABSENT;
        }
    }

    private static void bind() {
        try {
            Class<?> snapshot = Class.forName("strm.emfcompat.core.PoseSnapshot");
            Class<?> manager = Class.forName("strm.emfcompat.core.PoseManager");
            Class<?> core = Class.forName("strm.emfcompat.core.EMFCompatCore");
            snapshotPart = snapshot.getConstructor(ModelPart.class);
            snapshotRotation = snapshot.getConstructor(ModelPart.class, boolean.class);
            savePoses = manager.getMethod("savePoses", UUID.class, String.class, snapshot, snapshot, Map.class, Vector3f.class);
            clearPoses = manager.getMethod("clearPoses", UUID.class, String.class);
            localFirstPerson = core.getMethod("isLocalPlayerInFirstPerson", UUID.class);
            state = READY;
        } catch (Throwable ignored) {
            state = ABSENT;
        }
    }
}
