package com.rancher.imagesync;

public class Sync{
	private String image;
	private boolean success;
	private int tryCount;
	
	public Sync( String image, Boolean success, Integer tryCount) {
		this.image = image;
		this.success = success;
		this.tryCount = tryCount;
	}
	
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public int getTryCount() {
		return tryCount;
	}

	public void setTryCount(int tryCount) {
		this.tryCount = tryCount;
	}
}