# NWDI-Sonar-Plugin

## Introduction

The Jenkins NWDI-Sonar-Plugin runs Sonar on SAP NetWeaver development components.

## Building and installing the plugin

The plugin is not available through the Jenkins update center yet. To build the plugin
the following GitHub repositories have to be cloned:
```
git clone git://github.com/weigo/NWDI-config-plugin.git
git clone git://github.com/weigo/NWDI-pom-Plugin.git
git clone git://github.com/weigo/NWDI-Core-Plugin.git
git clone git://github.com/weigo/NWDI-Sonar-Plugin.git
```
Now the Maven projects can be built:
```
for d in NWDI-config-plugin NWDI-pom-Plugin NWDI-Core-Plugin NWDI-Sonar-Plugin;\
  do (cd $d; mvn install); done
```
Upload the the **NWDI-Core-Plugin** and **NWDI-Sonar-Plugin** using the Jenkins Update Center.
Be sure to install the **NWDI-Core-Plugin** first and afterwards the **NWDI-Sonar-Plugin**.

## Dependencies and global configuration

The plugin relies on Maven to actually execute the Maven Sonar plugin. You will
have to configure your Jenkins user's _settings.xml_ as described
<a href="http://docs.codehaus.org/display/SONAR/Installing+and+Configuring+Maven">here</a>.

In the main configuration view configure a maven installation.

## Project configuration

In the NWDI Project configuration view add NWDI-Sonar-Plugin as a build step and save the configuration. 