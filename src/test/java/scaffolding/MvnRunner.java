package scaffolding;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;

public class MvnRunner {

    private static boolean haveInstalledPlugin = false;
    private final File mvnHome;
    public boolean logToStandardOut = false;

    public MvnRunner() {
        this(null);
    }

    public MvnRunner(File mvnHome) {
        this.mvnHome = mvnHome;
    }

    public static MvnRunner mvn(String version) {
        System.out.println("Ensuring maven " + version + " is available");
        MvnRunner mvnRunner = new MvnRunner();
        String dirWithMavens = "target/mavens/" + version;
        mvnRunner.runMaven(new File("."), "-Dartifact=org.apache.maven:apache-maven:" + version + ":zip:bin",
                           "-DmarkersDirectory=" + dirWithMavens, "-DoutputDirectory=" + dirWithMavens,
                           "org.apache.maven.plugins:maven-dependency-plugin:2.10:unpack");
        File mvnHome = new File(dirWithMavens).listFiles((FileFilter) DirectoryFileFilter.INSTANCE)[0];
        System.out.println("Maven " + version + " available at " + mvnHome.getAbsolutePath());
        return new MvnRunner(mvnHome);
    }

    public static void installReleasePluginToLocalRepo() throws MavenInvocationException {
        if (haveInstalledPlugin) {
            return;
        }
        long start = System.currentTimeMillis();
        System.out.print("Installing the plugin into the local repo .. ");
        assertThat("Environment variable M2_HOME must be set", systemMavenHome() != null);
        MvnRunner mvnRunner = new MvnRunner();
        mvnRunner.runMaven(new File("."), "-DskipTests=true -Pcoverage", "install");
        System.out.println(
            " installed the plugin into the local repo in " + (System.currentTimeMillis() - start) + "ms");
        haveInstalledPlugin = true;
    }

    public static String systemMavenHome() {
        return System.getenv("M2_HOME");
    }

    public static void assertArtifactInLocalRepo(String groupId, String artifactId, String version) throws IOException,
                                                                                                           MavenInvocationException {
        assertThat("Could not find artifact " + artifactId + " in repository",
                   artifactInLocalRepo(groupId, artifactId, version), is(0));
    }

    public static void assertArtifactNotInLocalRepo(String groupId, String artifactId, String version) throws
                                                                                                      IOException,
                                                                                                           MavenInvocationException {
        assertThat("Found artifact " + artifactId + " in repository",
                   artifactInLocalRepo(groupId, artifactId, version), not(is(0)));
    }

    private static int artifactInLocalRepo(String groupId, String artifactId, String version) throws IOException,
                                                                                                     MavenInvocationException {
        String artifact = groupId + ":" + artifactId + ":" + version + ":pom";
        File temp = new File("target/downloads/" + RandomNameGenerator.getInstance().randomName());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Collections.singletonList("org.apache.maven.plugins:maven-dependency-plugin:2.8:copy"));

        Properties props = new Properties();
        props.setProperty("artifact", artifact);
        props.setProperty("outputDirectory", temp.getCanonicalPath());

        request.setProperties(props);
        Invoker invoker = new DefaultInvoker();
        CollectingLogOutputStream logOutput = new CollectingLogOutputStream(false);
        invoker.setOutputHandler(new PrintStreamHandler(new PrintStream(logOutput), true));

        return invoker.execute(request).getExitCode();
    }

    public List<String> runMaven(File workingDir, String... arguments) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(asList(arguments));
        request.setBaseDirectory(workingDir);
        request.setDebug(false);
        request.setShowErrors(false);

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(mvnHome);

        CollectingLogOutputStream logOutput = new CollectingLogOutputStream(logToStandardOut);
        invoker.setOutputHandler(new PrintStreamHandler(new PrintStream(logOutput), true));
        //invoker.setErrorHandler(new PrintStreamHandler(new PrintStream(logOutput), true));

        int exitCode;
        try {
            InvocationResult result = invoker.execute(request);
            exitCode = result.getExitCode();
        } catch (Exception e) {
            throw new MavenExecutionException(1, logOutput.getLines());
        }
        List<String> output = logOutput.getLines();

        if (exitCode != 0) {
            throw new MavenExecutionException(exitCode, output);
        }

        return output;
    }
}
