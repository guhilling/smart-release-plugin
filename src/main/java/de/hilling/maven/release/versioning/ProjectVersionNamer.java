package de.hilling.maven.release.versioning;

import java.util.Optional;

import org.apache.maven.project.MavenProject;

import de.hilling.maven.release.ValidationException;

/**
 * Select the next version for a specific module in a multi module build.
 */
class ProjectVersionNamer {
    private final ReleaseInfo  previousRelease;
    private final boolean      bugfixRelease;
    private final SnapshotVersion currentSnapshot;
    private ImmutableQualifiedArtifact artifact;

    /**
     * @param project the current module.
     * @param previousRelease information for the latest release of all modules.
     * @param bugfixRelease true if a bugfix release is to be built.
     */
    ProjectVersionNamer(MavenProject project, ReleaseInfo previousRelease, boolean bugfixRelease) {
        currentSnapshot = new VersionMatcher(project.getVersion()).snapshotVersion();
        artifact = ImmutableQualifiedArtifact.builder()
                                             .groupId(project.getGroupId())
                                             .artifactId(project.getArtifactId())
                                             .build();
        this.previousRelease = previousRelease;
        this.bugfixRelease = bugfixRelease;
    }

    FixVersion invoke() {
        final Optional<ImmutableModuleVersion> previousVersion = previousRelease.versionForArtifact(artifact);
        checkProjectVersion(previousVersion);
        return previousVersion.map(this::followupVersion).orElseGet(this::initialVersion);
    }

    private void checkProjectVersion(Optional<ImmutableModuleVersion> previousVersion) {
        if (previousVersion.isPresent() && previousVersion.get().getVersion().getMajorVersion() > currentSnapshot
                                                                                                      .getMajorVersion()) {
            throw new ValidationException("snapshot version is older than stored previous version");
        }
    }

    private FixVersion initialVersion() {
        if (bugfixRelease) {
            throw new ValidationException(VersionNamer.PREVIOUS_BUILDS_REQUIRED);
        }
        return ImmutableFixVersion.builder().majorVersion(currentSnapshot.getMajorVersion()).minorVersion(0)
                                  .build();
    }

    private FixVersion followupVersion(ModuleVersion previousModule) {
        final ImmutableFixVersion.Builder builder = ImmutableFixVersion.builder();
        FixVersion previousVersion = previousModule.getVersion();
        builder.from(previousVersion);
        if (bugfixRelease) {
            builder.bugfixVersion(previousVersion.getBugfixVersion().orElse(0L) + 1);
        } else {
            if (previousVersion.getBugfixVersion().isPresent()) {
                throw new IllegalStateException("can only create bugfixes from bugfix branch");
            }
            selectNextRegularVersion(builder, previousVersion);
        }
        return builder.build();
    }

    private void selectNextRegularVersion(ImmutableFixVersion.Builder builder, FixVersion previousVersion) {
        if (currentSnapshot.getMajorVersion() > previousVersion.getMajorVersion()) {
            builder.majorVersion(currentSnapshot.getMajorVersion());
            builder.minorVersion(0L);
        } else {
            builder.minorVersion(previousVersion.getMinorVersion() + 1);
        }
    }
}
