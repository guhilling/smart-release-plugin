package e2e;

import scaffolding.TestProject;

import org.junit.Rule;
import org.junit.Test;

public class EarProjectTest {

    @Rule
    public TestProject testProject = new TestProject(ProjectType.EAR);

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        testProject.mvnReleasePrepare();
        testProject.mvnInstall();
    }
}
