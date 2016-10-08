package com.rancher.imagesync;

public class Constants
{
	
	
	/**
	 * total thread
	 */
	public static final String KEY_THREADCOUNT = "sync.threadCount";
	
	/**
	 * thread timeout (millisecond)
	 */
	public static final String KEY_THREADTIMEOUT = "sync.threadTimeout";
	
	/**
	 * sync period 
	 */
	public static final String KEY_PERIOD = "sync.period";
	
	/**
	 * max try
	 */
	public static final String KEY_MAXTRY = "sync.maxTry";
	/**
	 * github
	 */
	public static final String KEY_RANCHER_CATALOG_URL = "sync.git.url.rancher";
	/**
	 * github
	 */
	public static final String KEY_COMMUNITY_CATALOG_URL = "sync.git.url.community";
	/**
	 * git fetch directory
	 */
	public static final String KEY_FETCH_DIR_NAME = "sync.git.dir";
	/**
	 * file to analyze
	 */
	public static final String KEY_FILENAME = "sync.yaml.filename";
	/**
	 * private registry to push
	 */
	public static final String KEY_REGISTRY_URL = "sync.docker.url.private";

	public static final String KEY_CUSTOMFILE = "sync.customfile";
}
