package scaffolding;

import e2e.ProjectType;

import static de.hilling.maven.release.TestUtils.CLEANUP_GOAL;
import static de.hilling.maven.release.TestUtils.PREPARE_GOAL;
import static de.hilling.maven.release.utils.ReleaseFileUtils.pathOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static scaffolding.GitMatchers.hasChangesOnlyInReleaseInfo;
import static scaffolding.GitMatchers.hasCleanWorkingDirectory;
import static scaffolding.GitMatchers.isInSynchWithOrigin;
import static scaffolding.Photocopier.copyTestProjectToTemporaryLocation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.rules.ExternalResource;

import de.hilling.maven.release.TestUtils;
import de.hilling.maven.release.exceptions.ReleaseException;
import de.hilling.maven.release.utils.Constants;
import de.hilling.maven.release.utils.ReleaseFileUtils;

public class TestProject extends ExternalResource {

    private static final MvnRunner DEFAULT_RUNNER;
    private static final String PLUGIN_VERSION_FOR_TESTS = "5-SNAPSHOT";

    static {
        DEFAULT_RUNNER = new MvnRunner(null);
        MvnRunner.installReleasePluginToLocalRepo();
    }

    private final String artifactId;
    public        File   originDir;
    public        Git    origin;
    public        File   localDir;
    public        Git    local;
    public boolean checkClean     = false;
    public boolean checkNoChanges = false;
    ObjectId originHeadAtStart;
    ObjectId localHeadAtStart;
    private AtomicInteger commitCounter = new AtomicInteger(1);
    private ProjectType type;
    private MvnRunner   mvnRunner;
    private boolean purge = true;
    private RandomNameGenerator nameGenerator;

    public TestProject(ProjectType type) {
        this.type = type;
        artifactId = type.getSubmoduleName();
        mvnRunner = DEFAULT_RUNNER;
        nameGenerator = RandomNameGenerator.getInstance();
    }

    /**
     * Create initialized and usable project.
     *
     * @param type project test type
     *
     * @return fresh, usable project.
     *
     * @deprecated use with Role to prevent cleaning local repo in the middle of test.
     */
    @Deprecated
    public static TestProject project(ProjectType type) {
        final TestProject testProject = new TestProject(type);
        testProject.purge = false;
        testProject.before();
        return testProject;
    }

    public static void performPomSubstitution(File sourceDir) {
        File pom = new File(sourceDir, "pom.xml");
        if (pom.exists()) {
            try {
                String xml = FileUtils.readFileToString(pom, "UTF-8");
                if (xml.contains("${scm.url}")) {
                    xml = xml.replace("${scm.url}", dirToGitScmReference(sourceDir));
                }
                xml = xml.replace("${current.plugin.version}", PLUGIN_VERSION_FOR_TESTS);
                FileUtils.writeStringToFile(pom, xml, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException("unable to substitute poms");
            }
        }

        for (File child : sourceDir.listFiles((FileFilter) FileFilterUtils.directoryFileFilter())) {
            performPomSubstitution(child);
        }
    }

    public static String dirToGitScmReference(File sourceDir) {
        return "scm:git:file://localhost/" + pathOf(sourceDir).replace('\\', '/').toLowerCase();
    }

    public static ObjectId head(Git git) {
        try {
            return git.getRepository().getRefDatabase().findRef("HEAD").getObjectId();
        } catch (IOException e) {
            throw new RuntimeException("failed to get objectid of head");
        }
    }

    @Override
    protected void before() {
        final String submoduleName = type.getSubmoduleName();
        final String subfolderName = nameGenerator.randomName();
        final String subfolderOriginName = subfolderName + "/origin";
        final String subfolderWorkingName = subfolderName + "/work";
        originDir = copyTestProjectToTemporaryLocation(submoduleName, subfolderOriginName);
        performPomSubstitution(originDir);

        InitCommand initCommand = Git.init();
        initCommand.setDirectory(originDir);
        try {
            origin = initCommand.call();

            origin.add().addFilepattern(".").call();
            origin.commit().setMessage("Initial commit").call();

            localDir = Photocopier.folderForSampleProject(submoduleName, subfolderWorkingName);
            local = Git.cloneRepository().setBare(false).setDirectory(localDir).setURI(originDir.toURI().toString())
                       .call();
            originHeadAtStart = head(origin);
            localHeadAtStart = head(local);
            assertThat(originHeadAtStart, equalTo(localHeadAtStart));
        } catch (GitAPIException e) {
            throw new RuntimeException("error accessing/creating git repo", e);
        }
        if (purge) {
            mvnRun("dependency:purge-local-repository",
                   "-DmanualInclude=de.hilling.maven.release.testprojects:,de.hilling.maven.release.testproject",
                   "-DactTransitively=false");
        }
    }

    @Override
    protected void after() {
        if (checkClean) {
            verifyWorkingDirectoryCleanAndUnchanged();
        }
        if (checkNoChanges) {
            assertThat(this, hasChangesOnlyInReleaseInfo());
        }
    }

    private void verifyWorkingDirectoryCleanAndUnchanged() {
        try {
            origin.reset().setMode(ResetCommand.ResetType.HARD).call();
        } catch (GitAPIException e) {
            throw new RuntimeException("resetting origin failed", e);
        }
        assertThat(local, hasCleanWorkingDirectory());
        assertThat(local, isInSynchWithOrigin());
    }

    /**
     * Runs a mvn command against the local repo and returns the console output.
     */
    public List<String> mvn(String... arguments) {
        return mvnRunner.runMaven(localDir, arguments);
    }

    public List<String> mvnReleaseComplete(String... arguments) {
        List<String> result = new ArrayList<>();

        result.addAll(mvnReleasePrepare(arguments));
        result.addAll(mvnInstall());
        result.addAll(mvnCleanup(arguments));
        push();
        pushTags();
        return result;
    }

    public List<String> mvnInstall() {
        final String modulesFile = ReleaseFileUtils.canonicalName(localDir) + "/" + Constants.MODULE_BUILD_FILE;
        String projectList = ReleaseFileUtils.read(modulesFile).stream().collect(Collectors.joining(","));
        return mvnRun("install", "-pl", projectList);
    }

    public List<String> mvnReleasePrepare(String... arguments) {
        return mvnRun(PREPARE_GOAL, arguments);
    }

    public List<String> mvnCleanup(String... arguments) {
        return mvnRun(CLEANUP_GOAL, arguments);
    }

    public List<String> mvnReleaseBugfixComplete() {
        return mvnReleaseComplete("-DbugfixRelease=true");
    }

    public List<String> mvnReleaserNext(String... arguments) {
        return mvnRun(TestUtils.NEXT_GOAL, arguments);
    }

    public String readFile(String module, String fileName) throws IOException {
        File file = getFile(module, fileName);
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    public TestProject commitFile(String module, String fileName, String fileContent) throws IOException,
                                                                                             GitAPIException {
        File file = getFile(module, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileUtils.write(file, fileContent, StandardCharsets.UTF_8);
        String modulePath = module.equals(".")
                            ? ""
                            : module + "/";
        local.add().addFilepattern(modulePath + file.getName()).call();
        local.commit().setMessage("Commit " + commitCounter.getAndIncrement() + ": adding " + file.getName()).call();
        return this;
    }

    public File getFile(String module, String fileName) throws IOException {
        checkNoChanges = false;
        File moduleDir = new File(localDir, module);
        if (!moduleDir.isDirectory()) {
            throw new RuntimeException("Could not find " + moduleDir.getCanonicalPath());
        }
        return new File(moduleDir, fileName);
    }

    public TestProject commitRandomFile(String module) throws IOException, GitAPIException {
        File random = getFile(module, nameGenerator.randomName() + ".txt");
        if (!random.createNewFile()) {
            throw new RuntimeException("file alredy exists: " + random.getCanonicalPath());
        }
        String modulePath = module.equals(".")
                            ? ""
                            : module + "/";
        local.add().addFilepattern(modulePath + random.getName()).call();
        local.commit().setMessage("Commit " + commitCounter.getAndIncrement() + ": adding " + random.getName()).call();
        return this;
    }

    public void push() {
        try {
            local.push().call();
        } catch (GitAPIException e) {
            throw new ReleaseException(e);
        }
    }

    public void pushTags() {
        try {
            local.push().setPushTags().call();
        } catch (GitAPIException e) {
            throw new ReleaseException(e);
        }
    }

    private List<String> mvnRun(String goal, String... arguments) {
        String[] args = new String[arguments.length + 1];
        System.arraycopy(arguments, 0, args, 1, arguments.length);
        args[0] = goal;
        return mvnRunner.runMaven(localDir, args);
    }

    public void setMvnRunner(MvnRunner mvnRunner) {
        this.mvnRunner = mvnRunner;
    }

    public String getArtifactId() {
        return artifactId;
    }
}
