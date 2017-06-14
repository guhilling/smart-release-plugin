package e2e;

import scaffolding.TestProject;

import static de.hilling.maven.release.TestUtils.RELEASE_GOAL;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.GitMatchers.hasTag;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;
import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;
import de.hilling.maven.release.versioning.ImmutableFixVersion;
import de.hilling.maven.release.versioning.ImmutableModuleVersion;
import de.hilling.maven.release.versioning.ImmutableReleaseInfo;
import de.hilling.maven.release.versioning.ReleaseInfo;

/**
 * Tests for simple layout with only one maven module.
 */
public class SingleModuleTest {

    private static final String EXPECTED_VERSION        = "1.0";
    private static final String EXPECTED_MANUAL_VERSION = "3.0";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.SINGLE);

    @Test
    public void canUpdateSnapshotVersionToReleaseVersion() throws Exception {
        List<String> outputLines = testProject.mvnReleaseComplete();
        assertThat(outputLines, oneOf(containsString("Going to release single-module " + EXPECTED_VERSION)));
    }

    @Test
    public void honorMinimalMajorVersionOnManualSnapshotUpdate() throws Exception {
        testProject.mvnReleaseComplete();
        final String content = testProject.readFile(".", "pom.xml");
        testProject.commitFile(".", "pom.xml", content.replaceAll("1-SNAPSHOT", "3-SNAPSHOT"));
        List<String> outputLines = testProject.mvnReleaseComplete();
        assertThat(outputLines, oneOf(containsString("Going to release single-module " + EXPECTED_MANUAL_VERSION)));
    }


    @Test
    public void localRepoIsCleanWithoutBuild() throws IOException, GitAPIException {
    }

    @Test
    public void localRepoIsCleanAfterReleaseBuild() throws IOException, GitAPIException {
        testProject.mvn(RELEASE_GOAL);
    }

    @Test
    public void theReleaseNumbersWillStartAt0AndThenIncrement() throws IOException, GitAPIException {
        testProject.mvnReleaseComplete();
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.0"));
        testProject.mvnReleaseComplete();
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.1"));
        testProject.mvnReleaseComplete();
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.2"));
    }

    @Test
    public void theReleaseNumbersWillStartAt0AndThenIncrementTakingIntoAccountManuallyUpdatedReleaseInfoFiles() throws
                                                                                                                Exception {
        testProject.mvnReleaseComplete();
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.0"));

        final ReleaseInfoStorage infoStorage = new ReleaseInfoStorage(testProject.localDir, testProject.local);
        final ReleaseInfo currentInfo = infoStorage.load();
        final ImmutableReleaseInfo.Builder releaseBuilder = ImmutableReleaseInfo.builder().from(currentInfo);
        final ImmutableModuleVersion currentModuleVersion = currentInfo.getModules().get(0);
        final ImmutableModuleVersion.Builder moduleInfo = ImmutableModuleVersion.builder().from(currentModuleVersion);
        final ImmutableFixVersion.Builder versionBuilder = ImmutableFixVersion.builder()
                                                                              .from(currentModuleVersion.getVersion());
        releaseBuilder.modules(singletonList(moduleInfo.version(versionBuilder.minorVersion(5L).build()).build()));
        infoStorage.store(releaseBuilder.build());

        testProject.mvn(RELEASE_GOAL);
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "single-module", "1.6"));
    }

    @Test
    public void theTagNameIsActuallyStoredInReleaseInfo() throws Exception {
        testProject.mvnReleaseComplete();
        final ReleaseInfo currentInfo = currentReleaseInfo();
        String expectedTag = expectedTag();
        assertThat(testProject.local, hasTag(expectedTag));
        assertThat(testProject.origin, hasTag(expectedTag));
    }

    public String expectedTag() {
        return currentReleaseInfo().getTagName().get();
    }

    public ReleaseInfo currentReleaseInfo() {
        final ReleaseInfoStorage infoStorage = new ReleaseInfoStorage(testProject.localDir, testProject.local);
        try {
            return infoStorage.load();
        } catch (MojoExecutionException e) {
            throw new RuntimeException("info access failed");
        }
    }

    @Test
    public void onlyLocalGitRepoIsTaggedWithoutPush() throws IOException, InterruptedException {
        testProject.checkClean = false;
        testProject.mvn("-Dpush=false", RELEASE_GOAL);
        assertThat(testProject.local, hasTag(expectedTag()));
        assertThat(testProject.origin, not(hasTag(expectedTag())));
    }

}
