package it.gov.pagopa.reward.service;

public interface LockService {
    int getBuketSize();
    void acquireLock(int lockId);
    void releaseLock(int lockId);
}
