package e2e;

import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static scaffolding.CountMatcher.oneOf;
import static scaffolding.CountMatcher.twoOf;
import static scaffolding.GitMatchers.hasTagWithModuleVersion;
import static scaffolding.MvnRunner.assertArtifactInLocalRepo;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;
import de.hilling.maven.release.versioning.VersionMatcher;

public class NestedModulesTest {

    private static final String      GROUP_ID                     = TestUtils.TEST_GROUP_ID + ".nested";
    private final        String      expectedAggregatorVersion    = "0.0";
    private final        String      expectedParentVersion        = "1.0";
    private final        String      expectedCoreVersion          = "2.0";
    private final        String      expectedAppVersion           = "3.0";
    private final        String      expectedServerModulesVersion = "1.0";
    private final        String      expectedServerModuleAVersion = "3.0";
    private final        String      expectedServerModuleBVersion = "3.0";
    private final        String      expectedServerModuleCVersion = "3.0";

    @Rule
    public TestProject testProject = new TestProject(ProjectType.NESTED);

    @Test
    public void buildsAndInstallsAndTagsAllModules() throws Exception {
        buildsEachProjectTwiceTests(testProject.mvnReleaseComplete());
        hasInstalledAllModulesIntoTheRepoWithTheBuildNumber();

        assertBothReposTagged("nested-project", expectedAggregatorVersion, "");
        assertBothReposTagged("core-utils", expectedCoreVersion, "");
        assertBothReposTagged("console-app", expectedAppVersion, "");
        assertBothReposTagged("parent-module", expectedParentVersion, "");
        assertBothReposTagged("server-modules", expectedServerModulesVersion, "");
        assertBothReposTagged("server-module-a", expectedServerModuleAVersion, "");
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, ".misnamed");

        testProject.commitRandomFile("server-modules/server-module-b");
        testProject.mvnReleaseComplete();

        assertBothReposNotTagged("nested-project", minor(expectedAggregatorVersion, 1));
        assertBothReposNotTagged("core-utils", minor(expectedCoreVersion, 1));
        assertBothReposTagged("console-app", minor(expectedAppVersion, 1), "");
        assertBothReposNotTagged("parent-module", minor(expectedParentVersion, 1));
        assertBothReposNotTagged("server-modules", minor(expectedServerModulesVersion, 1));
        assertBothReposNotTagged("server-module-a", minor(expectedServerModuleAVersion, 1));
        assertBothReposTagged("server-module-b", expectedServerModuleBVersion, "");
        assertBothReposTagged("server-module-c", expectedServerModuleCVersion, ".misnamed");

        testProject.commitRandomFile("parent-module");
        testProject.mvnReleaseComplete();

        assertBothReposNotTagged("nested-project", minor(expectedAggregatorVersion, 1));
        assertBothReposTagged("core-utils", minor(expectedCoreVersion, 1), "");
        assertBothReposTagged("console-app", minor(expectedAppVersion, 2), "");
        assertBothReposTagged("parent-module", minor(expectedParentVersion, 1), "");
        assertBothReposNotTagged("server-modules", minor(expectedServerModulesVersion, 1));
        assertBothReposTagged("server-module-a", minor(expectedServerModuleAVersion, 1), "");
        assertBothReposTagged("server-module-b", minor(expectedServerModuleBVersion, 1), "");
        assertBothReposTagged("server-module-c", minor(expectedServerModuleCVersion, 1), ".misnamed");

        testProject.mvnReleaseComplete();
        assertBothReposTagged("nested-project", minor(expectedAggregatorVersion, 1), "");
        assertBothReposTagged("core-utils", minor(expectedCoreVersion, 2), "");
        assertBothReposTagged("console-app", minor(expectedAppVersion, 3), "");
        assertBothReposTagged("parent-module", minor(expectedParentVersion, 2), "");
        assertBothReposTagged("server-modules", minor(expectedServerModulesVersion, 1), "");
        assertBothReposTagged("server-module-a", minor(expectedServerModuleAVersion, 2), "");
        assertBothReposTagged("server-module-b", minor(expectedServerModuleBVersion, 2), "");
        assertBothReposTagged("server-module-c", minor(expectedServerModuleCVersion, 2), ".misnamed");
    }

    private String minor(String expectedCoreVersion, int newMinor) {
        return new VersionMatcher(expectedCoreVersion).fixVersion().withMinorVersion(newMinor).toString();
    }

    private void buildsEachProjectTwiceTests(List<String> commandOutput) throws Exception {
        assertThat(commandOutput, allOf(twoOf(containsString("Building nested-project " + expectedAggregatorVersion)),
                                        oneOf(containsString("Building core-utils")),
                                        oneOf(containsString("Building console-app")),
                                        oneOf(containsString("Building parent-module")),
                                        oneOf(containsString("Building server-modules")),
                                        oneOf(containsString("Building server-module-a")),
                                        oneOf(containsString("Building server-module-b")),
                                        oneOf(containsString("Building server-module-c"))));
    }

    private void hasInstalledAllModulesIntoTheRepoWithTheBuildNumber() throws Exception {
        assertArtifactInLocalRepo(GROUP_ID, "nested-project", expectedAggregatorVersion);
        assertArtifactInLocalRepo(GROUP_ID, "core-utils", expectedCoreVersion);
        assertArtifactInLocalRepo(GROUP_ID, "console-app", expectedAppVersion);
        assertArtifactInLocalRepo(GROUP_ID, "parent-module", expectedParentVersion);
        assertArtifactInLocalRepo(GROUP_ID, "server-modules", expectedServerModulesVersion);
        assertArtifactInLocalRepo(GROUP_ID, "server-module-a", expectedServerModuleAVersion);
        assertArtifactInLocalRepo(GROUP_ID, "server-module-b", expectedServerModuleBVersion);
        assertArtifactInLocalRepo(GROUP_ID + ".misnamed", "server-module-c", expectedServerModuleCVersion);
    }

    private void assertBothReposTagged(String module, String version, String groupSuffix) {
        assertThat(testProject.local, hasTagWithModuleVersion(GROUP_ID + groupSuffix, module, version));
        assertThat(testProject.origin, hasTagWithModuleVersion(GROUP_ID + groupSuffix, module, version));
    }

    private void assertBothReposNotTagged(String module, String version) {
        assertThat(testProject.local, not(hasTagWithModuleVersion(GROUP_ID, module, version)));
        assertThat(testProject.origin, not(hasTagWithModuleVersion(GROUP_ID, module, version)));
    }
}
