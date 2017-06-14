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
    public void maven_3_0_1() throws Exception {
        buildProjectWithMavenVersion("3.0.1");
    }

    @Test
    public void maven_3_0_4() throws Exception {
        buildProjectWithMavenVersion("3.0.4");
    }

    @Test
    public void maven_3_2_1() throws Exception {
        buildProjectWithMavenVersion("3.2.1");
    }

    @Test
    public void maven_3_3_9() throws Exception {
        buildProjectWithMavenVersion("3.3.9");
    }

    private void buildProjectWithMavenVersion(String mavenVersionToTest) throws IOException, InterruptedException, MavenInvocationException {
        String expected = "1.0";
        testProject.setMvnRunner(MvnRunner.mvn(mavenVersionToTest));
        testProject.mvnReleaseComplete();
        assertArtifactInLocalRepo(TestUtils.TEST_GROUP_ID, "single-module", expected);
        assertThat(new File(testProject.localDir, "target/single-module-" + expected + ".jar").exists(), is(true));
    }

}
