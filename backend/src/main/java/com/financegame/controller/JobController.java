package com.financegame.controller;

import com.financegame.dto.ApplicationDto;
import com.financegame.dto.JobDto;
import com.financegame.dto.PlayerJobDto;
import com.financegame.security.PlayerPrincipal;
import com.financegame.service.JobService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /** All available jobs, annotated with player-specific flags */
    @GetMapping
    public List<JobDto> getAvailableJobs(@AuthenticationPrincipal PlayerPrincipal principal) {
        return jobService.getAvailableJobs(principal.id());
    }

    /** Player's active (currently held) jobs */
    @GetMapping("/my")
    public List<PlayerJobDto> getMyJobs(@AuthenticationPrincipal PlayerPrincipal principal) {
        return jobService.getActiveJobs(principal.id());
    }

    /** All job applications (pending + resolved) */
    @GetMapping("/applications")
    public List<ApplicationDto> getApplications(@AuthenticationPrincipal PlayerPrincipal principal) {
        return jobService.getApplications(principal.id());
    }

    /** Submit a new application */
    @PostMapping("/{jobId}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto apply(
        @PathVariable Long jobId,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        return jobService.applyForJob(principal.id(), jobId);
    }

    /** Quit an active job immediately */
    @DeleteMapping("/{jobId}/quit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quit(
        @PathVariable Long jobId,
        @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        jobService.quitJob(principal.id(), jobId);
    }
}
