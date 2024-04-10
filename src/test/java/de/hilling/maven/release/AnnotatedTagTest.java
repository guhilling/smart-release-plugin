package de.hilling.maven.release;

import e2e.ProjectType;
import scaffolding.GitMatchers;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.versioning.ModuleVersion;
import com.google.gson.JsonSyntaxException;

public class AnnotatedTagTest {
    @Rule
    public  TestProject  project = new TestProject(ProjectType.SINGLE);
    private AnnotatedTag tag;

    private static final long MAJOR_VERSION = 4L;
    private static final long MINOR_VERSION = 2134L;

    @Before
    public void setup() {
        tag = new AnnotatedTag("my-name", TestUtils.releaseInfo(MAJOR_VERSION, MINOR_VERSION, "test", "my-name"));
    }

    @Test
    public void gettersReturnValuesPassedIn() {
        assertModuleVersion(tag);
    }

    @Test
    public void aTagCanBeCreatedFromAGitTag() throws GitAPIException, IOException {
        tag.saveAtHEAD(project.local);
        Ref ref = project.local.tagList().call().get(0);
        AnnotatedTag inflatedTag = GitMatchers.fromRef(project.local.getRepository(), ref);
        assertModuleVersion(inflatedTag);
    }

    @Test(expected = JsonSyntaxException.class)
    public void ifATagIsSavedWithoutJsonThenAnExceptionIsThrown() throws GitAPIException, IOException {
        project.local.tag().setName("my-name-1.0.2").setAnnotated(true).setMessage("This is not json").call();
        Ref ref = project.local.tagList().call().get(0);
        GitMatchers.fromRef(project.local.getRepository(), ref);
    }

    private void assertModuleVersion(AnnotatedTag testedTag) {
        final ModuleVersion moduleVersion = testedTag.getReleaseInfo()
                                                     .versionForArtifact(TestUtils.artifactIdForModule("my-name"))
                                                     .orElseThrow(() -> new RuntimeException("artifact version missing"));
        assertThat(moduleVersion.getVersion().getMajorVersion(), equalTo(MAJOR_VERSION));
        assertThat(moduleVersion.getVersion().getMinorVersion(), equalTo(MINOR_VERSION));
    }
}
