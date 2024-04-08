package de.hilling.maven.release;

import org.apache.maven.project.MavenProject;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

import de.hilling.maven.release.versioning.ImmutableModuleVersion;

@Gson.TypeAdapters
@Value.Immutable
public interface ReleasableModule {
    String getRelativePathToModule();

    MavenProject getProject();

    ImmutableModuleVersion getImmutableModule();

    boolean isToBeReleased();
}
