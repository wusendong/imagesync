package com.rancher.imagesync;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Hashtable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.PropertyResolver;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.rancher.imagesync.job.AnalyzeFileJob;
import com.rancher.imagesync.job.SyncJob;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * 该对象为索引守护线程启动器，调用 {@linkplain #start()} 或 {@linkplain #start(CommandLine)}，启动
 * 
 */
public class Launcher {
	static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

	private static final Launcher INSTANCE = new Launcher();

	private ConfigurableApplicationContext _springContext;

	private ThreadPoolExecutor _analyzExecutor;
	private ThreadPoolExecutor _syncExecutor;

	@Autowired
	private PropertyResolver _config;

	private File localRancherGitDir;
	private File localCommunityGitDir;

	private Hashtable<String, Sync> images;

	public Hashtable<String, Sync> getImages() {

		return images;
	}

	public void addImage(String image) {
		images.putIfAbsent(image, new Sync(image, false, 0));
		trySync(image);

	}

	public void trySync(String image) {
		images.compute(image, (k, v) -> {
			if (v == null)
				v = new Sync(image, false, 0);
			if (!v.isSuccess() && v.getTryCount() <= _config.getProperty(Constants.KEY_MAXTRY, Integer.class)) {
				if (v.getTryCount() > 0) {
					LOG.error("pull fail {},retry count {} ", image, v.getTryCount());
				}
				SyncJob syncJob = new SyncJob(image, _config.getProperty(Constants.KEY_REGISTRY_URL));
				_syncExecutor.execute(syncJob);
				v.setTryCount(v.getTryCount() + 1);
			}
			return v;
		});
	}

	private Launcher() {
	}

	private boolean isForce = true;

	public ThreadPoolExecutor getSyncExecutor() {
		return _syncExecutor;
	}

	public ConfigurableApplicationContext getSpringContext() {
		return _springContext;
	}

	private DockerClient docker;

	public DockerClient getDocker() {
		return docker;
	}

	public void init() {
		_springContext = new ClassPathXmlApplicationContext("META-INF/spring/applicationContext.xml");
		_springContext.getBeanFactory().autowireBean(this);
		localRancherGitDir = new File(SystemUtils.getUserHome(), _config.getProperty(Constants.KEY_FETCH_DIR_NAME) + "/rancher-catalog");
		localCommunityGitDir = new File(SystemUtils.getUserHome(), _config.getProperty(Constants.KEY_FETCH_DIR_NAME) + "/community-catalog");
		_analyzExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
		_syncExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
		images = new Hashtable<String, Sync>();
		try {
			docker = DefaultDockerClient.fromEnv().build();
		} catch (DockerCertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 命令行方式启动时调用该方法
	 */
	public void start() {
		int period = _config.getProperty(Constants.KEY_PERIOD, Integer.class);
		while (true) {
			try {
				analyzeCustomFile(new File(_config.getProperty(Constants.KEY_CUSTOMFILE)));
				if (loadGitHub(localRancherGitDir, _config.getProperty(Constants.KEY_RANCHER_CATALOG_URL)) || isForce)
					analyzeYamls(localRancherGitDir);
				if (loadGitHub(localCommunityGitDir, _config.getProperty(Constants.KEY_COMMUNITY_CATALOG_URL))
						|| isForce)
					analyzeYamls(localCommunityGitDir);
			} catch (Exception ex) {
				LOG.error("failed schedule file index job.", ex);
			}

			try {
				Thread.sleep(period);
			} catch (Exception ex) {
				// ignore
			}
		}
	}

	private void analyzeCustomFile(File file) {
		if (file.exists()) {
			try {
				Files.lines(file.toPath()).forEach(image -> {
					if(!Strings.isNullOrEmpty(image)){
						trySync(image);
					}
				});
			} catch (IOException e) {
				LOG.error(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void analyzeYamls(File paths)
			throws IOException, InterruptedException, DockerCertificateException, DockerException {
		Files.walkFileTree(Paths.get(paths.getPath()), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				File file = path.toFile();
				if ("docker-compose.yml".equalsIgnoreCase(file.getName())) {
					AnalyzeFileJob analyzeFileJob = new AnalyzeFileJob(file);
					_analyzExecutor.execute(analyzeFileJob);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private boolean loadGitHub(File localPath, String remoteUrl) {
		Git git;
		try {
			if (localPath.exists() && new File(localPath, ".git").exists()) {
				LOG.debug("local repository exists : " + localPath);
				Repository repository = new FileRepositoryBuilder().setGitDir(new File(localPath, ".git"))
						.readEnvironment().findGitDir().build();
				git = new Git(repository);
				LOG.debug("Starting pull");
				PullResult result = git.pull().call();
				LOG.debug("Messages: " + result.getMergeResult());
				return !Objects.equal(MergeStatus.ALREADY_UP_TO_DATE, result.getMergeResult().getMergeStatus());
			} else {
				LOG.debug("Cloning from " + remoteUrl + " to " + localPath);
				git = Git.cloneRepository().setURI(remoteUrl).setDirectory(localPath).call();
				LOG.debug("cloned repository: " + git.getRepository().getDirectory());
			}
		} catch (Exception e) {
			LOG.debug("git pull fail!!!!!!");
			e.printStackTrace();
		}
		return true;
	}

	// protected void scan() {
	// SqlRowSet rs = _jdbcKit.queryPendingIdxJob();
	// while (rs.next()) {
	// String fileId = null;
	//
	// try {
	// fileId = rs.getString("file_id");
	// String scopeId = rs.getString("scope_id");
	// String filename = rs.getString("file_name");
	// Long fileSize = rs.getLong("file_size");
	// Integer version = rs.getInt("version");
	// Boolean deleted = rs.getBoolean("is_deleted");
	// Boolean isOpen = rs.getBoolean("is_open");
	//
	// IdxJob idxJob = new IdxJob(fileId, scopeId, filename, version, fileSize,
	// deleted,isOpen);
	//
	// Launcher.get().getSpringContext().getBeanFactory().autowireBean(idxJob);
	//
	// _executor.submit(idxJob);
	// } catch (RejectedExecutionException ex) {
	// LOG.warn("index job overload.", ex);
	// } catch (Exception ex) {
	// LOG.error("failed execute index job for file '" +
	// StringUtils.defaultString(fileId) + "'.", ex);
	// }
	// }
	// }

	public static Launcher get() {
		return INSTANCE;
	}

	public static void main(String[] args) throws Exception {
		Launcher.get().init();
		Launcher.get().start();
	}
}
