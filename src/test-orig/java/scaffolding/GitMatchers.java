package scaffolding;

import static scaffolding.TestProject.head;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import de.hilling.maven.release.AnnotatedTag;
import de.hilling.maven.release.utils.Guard;
import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;
import de.hilling.maven.release.versioning.GsonFactory;
import de.hilling.maven.release.versioning.ImmutableFixVersion;
import de.hilling.maven.release.versioning.ImmutableModuleVersion;
import de.hilling.maven.release.versioning.ImmutableQualifiedArtifact;
import de.hilling.maven.release.versioning.ImmutableReleaseInfo;
import de.hilling.maven.release.versioning.VersionMatcher;

public class GitMatchers {

    private static final GsonFactory GSON_FACTORY = new GsonFactory();

    public static Matcher<Git> hasTag(final String tag) {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo, Description mismatchDescription) {
                try {
                    mismatchDescription
                        .appendValueList("a git repo with tags: ", ", ", "", repo.getRepository().getTags().keySet());
                    return hasLocalTag(repo, tag);
                } catch (GitAPIException e) {
                    throw new RuntimeException("Couldn't access repo", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a git repo with the tag " + tag);
            }
        };
    }

    public static Matcher<Git> hasTagWithModuleVersion(String groupId, final String moduleName, String version) {
        final ImmutableFixVersion expectedVersion = new VersionMatcher(version).fixVersion();
        final ImmutableQualifiedArtifact artifact = ImmutableQualifiedArtifact.builder().groupId(groupId)
                                                                              .artifactId(moduleName).build();
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git repo, Description mismatchDescription) {
                try {
                    final ArrayList<String> foundVersions = new ArrayList<>();
                    for (Ref ref : repo.tagList().call()) {
                        final AnnotatedTag tag = fromRef(repo.getRepository(), ref);
                        final Optional<ImmutableModuleVersion> version = tag.getReleaseInfo()
                                                                            .versionForArtifact(artifact);
                        if (version.isPresent()) {
                            final ImmutableModuleVersion moduleVersion = version.get();
                            if (moduleVersion.getVersion().equals(expectedVersion)) {
                                return true;
                            } else {
                                foundVersions.add(moduleVersion.getVersion().toString());
                            }
                        }
                    }
                    mismatchDescription
                        .appendValueList("a git repo containing tags with module versions [", ", ", "]", foundVersions);
                    return false;
                } catch (GitAPIException | IOException e) {
                    throw new RuntimeException("Couldn't access repo", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(
                    "a git repo with tag containing module '" + moduleName + "' " + "with version " + expectedVersion
                                                                                                          .toString());
            }
        };
    }

    public static Matcher<Git> isInSynchWithOrigin() {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git git, Description mismatchDescription) {
                try {
                    Repository repo = git.getRepository();
                    ObjectId fetchHead = repo.resolve("origin/master^{tree}");
                    ObjectId head = repo.resolve("HEAD^{tree}");

                    ObjectReader reader = repo.newObjectReader();
                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                    oldTreeIter.reset(reader, head);
                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                    newTreeIter.reset(reader, fetchHead);
                    List<DiffEntry> diffs = git.diff().setShowNameAndStatusOnly(true).setNewTree(newTreeIter)
                                               .setOldTree(oldTreeIter).call();
                    if (diffs.isEmpty()) {
                        return true;
                    } else {
                        String start = "Detected the following changes in " + git.getRepository().getDirectory()
                                                                                 .getCanonicalPath() + ": ";
                        String end = ".";
                        mismatchDescription.appendValueList(start, ", ", end, diffs.stream().map(DiffEntry::toString)
                                                                                   .collect(Collectors.toList()));
                        return false;
                    }
                } catch (GitAPIException | IOException e) {
                    throw new RuntimeException("Error checking git status", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("A git directory with no difference to its origin");
            }
        };
    }

    public static Matcher<Git> hasCleanWorkingDirectory() {
        return new TypeSafeDiagnosingMatcher<Git>() {
            @Override
            protected boolean matchesSafely(Git git, Description mismatchDescription) {
                try {
                    Status status = git.status().call();
                    if (!status.isClean()) {
                        String start = "Uncommitted changes in ";
                        String end = " at " + git.getRepository().getWorkTree().getAbsolutePath();
                        mismatchDescription.appendValueList(start, ", ", end, status.getUncommittedChanges());
                    }
                    return status.isClean();
                } catch (GitAPIException e) {
                    throw new RuntimeException("Error checking git status", e);
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("A git directory with no staged or unstaged changes");
            }
        };
    }

    public static Matcher<TestProject> hasChangesOnlyInReleaseInfo() {
        return new TypeSafeDiagnosingMatcher<TestProject>() {
            @Override
            protected boolean matchesSafely(TestProject git, Description mismatchDescription) {
                final List<DiffEntry> originEntries = diffDir(git.origin, git.originHeadAtStart);
                boolean success = true;
                if(originEntries.stream().filter(GitMatchers::isNoReleaseInfoDiff)
                             .peek(diff -> mismatchDescription.appendText("unexpected diff: " + diff))
                             .count() > 0) {
                    success = false;
                }
                final List<DiffEntry> localEntries = diffDir(git.local, git.localHeadAtStart);
                if(localEntries.stream().filter(GitMatchers::isNoReleaseInfoDiff)
                                .peek(diff -> mismatchDescription.appendText("unexpected diff: " + diff))
                                .count() > 0) {
                    success = false;
                }
                return success;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("changes only in release-info file");
            }
        };
    }

    private static boolean isNoReleaseInfoDiff(DiffEntry m) {
        return !m.getNewPath().equals(ReleaseInfoStorage.RELEASE_INFO_FILE);
    }

    private static @NonNull
    List<DiffEntry> diffDir(Git git, ObjectId oldCommit) {
        final Repository repository = git.getRepository();
        ObjectId newCommit = head(git);
        try {
            final AbstractTreeIterator oldTree = prepareTreeParser(repository, oldCommit);
            final AbstractTreeIterator newTree = prepareTreeParser(repository, newCommit);
            return git.diff().setOldTree(oldTree).setNewTree(newTree).call();
        } catch (GitAPIException | IOException e) {
            throw new RuntimeException("unable to diff commits", e);
        }
    }

    private static AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(objectId);
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
            try (ObjectReader oldReader = repository.newObjectReader()) {
                oldTreeParser.reset(oldReader, tree.getId());
            }
            walk.dispose();
            return oldTreeParser;
        }
    }

    public static AnnotatedTag fromRef(Repository repository, Ref gitTag) throws IOException {
        Guard.notNull("gitTag", gitTag);

        RevWalk walk = new RevWalk(repository);
        ImmutableReleaseInfo releaseInfo;
        try {
            ObjectId tagId = gitTag.getObjectId();
            RevTag tag = walk.parseTag(tagId);
            releaseInfo = GSON_FACTORY.createGson().fromJson(tag.getFullMessage(), ImmutableReleaseInfo.class);
        } finally {
            walk.dispose();
        }
        return new AnnotatedTag(stripRefPrefix(gitTag.getName()), releaseInfo);
    }

    private static String stripRefPrefix(String refName) {
        return refName.substring("refs/tags/".length());
    }

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

    public static class EqualsMatcher {
        private final String tagToCheck;

        public EqualsMatcher(String tagToCheck) {
            this.tagToCheck = tagToCheck;
        }

        public boolean matches(String tagName) {
            return tagToCheck.equals(tagName);
        }
    }
}
