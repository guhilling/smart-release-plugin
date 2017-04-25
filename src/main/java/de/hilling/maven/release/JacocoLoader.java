package de.hilling.maven.release;

/**
 * Preload jacoco data writer classes.
 * <p>
 *     This class ist used only for jacoco coverage collection.
 * </p>
 */
public class JacocoLoader {

    static {
        try {
            Class.forName("org.jacoco.agent.rt.internal_8ff85ea.core.data.ExecutionDataWriter");
            Class.forName("org.jacoco.agent.rt.internal_8ff85ea.core.internal.data.CompactDataOutput");
            Class.forName("org.jacoco.agent.rt.internal_8ff85ea.core.data.SessionInfo");
        } catch (ClassNotFoundException e) {
            // Ignored. Used only for coverage collection.
        }

    }

}
