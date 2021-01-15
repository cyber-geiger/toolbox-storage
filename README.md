# GEIGER local Storage implementation

This repository contains the implementation for the local storage used in the GEIGER project.

## Structure
The code is structured in multiple packages.

[localStorage](localstorage/src/main/java/ch/fhnw/geiger/localstorage) contains interfaces used in all subpackages

[localStorage.DB](localstorage/src/main/java/ch/fhnw/geiger/localstorage/db) contains files for database interactions

[localStorage.DB.mapper](localstorage/src/main/java/ch/fhnw/geiger/localstorage/db/mapper) contains database mappers to translate functions into backends (e.g., SQL)

## Build
This is a simple maven project and thus supports the common maven operations.
For building the following commands can be used:
- ./gradlew clean test jar

## Installation
As this is part of a bigger project there is no particular installation process.

## GEIGER Project
The GEIGER project aims to create a solution for small businesses to protect themselves against cyber threats by constructing an application that calculates a threat score of the current device.

More information can be found under [https://project.cyber-geiger.eu/](https://project.cyber-geiger.eu/)

## Get binaries
Precompiled binaries of the latest PRODUCTION build are available [here](https://project.cyber-geiger.eu/jenkins/job/localstorage/job/main/lastSuccessfulBuild/artifact/localstorage/build/libs/localstorage-0.0.2-SNAPSHOT.jar).

Precompiled binaries of the latest integration build are available [here](https://project.cyber-geiger.eu/jenkins/job/localstorage/job/integration/lastSuccessfulBuild/artifact/localstorage/build/libs/localstorage-0.0.2-SNAPSHOT.jar).

![Build of PRODUCTION](https://github.com/cyber-geiger/toolbox-storage/workflows/Java%20CI%20with%20Gradle/badge.svg?branch=main)
![Build of integration](https://github.com/cyber-geiger/toolbox-storage/workflows/Java%20CI%20with%20Gradle/badge.svg?branch=integration)
![Build of devlopment](https://github.com/cyber-geiger/toolbox-storage/workflows/Java%20CI%20with%20Gradle/badge.svg?branch=development)

## Quick start
### Create an ephemeral storage with a controller for testing puposes 
```Java
StorageController controller = new GenericController("theOwner",new DummyMapper());
```
### Create a persistent storage with a controller  
```Java
torageController controller = new GenericController("theOwner",new H2SqlMapper("jdbc:h2:./dbFileName;AUTO_SERVER=TRUE", "user", "Password"));
```

### Create a dummy plugin for GEIGER values to be provided
```Java
// create a new storage for testing
StorageController controller = new GenericController("theOwner",new DummyMapper());
// create a dummy feeder for values
DummyStorageFeeder cysec = new DummyStorageFeeder(controller);
// start providing values to the database in a continuous way
cysec.startFeeder();
// do whatever you want to do here
// ...
// stop feeding values
cysec.stopFeeder();
```


