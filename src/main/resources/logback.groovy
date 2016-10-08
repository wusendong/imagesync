def log_home = System.getProperty("log.home");

appender("CONSOLE", ch.qos.logback.core.ConsoleAppender) 
{
	encoder(PatternLayoutEncoder) 
	{
    	pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %t %logger{36} - %msg%n"
  	}
}

def appenderList = ["CONSOLE"];
if (!org.apache.commons.lang3.StringUtils.isBlank(log_home))
{
	appender("FILE", ch.qos.logback.core.rolling.RollingFileAppender) 
	{1
		rollingPolicy(ch.qos.logback.core.rolling.TimeBasedRollingPolicy)
		{
			fileNamePattern = "${log_home}/%d{yyyy-MM-dd}.log";
			maxHistory = 30
		}
		encoder(PatternLayoutEncoder) 
		{
	    	pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %t %logger{36} - %msg%n"
	  	}
	}
		
	appenderList.add("FILE");
}

logger("com.rancher.imagesync", ALL, appenderList, false);
root(WARN, appenderList)