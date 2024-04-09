package de.hilling.maven.release;

import e2e.ProjectType;
import scaffolding.TestProject;

import static de.hilling.maven.release.TestUtils.saveFileInModule;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;

public class DiffDetectorTest {

    @Rule
    public TestProject singleProject = new TestProject(ProjectType.SINGLE);
    @Rule
    public TestProject independentVersions = new TestProject(ProjectType.INDEPENDENT_VERSIONS);
    @Rule
    public TestProject nestedProject = new TestProject(ProjectType.NESTED);

    private Log                                         log;
    private Map<AnnotatedTag, org.eclipse.jgit.lib.Ref> refMap;

    @Before
    public void setUp() {
        refMap = new HashMap<>();
        singleProject.checkClean = false;
        independentVersions.checkClean = false;
        nestedProject.checkClean = false;
        log = new SystemStreamLog();
    }

    @Test
    public void canDetectIfFilesHaveBeenChangedForAModuleSinceSomeSpecificTag() throws Exception {

        AnnotatedTag tag1 = saveFileInModule(independentVersions, "console-app", "1.2.3", refMap);
        AnnotatedTag tag2 = saveFileInModule(independentVersions, "core-utils", "2.0", refMap);
        AnnotatedTag tag3 = saveFileInModule(independentVersions, "console-app", "1.2.4", refMap);

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(independentVersions.local.getRepository(), log);

        assertThat(detector.hasChangedSince("core-utils", Collections.emptyList(), refMap.get(tag2)), is(false));
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), refMap.get(tag2)), is(true));
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), refMap.get(tag3)), is(false));
    }

    @Test
    public void canDetectThingsInTheRoot() throws IOException, GitAPIException {
        AnnotatedTag tag1 = saveFileInModule(singleProject, ".", "1.0.1", refMap);
        singleProject.commitRandomFile(".");
        AnnotatedTag tag2 = saveFileInModule(singleProject, ".", "1.0.2", refMap);

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(singleProject.local.getRepository(), log);

        assertThat(detector.hasChangedSince(".", Collections.emptyList(), refMap.get(tag1)), is(true));
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), refMap.get(tag2)), is(false));
    }

    @Test
    public void ignoreReleaseInfoInTheRoot() throws IOException, GitAPIException {
        AnnotatedTag tag1 = saveFileInModule(singleProject, ".", "1.0.1", refMap);
        singleProject.commitFile(".", ReleaseInfoStorage.RELEASE_INFO_FILE, "any-content");
        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(singleProject.local.getRepository(), log);
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), refMap.get(tag1)), is(false));

        AnnotatedTag tag2 = saveFileInModule(singleProject, ".", "1.0.2", refMap);
        assertThat(detector.hasChangedSince(".", Collections.emptyList(), refMap.get(tag2)), is(false));
    }

    @Test
    public void canDetectChangesAfterTheLastTag() throws IOException, GitAPIException {
        saveFileInModule(independentVersions, "console-app", "1.2.3", refMap);
        saveFileInModule(independentVersions, "core-utils", "2.0", refMap);
        AnnotatedTag tag3 = saveFileInModule(independentVersions, "console-app", "1.2.4", refMap);
        independentVersions.commitRandomFile("console-app");

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(independentVersions.local.getRepository(), log);
        assertThat(detector.hasChangedSince("console-app", Collections.emptyList(), refMap.get(tag3)), is(true));
    }

    @Test
    public void canIgnoreChangesInModuleFolders() throws IOException, GitAPIException {
        AnnotatedTag tag1 = saveFileInModule(nestedProject, "server-modules", "1.2.4", refMap);
        nestedProject.commitRandomFile("server-modules/server-module-a");

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(nestedProject.local.getRepository(), log);
        assertThat(detector.hasChangedSince("server-modules", asList("server-module-a", "server-module-b"),
                                            refMap.get(tag1)), is(false));
    }

    @Test
    public void canDetectLocalChangesWithModuleFolders() throws IOException, GitAPIException {
        AnnotatedTag tag1 = saveFileInModule(nestedProject, "server-modules", "1.2.4", refMap);
        nestedProject.commitRandomFile("server-modules");

        TreeWalkingDiffDetector detector = new TreeWalkingDiffDetector(nestedProject.local.getRepository(), log);
        assertThat(detector.hasChangedSince("server-modules", asList("server-module-a", "server-module-b"),
                                            refMap.get(tag1)), is(true));
    }

}
