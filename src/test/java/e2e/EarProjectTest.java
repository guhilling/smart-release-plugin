package e2e;

import scaffolding.TestProject;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class EarProjectTest {

    @Rule
    public TestProject testProject = new TestProject(ProjectType.EAR);

    @Before
    public void setUp() {
        testProject.mvnReleaseComplete();
    }

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "parent-project", "1.0"));
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "project-ear", "3.0"));
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "project-ejb", "3.0"));
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "project-war", "2.0"));
        assertThat(testProject.local, hasTagWithModuleVersion(TestUtils.TEST_GROUP_ID, "project-lib", "2.0"));
    }

    @Test
    public void buildsAndInstallsEarWithCorrectVersions() throws Exception {
        final ZipFile zipFile = new ZipFile(testProject.getFile("project-ear", "target/project-ear-3.0.ear"));
        assertThat(zipFile.stream().map(ZipEntry::getName).collect(toList()), hasItems("project-ejb.jar",
                                                                                       "project.war",
                                                                                       "project-lib-2.0.jar"));
    }
}
