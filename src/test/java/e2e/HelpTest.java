package e2e;

import scaffolding.MvnRunner;
import scaffolding.TestProject;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;

import de.hilling.maven.release.TestUtils;

public class HelpTest {

    @Rule
    public TestProject singleProject = new TestProject(ProjectType.SINGLE);

    private static Matcher<Iterable<? super String>> containsStrings(String... strings) {
        List<Matcher<Iterable<? super String>>> matchers = new ArrayList<>();
        for (String s : strings) {
            matchers.add(CoreMatchers.hasItem(containsString(s)));
        }

        return allOf((Iterable) matchers);
    }

    @Test
    public void runningTheHelpMojoTellsYouAboutThePlugin() throws IOException {
        final List<String> helpOutput = mvn(TestUtils.HELP_GOAL);
        assertThat(helpOutput, containsStrings("This plugin has 4 goals:", TestUtils.PREPARE_GOAL, TestUtils.NEXT_GOAL,
                                               TestUtils.HELP_GOAL, TestUtils.CLEANUP_GOAL));
    }

    private List<String> mvn(String... commands) throws IOException {
        return new MvnRunner().runMaven(singleProject.localDir, commands);
    }

    @Test
    public void canShowInformationAboutTheReleaseGoal() throws IOException {
        final List<String> helpOutput = mvn(TestUtils.HELP_GOAL, "-Dgoal=prepare", "-Ddetail=true");
        assertThat(helpOutput,
                   containsStrings("Perform a bugfix release. When performing a bugfix release"));
    }
}
