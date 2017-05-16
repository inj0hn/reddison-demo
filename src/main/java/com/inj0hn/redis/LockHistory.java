package com.inj0hn.redis;
import java.util.Date;

public class LockHistory {

	private String threadName;
	private Date attemptTimestamp;
	private Date lockTimestamp;
	private Date unlockTimestamp;
	
	public String getThreadName() {
		return threadName;
	}
	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}
	public Date getAttemptTimestamp() {
		return attemptTimestamp;
	}
	public void setAttemptTimestamp(Date attemptTimestamp) {
		this.attemptTimestamp = attemptTimestamp;
	}
	public Date getLockTimestamp() {
		return lockTimestamp;
	}
	public void setLockTimestamp(Date lockTimestamp) {
		this.lockTimestamp = lockTimestamp;
	}
	public Date getUnlockTimestamp() {
		return unlockTimestamp;
	}
	public void setUnlockTimestamp(Date unlockTimestamp) {
		this.unlockTimestamp = unlockTimestamp;
	}
	
}
