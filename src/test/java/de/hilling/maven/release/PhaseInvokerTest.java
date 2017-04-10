package de.hilling.maven.release;

import static de.hilling.maven.release.PhaseInvoker.DEPLOY;
import static de.hilling.maven.release.PhaseInvoker.SKIP_TESTS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

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
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PhaseInvokerTest {
    private final static String                 ACTIVE_PROFILE_ID   = "activeProfile";
    private final static String                 SOME_PROFILE_ID     = "someProfile";
    private final static File                   GLOBAL_SETTINGS     = new File("file:///globalSettings");
    private final static File                   USER_SETTINGS       = new File("file:///globalSettings");
    private final static String                 MODULE_PATH         = "modulePath";
    private final static String                 SITE                = "site";
    private final        Log                    log                 = mock(Log.class);
    private final        MavenProject           project             = mock(MavenProject.class);
    private final        InvocationRequest      request             = mock(InvocationRequest.class);
    private final        InvocationResult       result              = mock(InvocationResult.class);
    private final        Invoker                invoker             = mock(Invoker.class);
    private final        List<String>           goals               = new LinkedList<>();
    private final        List<String>           modulesToRelease    = new LinkedList<>();
    private final        List<String>           releaseProfiles     = new LinkedList<>();
    private final        List<ReleasableModule> modulesInBuildOrder = new LinkedList<>();
    private final        Reactor                reactor             = mock(Reactor.class);
    private final        ReleasableModule       module              = mock(ReleasableModule.class);
    private final        Profile                activeProfile       = mock(Profile.class);
    private PhaseInvoker phaseInvoker;

    @Before
    public void setup() throws Exception {
        phaseInvoker = new PhaseInvoker(log, project, request, invoker);
        phaseInvoker.setGoals(singletonList("deploy"));
        phaseInvoker.setProfiles(emptyList());
        modulesInBuildOrder.add(module);
        when(log.isDebugEnabled()).thenReturn(true);
        when(invoker.execute(request)).thenReturn(result);
        when(activeProfile.getId()).thenReturn(ACTIVE_PROFILE_ID);
        when(module.getRelativePathToModule()).thenReturn(MODULE_PATH);
    }

    @Test
    public void verifyDefaultConstructor() {
        new PhaseInvoker(log, project, new DefaultInvocationRequest(), new DefaultInvoker());
    }

    @Test
    public void runMavenBuild_BaseTest() throws Exception {
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setInteractive(false);
        verify(request).setShowErrors(true);
        verify(request).setDebug(true);
        verify(log).isDebugEnabled();
        verify(request).setAlsoMake(true);
        verify(request).setGoals(Mockito.argThat(new BaseMatcher<List<String>>() {

            @Override
            public boolean matches(final Object item) {
                @SuppressWarnings("unchecked")                final List<String> goals = (List<String>) item;
                return goals.size() == 1 && goals.contains(DEPLOY);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("deploy");
            }
        }));
        verify(request).setProjects(Mockito.argThat(new BaseMatcher<List<String>>() {

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                return ((List<String>) item).isEmpty();
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("projects");
            }
        }));
        verify(log).info("About to run mvn [deploy] with no profiles activated");
    }

    @Test
    public void runMavenBuild_WithUserSettings() throws Exception {
        phaseInvoker.setUserSettings(USER_SETTINGS);
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setUserSettingsFile(USER_SETTINGS);
    }

    @Test
    public void runMavenBuild_WithGlobalSettings() throws Exception {
        phaseInvoker.setGlobalSettings(GLOBAL_SETTINGS);
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setGlobalSettingsFile(GLOBAL_SETTINGS);
    }

    @Test
    public void runMavenBuild_WithReleasableModule() throws Exception {
        // releaseProfiles.add(e)
    }

    @Test
    public void runMavenBuild_WithGoals() throws Exception {
        goals.add(SITE);
        phaseInvoker.setGoals(goals);
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setGoals(Mockito.argThat(new BaseMatcher<List<String>>() {

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final List<String> goals = (List<String>) item;
                return goals.size() == 1 && goals.contains(SITE);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("goals");
            }
        }));
    }

    @Test
    public void runMavenBuild_WithActiveProfiles() throws Exception {
        releaseProfiles.add(SOME_PROFILE_ID);
        phaseInvoker.setProfiles(releaseProfiles);
        when(project.getActiveProfiles()).thenReturn(asList(activeProfile));
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setProfiles(Mockito.argThat(new BaseMatcher<List<String>>() {

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final List<String> profiles = (List<String>) item;
                return profiles.size() == 2 && profiles.contains(ACTIVE_PROFILE_ID) && profiles
                                                                                           .contains(SOME_PROFILE_ID);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("profiles");
            }
        }));
    }

    @Test
    public void runMavenBuild_UserImplicitlyWantsThisToBeReleased() throws Exception {
        when(reactor.getModulesInBuildOrder()).thenReturn(modulesInBuildOrder);
        when(module.isToBeReleased()).thenReturn(true);
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setProjects(Mockito.argThat(new BaseMatcher<List<String>>() {

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final List<String> modules = (List<String>) item;
                return modules.size() == 1 && modules.contains(MODULE_PATH);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("projects");
            }
        }));
    }

    @Test
    public void runMavenBuild_UserImplicitlyWantsThisToBeReleased_WillNotBeReleased() throws Exception {
        when(reactor.getModulesInBuildOrder()).thenReturn(modulesInBuildOrder);
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setProjects(Mockito.argThat(new BaseMatcher<List<String>>() {

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                return ((List<String>) item).isEmpty();
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("projects");
            }
        }));
    }

    @Test
    public void skipTests() throws Exception {
        phaseInvoker.setSkipTests(true);
        phaseInvoker.runMavenBuild(reactor);
        verify(request).setGoals(Mockito.argThat(new BaseMatcher<List<String>>() {

            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(final Object item) {
                final List<String> goals = (List<String>) item;
                return goals.size() == 2 && goals.contains(DEPLOY) && goals.contains(SKIP_TESTS);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("goals");
            }
        }));
    }

    @Test(expected = MojoExecutionException.class)
    public void runMavenBuild_ErrorExitCode() throws Exception {
        when(result.getExitCode()).thenReturn(1);
        phaseInvoker.runMavenBuild(reactor);
    }

    @Test
    public void runMavenBuild_InvocationFailed() throws Exception {
        final MavenInvocationException expected = new MavenInvocationException("anyMessage");
        doThrow(expected).when(invoker).execute(request);
        try {
            phaseInvoker.runMavenBuild(reactor);
            fail("Exception expected here");
        } catch (final MojoExecutionException e) {
            assertSame(expected, e.getCause());
        }
    }
}
