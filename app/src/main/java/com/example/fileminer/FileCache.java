package com.example.fileminer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A singleton in-memory cache to store lists of MediaItem objects.
 * This speeds up loading times for categories that have already been scanned.
 */
public class FileCache {

    private static volatile FileCache instance;
    private final Map<String, List<MediaItem>> cache;

    // Private constructor to prevent instantiation
    private FileCache() {
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Returns the single instance of the FileCache.
     */
    public static FileCache getInstance() {
        if (instance == null) {
            synchronized (FileCache.class) {
                if (instance == null) {
                    instance = new FileCache();
                }
            }
        }
        return instance;
    }

    /**
     * Stores a list of media items in the cache for a specific file type.
     * @param fileType The key for the cache (e.g., "Photo", "Video").
     * @param items The list of MediaItem objects to store.
     */
    public void put(String fileType, List<MediaItem> items) {
        if (fileType != null && items != null) {
            cache.put(fileType, items);
        }
    }

    /**
     * Retrieves a list of media items from the cache.
     * @param fileType The key for the cache.
     * @return The cached list, or null if it's not in the cache.
     */
    public List<MediaItem> get(String fileType) {
        return cache.get(fileType);
    }

    /**
     * Clears the cache for a specific file type.
     * This is useful for forcing a refresh.
     * @param fileType The key to remove from the cache.
     */
    public void clear(String fileType) {
        if (fileType != null) {
            cache.remove(fileType);
        }
    }

    /**
     * Clears the entire cache.
     */
    public void clearAll() {
        cache.clear();
    }
}