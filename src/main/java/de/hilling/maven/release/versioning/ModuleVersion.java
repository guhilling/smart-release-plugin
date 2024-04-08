package de.hilling.maven.release.versioning;

import java.time.ZonedDateTime;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public abstract class ModuleVersion {

    public abstract ZonedDateTime getReleaseDate();

    public abstract String getReleaseTag();

    public abstract ImmutableQualifiedArtifact getArtifact();

    public abstract ImmutableFixVersion getVersion();

    @Override
    public String toString() {
        return getArtifact() + "-"  + getVersion().toString() + "-" + getReleaseTag();
    }
}
