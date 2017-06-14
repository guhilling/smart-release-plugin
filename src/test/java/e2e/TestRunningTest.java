package e2e;

import scaffolding.MavenExecutionException;
import scaffolding.TestProject;

import static de.hilling.maven.release.TestUtils.RELEASE_GOAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class TestRunningTest {

    @Rule
    public TestProject projectWithTestsThatFail = new TestProject(ProjectType.MODULE_WITH_TEST_FAILURE);
    private String     expectedTagName          = "";

    @Before
    public void setUp() {
        expectedTagName = TestUtils.tagNameStart();
    }

    @Test
    public void doesNotReleaseIfThereAreTestFailuresAndNoTagsAreWritten() throws Exception {
        try {
            projectWithTestsThatFail.mvnReleaseComplete();
            Assert.fail("Should have failed");
        } catch (MavenExecutionException e) {
        }
        assertThat(projectWithTestsThatFail.local.tagList().call(), empty());
        assertThat(projectWithTestsThatFail.origin.tagList().call(), empty());
    }

    @Test
    public void ifTestsAreSkippedYouCanReleaseWithoutRunningThem() throws IOException {
        projectWithTestsThatFail.mvn("-DtestBehaviour=skipTests", RELEASE_GOAL);
    }
}
