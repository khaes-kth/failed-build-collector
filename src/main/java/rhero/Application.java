package rhero;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rhero.githubapi.GAA;
import rhero.githubapi.commits.GithubAPICommitAdapter;
import rhero.githubapi.commits.models.FailedCommit;
import rhero.githubapi.commits.models.FailedCommitList;
import rhero.githubapi.repositories.GithubAPIRepoAdapter;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {
    public static String outputDir = null;
    private static final int DELAY_HOURS = 5;
    private static final int INTERVAL_HOURS = 1;
    private static Long getLastStartTime(String outputDir) throws ParseException {
        long curTime = System.currentTimeMillis();

        File outDir = new File(outputDir);
        if(!outDir.exists() || outDir.listFiles().length == 0){
            outDir.mkdirs();

            return ((curTime - 3600L * 1000 * (DELAY_HOURS + INTERVAL_HOURS)) / 3600000L) * 3600000L;
        }

        long lastTime = -1;
        for(File file : outDir.listFiles()){
            String timeStr = file.getName().substring(0, file.getName().length() - 5);

            Date date = new SimpleDateFormat("yyyy-MM-dd-HH").parse(timeStr);
            long time = date.getTime();

            lastTime = Math.max(lastTime, time);
        }

        return lastTime;
    }

    public static void main(String[] args) throws IOException, ParseException {
        SpringApplication.run(Application.class, args);

        outputDir = args[0];

        while(true) {
            long intervalStart = getLastStartTime(outputDir) + 3600L * 1000 * INTERVAL_HOURS;
            long currentTime = System.currentTimeMillis();

            System.out.println("IntervalStart: " + new Date(intervalStart));

            if(currentTime - 3600L * 1000 * DELAY_HOURS < intervalStart){
                try {
                    System.out.println("Sleeping at: " + new Date(currentTime));
                    Thread.sleep(60 * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            long intervalEnd = intervalStart + 3600L * 1000 * INTERVAL_HOURS;

            System.out.println("Checking projects updated: " + new Date(intervalStart) + " " + new Date(intervalEnd));

            List<FailedCommit> failedCommits = GithubAPICommitAdapter.getInstance()
                    .getFailedCommits(intervalStart, intervalEnd);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH");
            String intervalStartFileName = format.format(new Date(intervalStart));
            FileWriter fw = new FileWriter(outputDir + File.separator + intervalStartFileName + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(new FailedCommitList(failedCommits), fw);
            fw.close();
        }
    }

}

@RestController
class Controller {

    @RequestMapping("/failed-commits/{datetime}")
    public ResponseEntity<FailedCommitList> failedCommits(@PathVariable String datetime) throws IOException {
        File targetFile = new File(Application.outputDir + File.separator + datetime + ".json");

        if(!targetFile.exists()){
            return ResponseEntity.notFound().build();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        FailedCommitList res = gson.fromJson(new FileReader(targetFile),
                FailedCommitList.class);

        return ResponseEntity.ok(res);
    }
}
