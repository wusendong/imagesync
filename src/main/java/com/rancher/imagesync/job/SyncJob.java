package com.rancher.imagesync.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rancher.imagesync.Launcher;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.ConflictException;
import com.spotify.docker.client.exceptions.DockerException;

public class SyncJob implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(SyncJob.class);

	private String image;

	private String registryUrl;

	public SyncJob(String image, String registryUrl) {
		this.image = image;
		this.registryUrl = registryUrl;
	}

	@Override
	public void run() {
		try {
			pullImages();
		} catch (Exception e) {
			LOG.error(e.getMessage());
			Thread.currentThread().interrupt();
			Launcher.get().trySync(image);
		}
	}

	private void pullImages() throws DockerException, InterruptedException {
		final DockerClient docker = Launcher.get().getDocker();
		LOG.debug("{} start pulling ", image);
		docker.pull(image);
		LOG.debug("{} pulled", image);
		String tagImage = registryUrl + "/" + image;
		try {
			LOG.debug("{} tag to {}", image, tagImage);
			docker.tag(image, tagImage);
			LOG.debug("{} taged", tagImage);
			LOG.debug("{} pushing ", tagImage);
			docker.push(tagImage);
			LOG.debug("{} pushed", tagImage);
			Launcher.get().getImages().computeIfPresent(image, (k, v) -> {
				v.setSuccess(true);
				return v;
			});
		} catch (ConflictException e) {
			LOG.warn("eixsts " + tagImage);
		}
	}
}
