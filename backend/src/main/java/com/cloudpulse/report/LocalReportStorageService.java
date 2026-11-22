package com.cloudpulse.report;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalReportStorageService implements ReportStorageService {

    private final Map<String, byte[]> localCache = new ConcurrentHashMap<>();

    @Override
    public String storeReport(String fileName, byte[] pdfData) {
        String key = "local://" + System.currentTimeMillis() + "-" + fileName;
        localCache.put(key, pdfData);
        return key;
    }

    @Override
    public byte[] retrieveReport(String storageKey) {
        return localCache.get(storageKey);
    }

    @Override
    public void deleteReport(String storageKey) {
        localCache.remove(storageKey);
    }
}
