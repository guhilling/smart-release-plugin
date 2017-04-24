package de.hilling.maven.release;

/**
 * Preload jacoco data writer classes.
 */
public class JacocoLoader {

    static {
        try {
            Class.forName("org.jacoco.agent.rt.internal_8ff85ea.core.data.ExecutionDataWriter");
            Class.forName("org.jacoco.agent.rt.internal_8ff85ea.core.internal.data.CompactDataOutput");
            Class.forName("org.jacoco.agent.rt.internal_8ff85ea.core.data.SessionInfo");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("unable to preload jacoco execution data collector classes", e);
        }

    }

}
