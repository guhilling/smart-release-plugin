package de.hilling.maven.release.versioning;

import org.apache.maven.project.MavenProject;

import de.hilling.maven.release.exceptions.ValidationException;

/**
 * Select the follow-up versions for project modules.
 */
public class VersionNamer {

    public static final String PREVIOUS_BUILDS_REQUIRED = "Previous build number required when creating bugfix " + "release.";

    private final boolean     bugfixRelease;
    private final ReleaseInfo previousRelease;

    /**
     * @param bugfixRelease true if a bugfix release should be built.
     * @param previousRelease information about the previous releases of all modules in a project.
     */
    public VersionNamer(boolean bugfixRelease, ReleaseInfo previousRelease) {
        this.bugfixRelease = bugfixRelease;
        this.previousRelease = previousRelease;
    }

    /**
     * Computes the next to be released version for the given project.
     *
     * @param project project to get next version for.
     *
     * @return next version name to use
     *
     * @throws ValidationException if current snapshot is older than previously released version
     */
    public FixVersion nextVersion(MavenProject project) throws ValidationException {
        return new ProjectVersionNamer(project, previousRelease, bugfixRelease).invoke();
    }
}
