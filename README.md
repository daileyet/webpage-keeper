# Webpage Keeper
The auto download for web site library

### How to use it?
Execute the following command line:
```shell
mvn clean install
java -jar target\webpage-keeper-1.2-exec.jar -config W:\Book\default_config.xml
```

### Configuration items
####  Configuration for single downloading
see example `configs/config_default.xml`

#### Configuration for batch downloading
see example `configs/config_default_batch.xml` and its sub configuration example `configs/config_default_batch_child.xml`


### Pages structure on disk
When you set download book save directory as W:\Book on windows OS or /Book on Linux OS, then you will get following download book structure:
```
|
|---- **js**
|------ js 1
|------ js 2
|------ ...
|---- **style**
|------ css 1
|------ css 2
|------ ...
|------ **styleref**
|-------- image 1
|-------- image 2
|-------- ...
|---- page 1
|---- page 2
|---- ...
```
### Reference project
This project has been used as a lib in [SafaribooksonlineGetter4J](https://github.com/daileyet/SafaribooksonlineGetter4J) system.
