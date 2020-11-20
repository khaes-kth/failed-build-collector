package rhero.githubapi;

import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;

// GAM stands for: Github Api Adapter
public class GAA {
    private static final String TOKENS_PATH = "files" + File.separator + "tokens";
    private static int lastUsedToken = 0;

    public static GitHub g() throws IOException {
        return new GitHubBuilder().withOAuthToken(tokens[lastUsedToken++ % tokens.length]).build();
    }

    static {
        try {
            tokens = FileUtils.readLines(new File("files\\config.ini"),
                    "UTF-8").toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Tokens not found.");
        }
    }

    private static String[] tokens;
}
