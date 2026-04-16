package com.financegame.service;

import org.springframework.stereotype.Service;

/**
 * Stub retained for API compatibility. Investment unlocking is now cert-based.
 * All logic has been moved to cert checks in RealEstateService, StockService, and CollectionService.
 */
@Service
public class InvestmentLevelService {
    // No-op — cert-based unlocking does not require level tracking
}
