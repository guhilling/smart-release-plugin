package de.hilling.maven.release;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import de.hilling.maven.release.versioning.GsonFactory;
import de.hilling.maven.release.versioning.ReleaseInfo;

public class AnnotatedTag {
    private static final GsonFactory GSON_FACTORY = new GsonFactory();

    private final String name;
    private final ReleaseInfo releaseInfo;
    private       Ref         ref;

    public AnnotatedTag(Ref ref, String name, ReleaseInfo releaseInfo) {
        Guard.notBlank("tag name", name);
        Guard.notNull("tag message", releaseInfo);
        this.ref = ref;
        this.name = name;
        this.releaseInfo = releaseInfo;
    }

    public void saveAtHEAD(Git git) throws GitAPIException {
        final String message = GSON_FACTORY.createGson().toJson(releaseInfo);
        git.tag().setName(name).setAnnotated(true).setMessage(message).call();
    }

    public Ref ref() {
        return ref;
    }

    public ReleaseInfo getReleaseInfo() {
        return releaseInfo;
    }

    public String name() {
        return name;
    }
}
