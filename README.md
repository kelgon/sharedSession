# 用spring-session和Redis实现会话共享

## 源码

https://github.com/kelgon/sharedSession

## 依赖库

spring-session，重写并覆盖Session对象，实现会话共享
```xml
<dependency>
  <groupId>org.springframework.session</groupId>
  <artifactId>spring-session</artifactId>
  <version>1.3.0.RELEASE</version>
  <exclusions>
    <exclusion>
      <groupId>commons-logging</groupId>
      <artifactId>commons-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

spring-data-redis，将spring与redis集成
```xml
<dependency>
  <groupId>org.springframework.data</groupId>
  <artifactId>spring-data-redis</artifactId>
  <version>1.8.1.RELEASE</version>
  <exclusions>
    <exclusion>
      <groupId>commons-logging</groupId>
      <artifactId>commons-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

spring-web，非必须。本例中只是为了偷懒用了其中的ContextLoaderListener在webapp启动时自动加载spring配置。也可以自己写Listener来实现。
```xml
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-web</artifactId>
  <version>4.3.7.RELEASE</version>
  <exclusions>
    <exclusion>
      <groupId>commons-logging</groupId>
      <artifactId>commons-logging</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

**spring相关的库都会自动依赖commons-logging库(JCL)，但本例中使用slf4j作为日志api，所以需要将spring对JCL的依赖exlude掉**

jedis客户端
```xml
<dependency>
  <groupId>redis.clients</groupId>
  <artifactId>jedis</artifactId>
  <version>2.9.0</version>
</dependency>
```

slf4j和JCL到slf4j的桥接库
```xml
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>1.7.24</version>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>jcl-over-slf4j</artifactId>
  <version>1.7.24</version>
</dependency>
```

log4j2和slf4j到log4j2的适配库
```xml
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-core</artifactId>
  <version>2.8.1</version>
</dependency>
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-web</artifactId>
  <version>2.8</version>
</dependency>
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-slf4j-impl</artifactId>
  <version>2.8.1</version>
</dependency>
```

fastjson，用来做demo的，非必须
```xml
<dependency>
  <groupId>com.alibaba</groupId>
  <artifactId>fastjson</artifactId>
  <version>1.2.24</version>
</dependency>
```

## spring-session相关配置

WEB-INF/spring-session.xml：
```xml
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd">

	<!-- spring-session内部是通过注解来实现相关机制的，所以需要启用对spring bean的内部注解的识别 -->
    <context:annotation-config/>

    <!--
    	spring session的相关配置
    	maxInactiveIntervalInSeconds：会话有效期
        redisNamespace：存储于Redis中的会话信息的key的前缀，在Redis中存储了多个应用的session时，通过配置不同的前缀方便区分
        rediFlushMode：ON_SAVE表示在响应返回时将本次请求处理中对Session的所有写动作一次提交至Redis；IMMEDIATE表示每次调用session.setAttribute时都会向Redis提交
    -->
	<bean class="org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration">
		<property name="maxInactiveIntervalInSeconds" value="1800"/>
		<property name="redisNamespace" value="shared"/>
		<property name="redisFlushMode" value="ON_SAVE"/>
	</bean>

	<!--
    	用于session交互的cookie的相关配置
        cookieName：存储sessionId的cookie名称
        cookiePath：cookie的path，默认为webapp的contextRoot
        domainName：cookie的domain：默认为请求中的域名，当需要让会话跨多个子域共享时，可通过此配置实现
        cookieMaxAge：cookie的有效期，默认为-1，即关闭浏览器时失效
    -->
	<bean class="org.springframework.session.web.http.DefaultCookieSerializer">
		<property name="cookieName" value="sh_JSESSIONID"></property>
		<!-- <property name="cookiePath" value="/sharedSession/"></property>
		<property name="domainName" value="localhost"></property>
		<property name="useHttpOnlyCookie" value="true"></property>
		<property name="cookieMaxAge" value="-1"></property> -->
	</bean>

	<!-- Jedis连接池的配置，配置项与commons-pool2支持的配置项完全一致 -->
	<bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
		<property name="maxTotal" value="20"/>
		<property name="maxIdle" value="10"/>
		<property name="minIdle" value="5"/>
    </bean>

	<!-- Jedis连接工厂配置 -->
	<bean class="org.springframework.data.redis.connection.jedis.JedisConnectionFactory">
		<property name="hostName" value="192.168.8.110"/>
		<property name="port" value="6379"/>
		<property name="poolConfig" ref="jedisPoolConfig"/>
	</bean>
</beans>
```

## log4j2相关配置
WEB-INF/log4j2.xml：
```xml
<Configuration status="WARN" monitorInterval="300">
    <properties>
    	<!-- 日志文件路径和文件名 -->
        <property name="logDir">/home/web/logs</property>
        <property name="fileName">sharedSession</property>
    </properties>
    <Appenders>
    	<!-- ConsoleAppender，向System.out输出日志 -->
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d [%t] [%level] %c - %m%n"/>
        </Console>
        <!-- RollingRandomAccessFileAppender，向文件输出日志 -->
        <!-- RandomAccessFileAppender使用NIO的RandomAccessFile类，比启用了buffered的普通FileAppender性能更好 -->
        <RollingRandomAccessFile name="File" fileName="${logDir}/${fileName}.log"
            filePattern="${logDir}/${fileName}-%d{yyyy-MM-dd}-%i.log">
            <PatternLayout pattern="%d [%t] [%level] %c - %m%n"/>
            <Policies>
            	<!-- 两重切分策略，每天切分，同时一天内日志达到10MB时切分 -->
                <TimeBasedTriggeringPolicy interval="1"/>
                <SizeBasedTriggeringPolicy size="10 MB"/>
            </Policies>
            <!-- 文件滚动策略nomax，代表一天内切分的日志文件数不受限制 -->
            <DefaultRolloverStrategy fileIndex="nomax"/>
        </RollingRandomAccessFile>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
            <!-- <AppenderRef ref="File"/> -->
        </Root>
    </Loggers>
</Configuration>
```

## web.xml
WEB-INF/web.xml：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5" xmlns="http://java.sun.com/xml/ns/javaee"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">

	<!-- log4j配置文件路径 -->
	<context-param>
        <param-name>log4jConfiguration</param-name>
        <param-value>/WEB-INF/log4j2.xml</param-value>
    </context-param>

	<!-- spring配置文件路径 -->
	<context-param>
		<param-name>contextConfigLocation</param-name>
		<param-value>/WEB-INF/spring-session.xml</param-value>
	</context-param>

	<!-- demo用的servlet -->
	<servlet>
		<servlet-name>SessionServlet</servlet-name>
		<servlet-class>kelgon.sharedSession.servlet.SessionServlet</servlet-class>
	</servlet>
	<servlet-mapping>
		<servlet-name>SessionServlet</servlet-name>
		<url-pattern>/s</url-pattern>
	</servlet-mapping>

	<!-- log4j2的Filter，过滤所有请求，做日志组件上下文的设置和清除操作，此Filter应位于FilterChain的首位 -->
    <filter>
        <filter-name>log4jServletFilter</filter-name>
        <filter-class>org.apache.logging.log4j.web.Log4jServletFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>log4jServletFilter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>FORWARD</dispatcher>
        <dispatcher>INCLUDE</dispatcher>
        <dispatcher>ERROR</dispatcher>
    </filter-mapping>

	<!-- spring session filter，此filter会将request对象指向的标准session对象替换为spring的session对象。此Filter应位于FilterChain的第二位，紧跟log4j2 filter -->
	<filter>
		<filter-name>springSessionRepositoryFilter</filter-name>
		<filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
	</filter>
	<filter-mapping>
		<filter-name>springSessionRepositoryFilter</filter-name>
		<url-pattern>/*</url-pattern>
		<dispatcher>REQUEST</dispatcher>
		<dispatcher>ERROR</dispatcher><!-- 让Filter也处理错误页的请求，以防自定义错误页中有访问session中的信息 -->
	</filter-mapping>

	<!-- log4j2的Listener，用于在启动时加载log4j2配置文件并初始化相关组件，应保证此Listener位于第一个 -->
    <listener>
        <listener-class>org.apache.logging.log4j.web.Log4jServletContextListener</listener-class>
    </listener>

	<!-- spring的ContextLoaderListener，用于在启动时加载spring配置文件并初始化相关bean，应保证此Listener仅次于log4j2 listener之后 -->
    <listener>
		<listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
	</listener>

</web-app>
```

## 特别说明

本例中的所有配置均是适用于Servlet 2.5规范的xml式配置。对于使用Servlet 3.0规范的webapp，则有更加简便的配置办法：

- spring-session针对Servlet 3.0提供了一套纯注解式的配置办法，具体见http://docs.spring.io/spring-session/docs/1.3.0.RELEASE/reference/html5/#httpsession-redis-jc
- log4j2针对Servlet 3.0也提供了免xml的配置方法，无需在web.xml中配置log4j2相关的listener和filter，log4j2会在webapp启动时自动配置

**通过web.xml中的web-app元素的"version"属性来指定Servlet规范版本**

> 不是所有的Servlet容器都支持Servlet 3.0规范。以Tomcat和Weblogic为例，Tomcat 7.0及以上和Weblogic 12及以上版本才支持Servlet 3.0