package com.lniosy.usedappliance.config;

import com.lniosy.usedappliance.service.EvaluationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SellerScoreJob {
    private final EvaluationService evaluationService;

    public SellerScoreJob(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Scheduled(cron = "${app.evaluation.recompute-cron:0 0 3 * * *}")
    public void recomputeScores() {
        evaluationService.recomputeAllSellerScores();
    }
}
