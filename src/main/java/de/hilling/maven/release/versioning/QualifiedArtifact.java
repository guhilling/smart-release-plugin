package de.hilling.maven.release.versioning;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

/**
 * Maven identifier:
 * groupid and artifact id.
 */
@Gson.TypeAdapters
@Value.Immutable
public abstract class QualifiedArtifact {

    public abstract String getGroupId();

    public abstract String getArtifactId();

    @Override
    public String toString() {
        return getGroupId() + ":" + getArtifactId();
    }

}
