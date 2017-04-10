package de.hilling.maven.release;

import static de.hilling.maven.release.GitHelper.getRemoteUrlOrNullIfNoneSet;
import static de.hilling.maven.release.repository.LocalGitRepo.fromCurrentDir;
import static java.lang.String.format;

import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;

import de.hilling.maven.release.repository.LocalGitRepo;
import de.hilling.maven.release.utils.ErrorUtils;

/**
 * Base class for {@link NextMojo} and {@link ReleaseMojo}.
 */
public abstract class BaseMojo extends AbstractMojo {
    @Parameter(property = "project", required = true, readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    @Parameter(property = "projects", required = true, readonly = true, defaultValue = "${reactorProjects}")
    protected List<MavenProject> projects;

    /**
     * A module to force release on, even if no changes has been detected.
     */
    @Parameter(alias = "forceRelease", property = "forceRelease")
    protected List<String> modulesToForceRelease;

    /**
     * Determines the action to take when no module changes are detected. Possible values:
     * {@code ReleaseAll}, {@code ReleaseNone}, {@code FailBuild}
     */
    @Parameter(alias = "noChangesAction", defaultValue = "ReleaseAll", property = "noChangesAction")
    protected NoChangesAction noChangesAction;
    /**
     * Perform a bugfix release. When performing a bugfix release, the last ("bugfix") number of the release
     * version is incremented. The previous releases are determined from the .release-info.json-File.
     */
    @Parameter(defaultValue = "false", alias = "bugfixRelease", property = "bugfixRelease")
    protected boolean         bugfixRelease;
    @Parameter(property = "disableSshAgent")
    private   boolean         disableSshAgent;
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private   Settings        settings;
    /**
     * If set, the identityFile and passphrase will be read from the Maven
     * settings file.
     */
    @Parameter(property = "serverId")
    private   String          serverId;

    /**
     * If set, this file will be used to specify the known_hosts. This will
     * override any default value.
     */
    @Parameter(property = "knownHosts")
    private String knownHosts;

    /**
     * Specifies the private key to be used.
     */
    @Parameter(property = "privateKey")
    private String privateKey;

    /**
     * Specifies the passphrase to be used with the identityFile specified.
     */
    @Parameter(property = "passphrase")
    private String passphrase;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        try {
            configureJsch();
            final Scm originalScm = project.getOriginalModel().getScm();
            final Scm scm = project.getModel().getScm();
            final LocalGitRepo repo = fromCurrentDir(getRemoteUrlOrNullIfNoneSet(originalScm, scm), getLog());
            executeConcreteMojo(scm, originalScm, repo);
        } catch (ValidationException e) {
            ErrorUtils.printBigErrorMessageAndThrow(getLog(), e.getMessage(), e.getMessages());
        } catch (GitAPIException gae) {
            ErrorUtils.printBigGitErrorExceptionAndThrow(getLog(), gae);
        }
    }

    protected abstract void executeConcreteMojo(Scm scm, Scm originalScm, LocalGitRepo repo) throws MojoExecutionException, MojoFailureException,
                                                                                                    GitAPIException, ValidationException;

    final Settings getSettings() {
        return settings;
    }

    final void setSettings(final Settings settings) {
        this.settings = settings;
    }

    final void setServerId(final String serverId) {
        this.serverId = serverId;
    }

    final void setKnownHosts(final String knownHosts) {
        this.knownHosts = knownHosts;
    }

    final void setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
    }

    final void setPassphrase(final String passphrase) {
        this.passphrase = passphrase;
    }

    final void disableSshAgent() {
        disableSshAgent = true;
    }

    protected final void configureJsch() {
        if (!disableSshAgent) {
            if (serverId != null) {
                final Server server = settings.getServer(serverId);
                if (server != null) {
                    privateKey = privateKey == null
                                 ? server.getPrivateKey()
                                 : privateKey;
                    passphrase = passphrase == null
                                 ? server.getPassphrase()
                                 : passphrase;
                } else {
                    getLog().warn(format("No server configuration in Maven settings found with id %s", serverId));
                }
            }

            JschConfigSessionFactory
                .setInstance(new SshAgentSessionFactory(getLog(), knownHosts, privateKey, passphrase));
        }
    }
}
