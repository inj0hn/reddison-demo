package com.inj0hn.redis;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedisTest {
	private static String phoneName = "object:1";
	private static String tv = "map:1";
	private RedissonClient client; 
	
	public RedisTest() throws IOException {
		client = Redisson
				.create(Config.fromJSON(new File(getClass().getClassLoader().getResource("config.txt").getFile())));
	}
	
	@Before
	public void clear() {
		client.getKeys().delete(phoneName, tv);
		assert client.getKeys().count() == 0;
	}
	
	@Test
	public void rBucket() throws Exception {
		RBucket<Phone> bucket = client.getBucket(phoneName);
		bucket.set(new Phone("1-877-3481-1861","Sony"));
		keys();

		RBucket<Phone> cachedPhone = client.getBucket(phoneName);
		System.out.println("Cached RBucket name: " + cachedPhone.getName());
		System.out.println("Cached RBucket object: " + cachedPhone.get().toString());
	}

	@Test
	public void rMap() throws Exception {
		RMap<String, Television> map = client.getMap(tv);
		map.put("foo", new Television("Sony"));
		keys();
		
		RMap<String, Television> cachedMap = client.getMap(tv);
		System.out.println("Cached RMap name: " + cachedMap.getName());
		System.out.println("Cached RMap entry: " + cachedMap.get("foo").toString());
	}
	
	@Test
	public void readLock() throws Exception {
		RBucket<Phone> bucket = client.getBucket(phoneName);
		bucket.set(new Phone("1-877-3481-1861","Sony"));
		System.out.println("Cached RBucket object: " + client.getBucket(phoneName).get().toString());
		
		List<Callable<String>> tasks = new ArrayList<>();
		for(int i=0 ; i<100 ; i++) {
			String threadName = "Thread:" + i;
			tasks.add(() -> {
				RReadWriteLock rw = client.getReadWriteLock(phoneName);
				System.out.println("Attempting to lock");
				rw.readLock().lock();
				Thread.sleep(1000);
				rw.readLock().unlock();
				System.out.println("Locked. " + threadName + ":" + client.getBucket(phoneName).get().toString());
				return threadName;
			});
		}
		
		ExecutorService service = Executors.newFixedThreadPool(10);
		service.invokeAll(tasks);
		service.awaitTermination(10, TimeUnit.SECONDS);
	}
	
	@Test
	public void readWriteLock() throws Exception {
		RBucket<Phone> bucket = client.getBucket(phoneName);
		bucket.set(new Phone("1-877-3481-1861","Sony"));
		System.out.println("Cached RBucket object: " + client.getBucket(phoneName).get().toString());
		
		List<LockHistory> histories = new ArrayList<>();
		String lockName = phoneName + ":lock";
		List<Callable<String>> tasks = new ArrayList<>();
		for(int i=0 ; i<20 ; i++) {
			String threadName = "Thread:" + i;
			int index = i;
			tasks.add(() -> {
				LockHistory history = new LockHistory();
				history.setThreadName(threadName);
				histories.add(history);
				try{
					SimpleDateFormat sdf = new SimpleDateFormat("K:mm:ss.SSS a");
					RLock lock;
					if(index == 1) {
						lock = client.getReadWriteLock(lockName).writeLock();
					} else {
						Thread.sleep(1000);
						lock = client.getReadWriteLock(lockName).readLock();
					}
					history.setAttemptTimestamp(new Date());
					System.out.println(threadName + " Attempting to lock '" + lock.getName() + "'. @" + sdf.format(history.getAttemptTimestamp()));
					
					lock.lock();
					history.setLockTimestamp(new Date());
					System.out.println(threadName + " locked '" + lock.getName() + "'. @" + sdf.format(history.getLockTimestamp()));
					if(index == 1) {
						System.out.println("Write lock sleeping...");
						Thread.sleep(11000);
						System.out.println("Write lock awaked. Releasing lock");
						lock.unlock();
					} else {
						Thread.sleep(5000);
						lock.unlock();
					}
					history.setUnlockTimestamp(new Date());
					System.out.println(threadName + " unlocked '" + lock.getName() + "'. @" + sdf.format(history.getUnlockTimestamp()));
				}catch (Exception e) {
					e.printStackTrace(System.out);
				}
				return threadName;
			});
		}
		
		ExecutorService service = Executors.newFixedThreadPool(10);
		service.invokeAll(tasks);
		service.shutdown();
		service.awaitTermination(10, TimeUnit.SECONDS);
		
		Collections.sort(histories, new Comparator<LockHistory>() {
			@Override
			public int compare(LockHistory o1, LockHistory o2) {
				return o1.getLockTimestamp().compareTo(o2.getLockTimestamp());
			}
		});
		
		SimpleDateFormat sdf = new SimpleDateFormat("K:mm:ss.SSS a");
		System.out.println("\n*********** HISTORY ***********");
		System.out.println("Thread Name\tAttempt Time\tLock Time\tUnlock Time");
		histories.forEach(history -> {
			System.out.println(history.getThreadName() + "\t" + sdf.format(history.getAttemptTimestamp()) + "\t"
					+ sdf.format(history.getLockTimestamp()) + "\t" + sdf.format(history.getUnlockTimestamp()));
		});
	}
	
	private void keys() {
		RKeys keys = client.getKeys();
		System.out.println("****************\nRKey: ");
		keys.getKeys().forEach(System.out::println);
		System.out.println("****************");
	}

	private static class Phone {
		String number;
		String model;
		
		@SuppressWarnings("unused")
		public Phone() {
			
		}
		
		public Phone(String number, String model) {
			this.number = number;
			this.model = model;
		}
		
		@Override
		public String toString() {
			return "Phone [number=" + number + ", model=" + model + "]";
		}
	}
	
	private static class Television {
		String model;
		
		@SuppressWarnings("unused")
		private Television() {
			
		}
		
		public Television(String model) {
			this.model = model;
		}
		
		@Override
		public String toString() {
			return "Television [model=" + model + "]";
		}

	}
}
