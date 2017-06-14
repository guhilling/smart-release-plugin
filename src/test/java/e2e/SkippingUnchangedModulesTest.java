package e2e;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static scaffolding.CountMatcher.noneOf;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class SkippingUnchangedModulesTest {

    @Rule
    public TestProject testProject = new TestProject(ProjectType.DEEP_DEPENDENCIES);

    private static final String GROUP_ID= TestUtils.TEST_GROUP_ID + ".deepdependencies";

    @Test
    public void changesInTheRootAreDetected() throws Exception {
        TestProject simple = TestProject.project(ProjectType.SINGLE);
        simple.mvnReleaseComplete();
        simple.commitRandomFile(".");
        List<String> output = simple.mvnReleaseComplete();
        assertThat(output, noneOf(containsString("No changes have been detected in any modules")));
        assertThat(output, noneOf(containsString("Will use version 1.0")));
    }

    @Test
    public void doesNotReReleaseAModuleThatHasNotChanged() throws Exception {
        List<String> initialBuildOutput = testProject.mvnReleaseComplete();
        assertTagExists("deep-dependencies-aggregator", "1.0");
        assertTagExists("parent-module", "1.0");
        assertTagExists("core-utils", "2.0");
        assertTagExists("console-app", "3.0");
        assertTagExists("more-utils", "10.0");

        assertThat(initialBuildOutput, oneOf(containsString("Releasing core-utils 2.0 as at least one dependency has changed")));
        assertThat(initialBuildOutput, oneOf(containsString("Releasing console-app 3.0 as at least one dependency has changed")));

        testProject.commitRandomFile("console-app").push();
        List<String> output = testProject.mvnReleaseComplete();
        assertTagExists("console-app", "3.1");
        assertTagDoesNotExist("parent-module", "1.1");
        assertTagDoesNotExist("core-utils", "2.1");
        assertTagDoesNotExist("more-utils", "10.1");
        assertTagDoesNotExist("deep-dependencies-aggregator", "1.1");

        assertThat(output, oneOf(containsString("Going to release console-app 3.1")));
        assertThat(output, noneOf(containsString("Going to release parent-module")));
        assertThat(output, noneOf(containsString("Going to release core-utils")));
        assertThat(output, noneOf(containsString("Going to release more-utils")));
        assertThat(output, noneOf(containsString("Going to release deep-dependencies-aggregator")));
    }

    @Test
    public void ifThereHaveBeenNoChangesThenReReleaseAllModules() throws Exception {
        List<String> firstBuildOutput = testProject.mvnReleaseComplete();
        assertThat(firstBuildOutput, noneOf(containsString("No changes have been detected in any modules so will re-release them all")));
        List<String> secondBuildOutput = testProject.mvnReleaseComplete();
        assertThat(secondBuildOutput, oneOf(containsString("No changes have been detected in any modules so will re-release them all")));

        assertTagExists("console-app", "3.1");
        assertTagExists("parent-module", "1.1");
        assertTagExists("core-utils", "2.1");
        assertTagExists("more-utils", "10.1");
        assertTagExists("deep-dependencies-aggregator", "1.1");
    }

    @Test
    public void ifThereHaveBeenNoChangesThenCanOptNotToReleaseAnything() throws Exception {
        List<String> firstBuildOutput = testProject.mvnReleaseComplete();
        List<String> secondBuildOutput = testProject.mvnReleasePrepare("-DnoChangesAction=ReleaseNone");
        assertThat(secondBuildOutput, oneOf(containsString("No changes have been detected in any modules so will not perform release")));

        assertTagExists("console-app", "3.0");
        assertTagExists("parent-module", "1.0");
        assertTagExists("core-utils", "2.0");
        assertTagExists("more-utils", "10.0");
        assertTagExists("deep-dependencies-aggregator", "1.0");
    }

    @Test
    public void ifThereHaveBeenNoChangesThenCanOptToFailTheBuild() throws Exception {
        List<String> firstBuildOutput = testProject.mvnReleaseComplete();
        assertThat(firstBuildOutput, noneOf(containsString("No changes have been detected in any modules so will not perform release")));
        try {
            testProject.mvnReleaseComplete("-DnoChangesAction=FailBuild");
            fail("Expected an exception to be thrown");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("No module changes have been detected"));
        }
    }

    @Test
    public void ifADependencyHasNotChangedButSomethingItDependsOnHasChangedThenTheDependencyIsReReleased() throws Exception {
        testProject.mvnReleaseComplete();
        testProject.commitRandomFile("more-utilities").push();
        List<String> output = testProject.mvnReleaseComplete();

        assertTagExists("console-app", "3.1");
        assertTagDoesNotExist("parent-module", "1.1");
        assertTagExists("core-utils", "2.1");
        assertTagExists("more-utils", "10.1");
        assertTagDoesNotExist("deep-dependencies-aggregator", "1.1");

        assertThat(output, oneOf(containsString("Going to release console-app 3.1")));
        assertThat(output, noneOf(containsString("Going to release parent-module")));
        assertThat(output, oneOf(containsString("Going to release core-utils")));
        assertThat(output, oneOf(containsString("Going to release more-utils")));
        assertThat(output, noneOf(containsString("Going to release deep-dependencies-aggregator")));
    }

    private void assertTagExists(String module, String version) {
        assertThat(testProject.local, hasTagWithModuleVersion(GROUP_ID, module, version));
        assertThat(testProject.origin, hasTagWithModuleVersion(GROUP_ID, module, version));
    }

    private void assertTagDoesNotExist(String module, String version) {
        assertThat(testProject.local, not(hasTagWithModuleVersion(GROUP_ID, module, version)));
        assertThat(testProject.origin, not(hasTagWithModuleVersion(GROUP_ID, module, version)));
    }

}
