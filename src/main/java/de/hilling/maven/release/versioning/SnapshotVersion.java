package de.hilling.maven.release.versioning;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public interface SnapshotVersion {
    /**
     * @return Major version as used in maven dependencies.
     */
    long getMajorVersion();

    default String versionAsString() {
        return getMajorVersion() + "-SNAPSHOT";
    }

}
