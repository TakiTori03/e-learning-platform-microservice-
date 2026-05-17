package com.hust.commonlibrary.constant;

/**
 * Centralized definitions for all Redis Key Prefixes and Namespace Schemas across the platform.
 */
public final class RedisPrefixConstants {

    // Prevents instantiation
    private RedisPrefixConstants() {}

    // ==================================================================================
    // GLOBAL SHARED NAMESPACES
    // Must explicitly start with AppConstants.Redis_Constants.APP_PREFIX ("elearning:")
    // to bypass automatic Application Name isolation in RedisServiceImpl!
    // ==================================================================================
    
    /**
     * Namespace base for global shared course-ownership authorization.
     */
    public static final String SHARED_AUTH_COURSE_BASE = AppConstants.Redis_Constants.APP_PREFIX + "shared:auth:course:";

    // ==================================================================================
    // LOCAL SERVICE NAMESPACES
    // Prepend local operations. Automatically scoped to applicationName by RedisServiceImpl.
    // ==================================================================================
    
    /**
     * Identity Service: Blacklisted token keyspace.
     */
    public static final String IDENTITY_BLACKLIST = "blacklist:";

    /**
     * Identity Service: Spring Cache profile keyspace.
     */
    public static final String CACHE_USER_PROFILE = "profile::";

    // ==================================================================================
    // UTILITY KEY BUILDERS
    // Safe helper methods to prevent manual string-concatenation errors.
    // ==================================================================================

    /**
     * Safely constructs the global permanent course ownership key:
     * Format: elearning:shared:auth:course:{courseId}:owner
     */
    public static String getSharedCourseOwnerKey(String courseId) {
        return SHARED_AUTH_COURSE_BASE + courseId + ":owner";
    }

    /**
     * Safely constructs blacklisted token key:
     * Format: blacklist:{token}
     */
    public static String getTokenBlacklistKey(String token) {
        return IDENTITY_BLACKLIST + token;
    }
}
