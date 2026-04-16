package com.financegame.service;

import com.financegame.dto.ApplicationDto;
import com.financegame.dto.JobDto;
import com.financegame.dto.PlayerJobDto;
import com.financegame.entity.*;
import com.financegame.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final PlayerJobRepository playerJobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final EducationProgressRepository educationProgressRepository;
    private final CharacterRepository characterRepository;

    public JobService(
        JobRepository jobRepository,
        PlayerJobRepository playerJobRepository,
        JobApplicationRepository jobApplicationRepository,
        EducationProgressRepository educationProgressRepository,
        CharacterRepository characterRepository
    ) {
        this.jobRepository = jobRepository;
        this.playerJobRepository = playerJobRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.educationProgressRepository = educationProgressRepository;
        this.characterRepository = characterRepository;
    }

    @Transactional(readOnly = true)
    public List<JobDto> getAvailableJobs(Long playerId) {
        List<Job> jobs = jobRepository.findAllAvailable();

        EducationProgress ep = educationProgressRepository.findByPlayerId(playerId).orElse(null);
        List<String> completed = ep != null ? Arrays.asList(ep.getCompletedStages()) : List.of();

        Set<Long> appliedJobIds = jobApplicationRepository.findPendingByPlayerId(playerId)
            .stream().map(a -> a.getJob().getId()).collect(Collectors.toSet());

        Set<Long> activeJobIds = playerJobRepository.findActiveByPlayerId(playerId)
            .stream().map(pj -> pj.getJob().getId()).collect(Collectors.toSet());

        int totalExperience = playerJobRepository.findAllByPlayerId(playerId)
            .stream().mapToInt(PlayerJob::getMonthsWorked).sum();

        return jobs.stream().map(job -> {
            boolean meetsEdu = meetsEducationRequirementWithJson(completed, job);
            boolean meetsSide = meetsSideCertRequirement(completed, job);
            boolean meetsExp = totalExperience >= job.getRequiredMonthsExperience();
            String sideCertName = EducationService.sideCertLabel(job.getRequiredSideCert());
            return JobDto.from(job, meetsEdu && meetsSide && meetsExp,
                appliedJobIds.contains(job.getId()),
                activeJobIds.contains(job.getId()),
                sideCertName);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerJobDto> getActiveJobs(Long playerId) {
        return playerJobRepository.findActiveByPlayerId(playerId)
            .stream().map(PlayerJobDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> getApplications(Long playerId) {
        return jobApplicationRepository.findByPlayerIdOrderByIdDesc(playerId)
            .stream().map(ApplicationDto::from).toList();
    }

    @Transactional
    public ApplicationDto applyForJob(Long playerId, Long jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job nicht gefunden"));

        if (!job.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stelle ist nicht mehr verfuegbar");
        }
        if (playerJobRepository.existsActiveByPlayerAndJob(playerId, jobId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Du arbeitest bereits dort");
        }
        if (jobApplicationRepository.hasPendingForJob(playerId, jobId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bewerbung bereits eingereicht");
        }

        int currentTurn = characterRepository.findByPlayerId(playerId)
            .map(GameCharacter::getCurrentTurn)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Charakter nicht gefunden"));

        JobApplication application = new JobApplication(playerId, job, currentTurn);
        jobApplicationRepository.save(application);
        return ApplicationDto.from(application);
    }

    @Transactional
    public void quitJob(Long playerId, Long jobId) {
        PlayerJobId id = new PlayerJobId(playerId, jobId);
        PlayerJob pj = playerJobRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job nicht gefunden"));
        if (!pj.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job ist bereits inaktiv");
        }
        pj.setActive(false);
        playerJobRepository.save(pj);
    }

    // -------------------------------------------------------------------------

    private static boolean meetsSideCertRequirement(List<String> completed, Job job) {
        String cert = job.getRequiredSideCert();
        return cert == null || cert.isBlank() || completed.contains(cert);
    }

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private boolean meetsEducationRequirement(List<String> completed,
                                               String requiredType, String requiredField) {
        if (requiredType == null) return true;
        String key = requiredField != null ? requiredType + "_" + requiredField : requiredType;
        return completed.contains(key);
    }

    boolean meetsEducationRequirementWithJson(List<String> completed, Job job) {
        String json = job.getEducationRequirementsJson();
        if (json != null && !json.isBlank()) {
            try {
                List<Map<String, String>> reqs = JSON_MAPPER.readValue(json,
                    new TypeReference<List<Map<String, String>>>() {});
                // OR-logic: any single satisfied requirement passes
                return reqs.stream().anyMatch(req -> {
                    String type = req.get("type");
                    String field = req.get("field");
                    if (type == null) return true;
                    String key = field != null ? type + "_" + field : type;
                    return completed.contains(key);
                });
            } catch (Exception ignored) {
                // Fall through to legacy check
            }
        }
        return meetsEducationRequirement(completed,
            job.getRequiredEducationType(), job.getRequiredEducationField());
    }
}
