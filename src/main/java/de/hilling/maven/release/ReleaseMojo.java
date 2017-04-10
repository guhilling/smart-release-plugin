package de.hilling.maven.release;

import static de.hilling.maven.release.Reactor.fromProjects;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.eclipse.jgit.api.errors.GitAPIException;

import de.hilling.maven.release.releaseinfo.ReleaseInfoStorage;
import de.hilling.maven.release.repository.LocalGitRepo;
import de.hilling.maven.release.versioning.ImmutableReleaseInfo;
import de.hilling.maven.release.versioning.ReleaseDateSingleton;
import de.hilling.maven.release.versioning.ReleaseInfo;

/**
 * Releases the project.
 */
@Mojo(name = "release", requiresDirectInvocation = true,
      // this should not be bound to a phase as this plugin starts a phase itself
      inheritByDefault = true, // so you can configure this in a shared parent pom
      requiresProject = true, // this can only run against a maven project
      aggregator = true // the plugin should only run once against the aggregator pom
      )
public class ReleaseMojo extends BaseMojo {

    /**
     * <p>
     * Profiles to activate during the release.
     * </p>
     * <p>
     * Note that if any profiles are activated during the build using the `-P` or `--activate-profiles` will also be activated during release.
     * This gives two options for running releases: either configure it in the plugin configuration, or activate profiles from the command line.
     * </p>
     *
     * @since 1.0.1
     */
    @Parameter(alias = "releaseProfiles")
    protected List<String> releaseProfiles = emptyList();
    /**
     * <p>
     * The goals to run against the project before the release. By default this is "test" which
     * means the project is built and the tests are run.
     * </p>
     * <p>
     * You can specify more goals and maven options. For example if you want to perform
     * a clean install, use:
     * </p>
     * <pre>
     * {@code
     * <releaseGoals>
     *     <releaseGoal>clean</releaseGoal>
     *     <releaseGoal>install</releaseGoal>
     * </releaseGoals>
     * }
     * </pre>
     * <p>
     * Remember that you will most probably do an implicit "compile package install deploy" during the release phase.
     * </p>
     */
    @Parameter(alias = "testGoals")
    protected List<String> testGoals = emptyList();
    /**
     * <p>
     * Profiles to activate during the the test run.
     * </p>
     * <p>
     * Note that if any profiles are activated during the build using the `-P` or `--activate-profiles` will also be
     * activated during test.
     * This gives two options for running test: either configure it in the plugin configuration, or activate profiles
     * from the command line.
     * </p>
     *
     * @since 1.0.1
     */
    @Parameter(alias = "testProfiles")
    protected List<String> testProfiles = emptyList();
    /**
     * Determines running of tests. Possible values:
     * {@code testPhaseOnly}, {@code runAlways}, {@code skipTests}
     */
    @Parameter(alias = "testBehaviour", defaultValue = "testPhaseOnly", property = "testBehaviour")
    protected TestBehaviour testBehaviour;
    /**
     * <p>
     * The goals to run against the project during a release. By default this is "deploy" which
     * means the release version of your artifact will be tested and deployed.
     * </p>
     * <p>
     * You can specify more goals and maven options. For example if you want to perform
     * a clean, build a maven site, and then deploy it, use:
     * </p>
     * <pre>
     * {@code
     * <releaseGoals>
     *     <releaseGoal>clean</releaseGoal>
     *     <releaseGoal>site</releaseGoal>
     *     <releaseGoal>deploy</releaseGoal>
     * </releaseGoals>
     * }
     * </pre>
     */
    @Parameter(alias = "releaseGoals")
    private List<String> releaseGoals = emptyList();
    /**
     * Push changes to remote repository. This includes:
     * <ul>
     * <li>The newly created tag for this release containing the release information</li>
     * <li>The release info file containing the same information. This will be used to find relevant older
     * releases to compare to during the following release.</li>
     * </ul>
     */
    @Parameter(alias = "push", defaultValue = "true", property = "push")
    private boolean push;

    private static List<File> updatePomsAndReturnChangedFiles(Log log, LocalGitRepo repo, Reactor reactor) throws
                                                                                                           MojoExecutionException,
                                                                                                           ValidationException {
        PomUpdater pomUpdater = new PomUpdater(log, reactor);
        PomUpdater.UpdateResult result = pomUpdater.updateVersion();
        if (!result.success()) {
            log.info("Going to revert changes because there was an error.");
            repo.revertChanges(log, result.alteredPoms);
            if (result.unexpectedException != null) {
                throw new ValidationException("Unexpected exception while setting the release versions in the pom",
                                              result.unexpectedException);
            } else {
                String summary = "Cannot release with references to snapshot dependencies";
                List<String> messages = new ArrayList<>();
                messages.add(summary);
                messages.add("The following dependency errors were found:");
                for (String dependencyError : result.dependencyErrors) {
                    messages.add(" * " + dependencyError);
                }
                throw new ValidationException(summary, messages);
            }
        }
        return result.alteredPoms;
    }

    @Override
    public void executeConcreteMojo(Scm scm, Scm originalScm, LocalGitRepo repo) throws MojoExecutionException,
                                                                                        MojoFailureException,
                                                                                        GitAPIException {
        setDefaults();
        repo.errorIfNotClean();

        final ReleaseInfoStorage infoStorage = new ReleaseInfoStorage(project.getBasedir(), repo.git);
        ReleaseInfo previousRelease = infoStorage.load();
        getLog().info("previous release: " + previousRelease);

        Reactor reactor = fromProjects(getLog(), repo, project, projects, modulesToForceRelease, noChangesAction,
                                       bugfixRelease, previousRelease);
        if (reactor == null) {
            return;
        }

        final List<ReleasableModule> releasableModules = reactor.getModulesInBuildOrder();

        final ImmutableReleaseInfo.Builder releaseBuilder = ImmutableReleaseInfo.builder().tagName(
            ReleaseDateSingleton.getInstance().tagName());

        List<String> modulesToRelease = new ArrayList<>();
        for (ReleasableModule releasableModule : releasableModules) {
            releaseBuilder.addModules(releasableModule.getImmutableModule());
            if (releasableModule.isToBeReleased()) {
                modulesToRelease.add(releasableModule.getRelativePathToModule());
            }
        }

        final ImmutableReleaseInfo currentRelease = releaseBuilder.build();
        getLog().info("current release: " + currentRelease);
        infoStorage.store(currentRelease);

        List<File> changedFiles = updatePomsAndReturnChangedFiles(getLog(), repo, reactor);

        if (testBehaviour != TestBehaviour.skipPreRelease) {
            new PhaseInvoker(getLog(), project, new DefaultInvocationRequest(), new DefaultInvoker(), testGoals,
                             testProfiles, !testBehaviour.isRunInTestPhase()).runMavenBuild(reactor);
        }

        tagAndPushRepo(repo, currentRelease);

        try {
            final PhaseInvoker invoker = new PhaseInvoker(getLog(), project, new DefaultInvocationRequest(),
                                                          new DefaultInvoker(), releaseGoals, releaseProfiles,
                                                          !testBehaviour.isRunInReleasePhase());
            invoker.runMavenBuild(reactor);
            revertChanges(repo, changedFiles, true); // throw if you can't revert as that is the root problem
        } finally {
            revertChanges(repo, changedFiles,
                          false); // warn if you can't revert but keep throwing the original exception so the root cause isn't lost
        }
    }

    private void setDefaults() {
        if (testGoals.isEmpty()) {
            testGoals = singletonList("test");
        }
        if (releaseGoals.isEmpty()) {
            releaseGoals = singletonList("deploy");
        }
    }

    private void tagAndPushRepo(LocalGitRepo repo, ImmutableReleaseInfo releaseInfo) throws GitAPIException {
        final Optional<String> optionalTag = releaseInfo.getTagName();
        if (optionalTag.isPresent()) {
            final AnnotatedTag tag = new AnnotatedTag(null, optionalTag.get(), releaseInfo);

            getLog().info("About to tag repository with " + releaseInfo.toString());
            repo.tagRepo(tag);
            if (push) {
                getLog().info("About to push tags " + tag.name());
                repo.pushAll();
            }
        } else {
            throw new ValidationException("internal error: no tag found on release info " + releaseInfo);
        }
    }

    private void revertChanges(LocalGitRepo repo, List<File> changedFiles, boolean throwIfError) throws
                                                                                                 MojoExecutionException {
        if (!repo.revertChanges(getLog(), changedFiles)) {
            String message = "Could not revert changes - working directory is no longer clean. Please revert changes manually";
            if (throwIfError) {
                throw new MojoExecutionException(message);
            } else {
                getLog().warn(message);
            }
        }
    }
}
