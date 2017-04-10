package de.hilling.maven.release;

import static java.util.Arrays.asList;

import org.apache.maven.model.Scm;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

public class GitHelper {

    public static final String NEED_TO_USE_GIT = "Cannot run the release plugin with a non-Git version control system";
    public static final String GIT_PREFIX = "scm:git:";

    public static boolean hasLocalTag(Git repo, String tagToCheck) throws GitAPIException {
        return tag(repo, new EqualsMatcher(tagToCheck)) != null;
    }

    public static Ref tag(Git repo, EqualsMatcher matcher) throws GitAPIException {
        for (Ref ref : repo.tagList().call()) {
            String currentTag = ref.getName().replace("refs/tags/", "");
            if (matcher.matches(currentTag)) {
                return ref;
            }
        }
        return null;
    }

    public static String scmUrlToRemote(String scmUrl) throws ValidationException {
        if (!scmUrl.startsWith(GIT_PREFIX)) {
            throw new ValidationException(NEED_TO_USE_GIT, asList(NEED_TO_USE_GIT, "The value in your scm tag is " + scmUrl));
        }
        String remote = scmUrl.substring(GIT_PREFIX.length());
        remote  = remote.replace("file://localhost/", "file:///");
        return remote;
    }

    static String getRemoteUrlOrNullIfNoneSet(Scm originalScm, Scm actualScm) throws ValidationException {
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

    private static class EqualsMatcher {
        private final String tagToCheck;

        public EqualsMatcher(String tagToCheck) {
            this.tagToCheck = tagToCheck;
        }

        public boolean matches(String tagName) {
            return tagToCheck.equals(tagName);
        }
    }
}
