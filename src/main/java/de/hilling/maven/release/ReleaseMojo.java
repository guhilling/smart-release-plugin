package de.hilling.maven.release;

import static de.hilling.maven.release.Reactor.fromProjects;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
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

    private static List<String> updatePomsAndReturnChangedFiles(Log log, LocalGitRepo repo, Reactor reactor) throws
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
        return result.alteredPoms.stream().map(ReleaseFileUtils::canonicalName).collect(Collectors.toList());
    }

    @Override
    public void executeConcreteMojo(Scm scm, Scm originalScm, LocalGitRepo repo) throws MojoExecutionException,
                                                                                        MojoFailureException,
                                                                                        GitAPIException {
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

        saveModulesToBuild(reactor);

        saveFilesToRevert(repo, reactor);

        tagRepo(repo, currentRelease);
    }

    private void saveFilesToRevert(LocalGitRepo repo, Reactor reactor) throws MojoExecutionException {
        final List<String> changedFiles = updatePomsAndReturnChangedFiles(getLog(), repo, reactor);
        changedFiles.add(Constants.MODULE_BUILD_FILE);
        ReleaseFileUtils.write(Constants.FILES_TO_REVERT, changedFiles.stream().collect(joining("\n")));
    }

    private void saveModulesToBuild(Reactor reactor) {
        final String changedModules = reactor.getModulesInBuildOrder().stream().filter(ReleasableModule::isToBeReleased)
                                             .map(ReleasableModule::getRelativePathToModule)
                                             .collect(joining(","));
        ReleaseFileUtils.write(Constants.MODULE_BUILD_FILE, changedModules);
    }

    private void tagRepo(LocalGitRepo repo, ImmutableReleaseInfo releaseInfo) throws GitAPIException {
        final Optional<String> optionalTag = releaseInfo.getTagName();
        if (optionalTag.isPresent()) {
            final AnnotatedTag tag = new AnnotatedTag(optionalTag.get(), releaseInfo);

            getLog().info("About to tag repository with " + releaseInfo.toString());
            repo.tagRepo(tag);
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
