package de.hilling.maven.release;

import static java.lang.String.format;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * Invoker for maven run.
 */
class PhaseInvoker {
    static final String DEPLOY     = "deploy";
    static final String SKIP_TESTS = "-DskipTests=true";
    private final Log               log;
    private final MavenProject      project;
    private final InvocationRequest request;
    private final Invoker           invoker;
    private       boolean           skipTests;
    private       List<String>      goals;
    private       List<String>      profiles;

    public PhaseInvoker(final Log log, final MavenProject project, final InvocationRequest request,
                        final Invoker invoker) {
        this.log = log;
        this.project = project;
        this.request = request;
        this.invoker = invoker;
    }

    public PhaseInvoker(Log log, MavenProject project, DefaultInvocationRequest request,
                        DefaultInvoker invoker, List<String> goals, List<String> profiles,
                        boolean skipTests) {
        this.log = log;
        this.project = project;
        this.request = request;
        this.invoker = invoker;
        this.goals = goals;
        this.profiles = profiles;
        this.skipTests = skipTests;
    }

    private static List<?> from(List input) {
        return input;
    }

    final void setGoals(final List<String> goals) {
        this.goals = goals;
    }

    final void setProfiles(final List<String> profiles) {
        this.profiles = profiles;
    }

    final void setSkipTests(final boolean skipTests) {
        System.out.println("skip tests: " + skipTests);
        this.skipTests = skipTests;
    }

    final void setGlobalSettings(final File globalSettings) {
        request.setGlobalSettingsFile(globalSettings);
    }

    final void setUserSettings(final File userSettings) {
        request.setUserSettingsFile(userSettings);
    }

    public final void runMavenBuild(final Reactor reactor) throws MojoExecutionException {
        request.setInteractive(false);
        request.setShowErrors(true);
        request.setDebug(log.isDebugEnabled());

        if (skipTests) {
            goals = concat(goals.stream(), of(SKIP_TESTS)).collect(Collectors.toList());
        }

        request.setGoals(goals);

        final List<String> profiles = profilesToActivate();
        request.setProfiles(profiles);

        request.setAlsoMake(true);
        final List<String> changedModules = reactor.getModulesInBuildOrder().stream()
                                                   .filter(ReleasableModule::isToBeReleased)
                                                   .map(ReleasableModule::getRelativePathToModule)
                                                   .collect(Collectors.toList());
        request.setProjects(changedModules);

        final String profilesInfo = profiles.isEmpty()
                                    ? "no profiles activated"
                                    : "profiles " + profiles;

        log.info("building projects " + changedModules.stream().collect(Collectors.joining(",")));
        log.info(format("About to run mvn %s with %s", this.goals, profilesInfo));

        try {
            final InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new MojoExecutionException("Maven execution returned code " + result.getExitCode());
            }
        } catch (final MavenInvocationException e) {
            throw new MojoExecutionException("Failed to build artifact", e);
        }
    }

    private List<String> profilesToActivate() {
        return concat(profiles.stream(),
                      from(project.getActiveProfiles()).stream().map(Profile.class::cast).map(Profile::getId))
                   .collect(Collectors.toList());
    }
}
