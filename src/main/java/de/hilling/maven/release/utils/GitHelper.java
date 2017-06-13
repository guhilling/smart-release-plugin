package de.hilling.maven.release.utils;

import static java.util.Arrays.asList;

import org.apache.maven.model.Scm;

import de.hilling.maven.release.exceptions.ValidationException;

public class GitHelper {

    public static final String NEED_TO_USE_GIT = "Cannot run the release plugin with a non-Git version control system";
    public static final String GIT_PREFIX = "scm:git:";

    public static String scmUrlToRemote(String scmUrl) throws ValidationException {
        if (!scmUrl.startsWith(GIT_PREFIX)) {
            throw new ValidationException(NEED_TO_USE_GIT, asList(NEED_TO_USE_GIT, "The value in your scm tag is " + scmUrl));
        }
        String remote = scmUrl.substring(GIT_PREFIX.length());
        remote  = remote.replace("file://localhost/", "file:///");
        return remote;
    }

    public static String getRemoteUrlOrNullIfNoneSet(Scm originalScm, Scm actualScm) throws ValidationException {
        if (originalScm == null) {
            // No scm was specified, so don't inherit from any parent poms as they are probably used in different git repos
            return null;
        }

        // There is an SCM specified, so the actual SCM with derived values is used in case (so that variables etc are interpolated)
        String remote = actualScm.getDeveloperConnection();
        if (remote == null) {
            remote = actualScm.getConnection();
        }
        if (remote == null) {
            return null;
        }
        return scmUrlToRemote(remote);
    }
}
