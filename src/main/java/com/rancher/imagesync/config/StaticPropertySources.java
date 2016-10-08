package com.rancher.imagesync.config;

import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;


public class StaticPropertySources extends MutablePropertySources implements ApplicationContextAware
{
	private ApplicationContext _applicationContext;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		_applicationContext = applicationContext;
	}
	
	public StaticPropertySources() throws IOException
	{
		// 默认的配置信息
		addFirst(new ResourcePropertySource(CLASSPATH_URL_PREFIX + "config.properties"));
		
		// S3 配置
		addLast(new S3PropertySource());
	}
	
	class S3PropertySource extends PropertySource
	{
		private String _accessKey;
		private String _secretKey;
		private String _url;
		private String _bucket;
		
		public S3PropertySource()
		{
			super("S3");
		}

		@Override
		public Object getProperty(String name)
		{
//			if (_accessKey == null)
//			{
//				JdbcOperations jdbcOps = _applicationContext.getBean(JdbcOperations.class);
//				
//				SqlRowSet rs =  jdbcOps.queryForRowSet("select value from pan_env where name = ?", "S3");
//				if (!rs.next())
//				{
//					throw new RuntimeException("not s3 config found.");
//				}
//				
//				JSONObject s3Config = JSONObject.parseObject(rs.getString(1));
//				
//				_accessKey = s3Config.getString("accessKey");
//				_secretKey = s3Config.getString("secretKey");
//				_url = s3Config.getString("intranet");
//				_bucket = s3Config.getString("bucketName");
//				
//				Validate.notBlank(_accessKey);
//				Validate.notBlank(_secretKey);
//				Validate.notBlank(_url);
//				Validate.notBlank(_bucket);
//				
//				if (StringUtils.startsWithIgnoreCase(_url, "https"))
//				{
//					_url = "http" + StringUtils.removeStart(_url, "https");
//				}
//			}
//			
//			if (Objects.equals(name, Constants.S3_ACCESSKEY_KEY))
//			{
//				return _accessKey;
//			}
//			else if (Objects.equals(name, Constants.S3_SECRETKEY_KEY))
//			{
//				return _secretKey;
//			}
//			else if (Objects.equals(name, Constants.S3_URL_KEY))
//			{
//				return _url;
//			}
//			else if (Objects.equals(name, Constants.S3_BUCKET_KEY))
//			{
//				return _bucket;
//			}
			
			return null;
		}
	}
}
