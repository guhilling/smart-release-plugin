package e2e;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import java.io.File;
import java.io.IOException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

/**
 * This test actually downloads multiple versions of maven and runs the plugin against them.
 */
public class MavenCompatibilityTest {

    @Rule
    public TestProject testProject = new TestProject(ProjectType.SINGLE);

    @Test
    public void maven_4_0_0() throws Exception {
        buildProjectWithMavenVersion("4.0.0-alpha-12");
    }

    @Test
    public void maven_3_9_6() throws Exception {
        buildProjectWithMavenVersion("3.9.6");
    }

    @Test
    public void maven_3_8_8() throws Exception {
        buildProjectWithMavenVersion("3.8.8");
    }

    @Test
    public void maven_3_6_3() throws Exception {
        buildProjectWithMavenVersion("3.6.3");
    }

    private void buildProjectWithMavenVersion(String mavenVersionToTest) throws IOException, MavenInvocationException {
        String expected = "1.0";
        testProject.setMvnRunner(MvnRunner.mvn(mavenVersionToTest));
        testProject.mvnReleaseComplete();
        assertArtifactInLocalRepo(TestUtils.TEST_GROUP_ID, "single-module", expected);
        assertThat(new File(testProject.localDir, "target/single-module-" + expected + ".jar").exists(), is(true));
    }

}
