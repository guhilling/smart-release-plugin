package de.hilling.maven.release;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import de.hilling.maven.release.versioning.GsonFactory;
import de.hilling.maven.release.versioning.ImmutableReleaseInfo;

public class GsonTest {

    @Test
    public void unmarshallJson() {
        final String json = "{\n" +
                                "  \"tagName\": \"MULTI_MODULE_RELEASE-2024-04-09-165458\",\n" +
                                "  \"modules\": [\n" +
                                "    {\n" +
                                "      \"releaseDate\": \"2024-04-09T18:54:58.305631+02:00[Europe/Berlin]\",\n" +
                                "      \"releaseTag\": \"MULTI_MODULE_RELEASE-2024-04-09-165458\",\n" +
                                "      \"artifact\": {\n" +
                                "        \"groupId\": \"de.hilling.maven.release.testprojects\",\n" +
                                "        \"artifactId\": \"single-module\"\n" +
                                "      },\n" +
                                "      \"version\": {\n" +
                                "        \"majorVersion\": 1,\n" +
                                "        \"minorVersion\": 0\n" +
                                "      }\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}";
        ImmutableReleaseInfo info = new GsonFactory().createGson().fromJson(json, ImmutableReleaseInfo.class);
        Assert.assertNotNull(info);
    }
}
