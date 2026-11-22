package com.cloudpulse.report;

/**
 * Report storage interface.
 * Implemented locally by default (in-memory streaming with zero cloud cost)
 * and prepared for future S3 integration on AWS.
 */
public interface ReportStorageService {

    String storeReport(String fileName, byte[] pdfData);

    byte[] retrieveReport(String storageKey);

    void deleteReport(String storageKey);
}
