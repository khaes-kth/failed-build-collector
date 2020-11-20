package rhero.githubapi.commits;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitQueryBuilder;
import org.kohsuke.github.GHRepository;
import rhero.githubapi.commits.models.FailedCommit;
import rhero.githubapi.commits.models.FailedCommitList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GithubAPICommitAdapter {
    private static GithubAPICommitAdapter _instance;

    public static GithubAPICommitAdapter getInstance() {
        if (_instance == null)
            _instance = new GithubAPICommitAdapter();
        return _instance;
    }

    public List<FailedCommit> getFailedCommits(GHRepository repo, long since, long until) throws IOException {
        List<FailedCommit> res = new ArrayList<>();

        GHCommitQueryBuilder query = repo.queryCommits().since(since).until(until);
        for (GHCommit commit : query.list().toList()) {
            boolean isGithubActionsFailed = false, isTravisFailed = false;
            for (GHCheckRun check : commit.getCheckRuns()) {
                if (check.getApp().getName().equals("Travis CI") && !isTravisFailed) {
                    if (check.getConclusion() == null || !check.getConclusion().equals("success")) {
                        isTravisFailed = true;
                    }
                }
                if (check.getApp().getName().equals("GitHub Actions") && !isGithubActionsFailed) {
                    if (check.getConclusion() == null || !check.getConclusion().equals("success")) {
                        isGithubActionsFailed = true;
                    }
                }
                if (isGithubActionsFailed && isTravisFailed)
                    break;
            }

            if(isTravisFailed || isGithubActionsFailed){
                res.add(new FailedCommit(isTravisFailed, isGithubActionsFailed, commit.getSHA1(), repo.getFullName()));
            }
        }

        return res;
    }
}
