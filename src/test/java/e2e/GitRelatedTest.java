package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.MvnRunner;
import scaffolding.Photocopier;
import scaffolding.RandomNameGenerator;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.CountMatcher.twoOf;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class GitRelatedTest {

    @Rule
    public TestProject testProject = new TestProject(ProjectType.SINGLE);
    @Rule
    public TestProject scmTagProject = new TestProject(ProjectType.TAGGED_MODULE);

    @Before
    public void setUp() {
        testProject.checkClean = false;
        scmTagProject.checkClean = false;
    }

    @Test
    public void ifTheReleaseIsRunFromANonGitRepoThenAnErrorIsClearlyDisplayed() throws IOException {
        File projectRoot = Photocopier.copyTestProjectToTemporaryLocation("single-module", RandomNameGenerator
                                                                                               .getInstance().randomName());
        TestProject.performPomSubstitution(projectRoot);
        try {
            new MvnRunner().runMaven(projectRoot, TestUtils.PREPARE_GOAL);
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Releases can only be performed from Git repositories.")));
            assertThat(e.output, oneOf(containsString(projectRoot.getCanonicalPath() + " is not a Git repository.")));
        }
    }

    @Test
    public void ifTheScmIsSpecifiedButIsNotGitThenThisIsThrown() throws GitAPIException, IOException, InterruptedException {
        scmTagProject.checkNoChanges = false;
        File pom = new File(scmTagProject.localDir, "pom.xml");
        String xml = FileUtils.readFileToString(pom, "UTF-8");
        xml = xml.replace("scm:git:", "scm:svn:");
        FileUtils.writeStringToFile(pom, xml, "UTF-8");
        scmTagProject.local.add().addFilepattern("pom.xml").call();
        scmTagProject.local.commit().setMessage("Changing pom for test").call();

        try {
            scmTagProject.mvnReleaseComplete();
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
            assertThat(e.output, twoOf(containsString("Cannot run the release plugin with a non-Git version control system")));
            assertThat(e.output, oneOf(containsString("The value in your scm tag is scm:svn:")));
        }
    }

}
