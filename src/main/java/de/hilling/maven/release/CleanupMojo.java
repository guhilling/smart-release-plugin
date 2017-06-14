package de.hilling.maven.release;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.errors.GitAPIException;

import de.hilling.maven.release.repository.LocalGitRepo;
import de.hilling.maven.release.utils.Constants;
import de.hilling.maven.release.utils.ReleaseFileUtils;

/**
 * Cleans up temporary release files from the project directory:
 * <ul>
 * <li>Revert changed poms.</li>
 * <li>Remove info files for modules to build and changed files.</li>
 * </ul>
 */
@Mojo(name = "cleanup", requiresDirectInvocation = true,
      // this should not be bound to a phase as this plugin starts a phase itself
      inheritByDefault = true, // so you can configure this in a shared parent pom
      requiresProject = true, // this can only run against a maven project
      aggregator = true // the plugin should only run once against the aggregator pom
      )
public class CleanupMojo extends BaseMojo {

    private static File fromString(String fileName) {
        return new File(fileName);
    }

    @Override
    public void executeConcreteMojo(Scm scm, Scm originalScm, LocalGitRepo repo) throws MojoExecutionException,
                                                                                        MojoFailureException,
                                                                                        GitAPIException {
        List<File> filesToRevert = ReleaseFileUtils.read(Constants.FILES_TO_REVERT).stream()
                                                   .map(CleanupMojo::fromString).collect(toList());
        if (!repo.revertChanges(getLog(), filesToRevert)) {
            String message = "Could not revert changes - working directory is no longer clean. Please revert changes manually";
            throw new MojoExecutionException(message);
        }
    }
}
