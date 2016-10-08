package com.rancher.imagesync.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.rancher.imagesync.Launcher;

public class AnalyzeFileJob implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(AnalyzeFileJob.class);

	private File file;

	public AnalyzeFileJob(File file) {
		this.file = file;
	}

	@Override
	public void run() {
		try {
			analyzeYaml(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void analyzeYaml(File file) throws FileNotFoundException {
		Yaml yaml = new Yaml();
		LOG.debug("analyzing {}",file);
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object> load = (LinkedHashMap<String, Object>) yaml.load(new FileInputStream(file));
		searchImage(load);
		LOG.debug("completed {}",file);
	}

	@SuppressWarnings("unchecked")
	private void searchImage(Map<String, Object> map) {
		map.forEach((k, v) -> {
			if (v instanceof Map)
				searchImage((Map<String, Object>) v);
			if ("image".equals(k)&&null!=v) {
				Launcher.get().addImage(v.toString());
			}
		});
	}
}
