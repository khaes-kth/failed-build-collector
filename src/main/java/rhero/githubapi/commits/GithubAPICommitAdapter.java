package rhero.githubapi.commits;

import org.kohsuke.github.*;
import rhero.githubapi.GAA;
import rhero.githubapi.commits.models.FailedCommit;
import rhero.githubapi.commits.models.FailedCommitList;
import rhero.githubapi.repositories.GithubAPIRepoAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
                    if (check.getConclusion() == null || (!check.getConclusion().equals("success")
                            && !check.getConclusion().equals("neutral") && !check.getConclusion().equals("skipped"))) {
                        isTravisFailed = true;
                    }
                }
                if (check.getApp().getName().equals("GitHub Actions") && !isGithubActionsFailed) {
                    if (check.getConclusion() == null || (!check.getConclusion().equals("success")
                            && !check.getConclusion().equals("neutral") && !check.getConclusion().equals("skipped"))) {
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

    public List<FailedCommit> getFailedCommits(long intervalStart, long intervalEnd) throws IOException {
        Set<String> repos = GithubAPIRepoAdapter.getInstance()
                .listJavaRepositories(intervalStart, 1, GithubAPIRepoAdapter.MAX_STARS);

        int cnt = 0;
        List<FailedCommit> failedCommits = new ArrayList<>();
        for (String repoName : repos) {
            try {
                GHRepository repo = GAA.g().getRepository(repoName);
                boolean isMaven = false;
                for (GHTreeEntry treeEntry : repo.getTree("HEAD").getTree()) {
                    if (treeEntry.getPath().equals("pom.xml")) {
                        isMaven = true;
                        break;
                    }
                }

                if (!isMaven)
                    continue;

                System.out.println("Checking commits for: " + repo.getName() + " " + cnt++ + " " + repos.size()
                        + " " + new Date(intervalStart));
                failedCommits.addAll(GithubAPICommitAdapter.getInstance()
                        .getFailedCommits(repo, intervalStart, intervalEnd));

            } catch (Exception e) {
                System.err.println("error occurred for: " + repoName);
                e.printStackTrace();
            }
        }
        return failedCommits;
    }
}
