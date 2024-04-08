package e2e;

import scaffolding.TestProject;

import static de.hilling.maven.release.TestUtils.PREPARE_GOAL;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;

public class TestRunningTest {

    @Rule
    public TestProject projectWithTestsThatFail = new TestProject(ProjectType.MODULE_WITH_TEST_FAILURE);

    @Test
    public void ifTestsAreSkippedYouCanReleaseWithoutRunningThem() throws IOException {
        projectWithTestsThatFail.mvn("-DtestBehaviour=skipTests", PREPARE_GOAL);
    }
}
