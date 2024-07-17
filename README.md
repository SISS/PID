# Persistent Identifier Dispatcher Service

- [Overview](#overview)
- [History](#history)
  - [Original Documention](#original-documention)
  - [Original Software](#original-software)
  - [ONACI PID Service Fork](#onaci-pid-service-fork)
- [Usage](#usage)
  - [Container Build](#container-build)
    - [Stage 1: `builder`](#stage-1-builder)
    - [Stage 2: `server`](#stage-2-server)
    - [Build Arguments](#build-arguments)
  - [Runtime Deployment](#runtime-deployment)
    - [Environment Variables](#environment-variables)
    - [Database Initialisation](#database-initialisation)
    - [Hostname and Path](#hostname-and-path)
  - [Development Environment](#development-environment)
    - [Build the PID Service Image](#build-the-pid-service-image)
    - [Auto-initialize the PostgreSQL Container](#auto-initialize-the-postgresql-container)
    - [Launch the PID Service Tomcat Container](#launch-the-pid-service-tomcat-container)


## Overview

The Persistent Identifier Dispatcher Service (PID Service) enables resolution of persistent identifiers.

It intercepts all incoming HTTP requests, and matches them with preconfigured patterns that have been stored in a persistent relational data store (e.g. PostgreSQL).  The requests are then re-routed according to the rules configured for the matched pattern with possible actions including:

- HTTP header manipulation
- HTTP redirects
- Reverse-proxying proxying requests
- Delegating resolution to another service, etc.

The application features an extendable architecture for future improvements and supports multiple control interfaces - a Graphical User interface (UI) as well as programmable API for remote user-less management of URI mapping rules.

&nbsp;

## History

This PID Service was originally created as part of CSIRO's Solid Earth and Environment GRID (SEE GRID) Spatial Information Services Stack (SISS).

That initiative ended back in 2014, and most associated resources - including build services, wikis and other documentation are no longer online.

### Original Documention

The best way to access the original documentation is now via the Internet Archive:

- [SEE GRID SISS PID Service User Guide](https://web.archive.org/web/20200405033839/https://www.seegrid.csiro.au/wiki/Siss/PIDServiceUserGuide)
- [SEE GRID SISS PID Service API Documentation](https://web.archive.org/web/20200315043706/https://www.seegrid.csiro.au/wiki/Siss/PIDServiceAPI)
- [Limited documentation on CSIRO's Confluence](https://confluence.csiro.au/x/9ZMKGg)

### Original Software

The original software repository *is* still available at <https://github.com/SISS/PID>, but it has not been updated or supported in quite some time.

### ONACI PID Service Fork

CSIRO's Coastal Informatics team still have a use for the PID Service, so we have created this fork so we can update it to work with more modern deployment environments.

The changes we have made to the original include:

- Updated to newer Java Runtime Versions
- Removed dead links from the User Interface
- Upgraded key library versions, including:
  - Log4J2
  - PostgreSQL JDBC Driver
- Added support for docker build and deployment
- Added support for docker-compose based development

&nbsp;

## Usage

These usage docs relate only to the ONACI fork of this repository.

### Container Build

The [Dockerfile](./Dockerfile) in the repository root defines a multistage build environment for the PID Service:

#### Stage 1: `builder`

This stage uses the official Maven base image to compile and package the PID Service's Java Servlet application.

You can build *just* this stage by running:

```bash
docker build --tag onaci/pidsvc:builder --target builder .
```

The resulting .war file will be located at `/app/target/` in the resulting image.

&nbsp;

#### Stage 2: `server`

This build stage installs the .war file packaged in the `builder` stage into a production-ready tomcat server container that can be used to deploy the PID Service application into a container-enabled hosting environment.

```bash
docker build --tag onaci/pidsvc:server --target server .
```

&nbsp;

#### Build Arguments

The container build stages understand the following build-time arguments:

| ARG | Description | Default Value |
|-----|-------------|---------------|
| `BUILD_REVISION` | The base-version of the PID Service Java Servlet application <sup>1</sup> | `1.2` |
| `BUILD_NUMBER` | The container-build version that is being compiled. <sup>1</sup> | `dev` |
| `JAVA_VERSION` | The Java Runtime version you want to compile the Java Servlet application for <sup>2</sup> | `11` |
| `TOMCAT_VERSION` | The version of the [Apache Tomcat](https://tomcat.apache.org/) `server` you want to deploy at runtime <sup>2</sup>| `9.0` |

**<sup>1</sup>** The packaged .war file name will be `pidsvc-${BUILD_REVISION}.${BUILD_NUMBER}.war`

**<sup>2</sup>** The pattern `${TOMCAT_VERSION}-jdk${JAVA_VERSION}` *must* match a valid tag of the [onaci/tomcat-base](https://hub.docker.com/r/onaci/tomcat-base) image.

&nbsp;

### Runtime Deployment

The PID Service application must be deployed with access to a postgresql database, which is where it will store all its patterns and rules.

#### Environment Variables

If you are using a docker image built from the [Dockerfile](./Dockerfile) in this container, then you can use the following environment variables to specify the connection details for that database:

| Environment Variable | Description | Default Value |
|----------------------|-------------|---------------|
| `POSTGRES_DB` | Name of the posgresql database to store data in | `pidsvc` |
| `POSTGRES_HOST` | Hostname for the postgresql server | `pidsvc-db` |
| `POSTGRES_PORT` | Port that the postgresql server is listening on | `5432` |
| `POSTGRES_PASSWORD`<br/>`POSTGRES_PASSWORD_FILE` | Plain-text password to connect to the postgresql server with or (with the `_FILE` suffix) absolute path to a docker secret that contains the password <sup>3</sup>| `pidsvc123` |
| `POSTGRES_USERNAME`<br/>`POSTGRES_USERNAME_FILE` | Username to connect to the postgresql server as or (with the `_FILE` suffix) absolte path to a docker secret that contains the username. |

**<sup>3</sup>** Do *NOT* use this default password in production, please!  Secure your databases, and use docker secrets in preference to plain-text environment variables so that the password is not included in the result of a `docker inspect` call.

In addition to the database connection parameters, you can also use any of the environment variables for the base images to configure the properties of the tomcat servlet container.  Please see:

- [onaci/tomcat-base Environment Variables](https://github.com/onaci/tomcat-base/blob/main/README.md#environment-variables)
- [unidata/tomcat-docker Configuration](https://github.com/Unidata/tomcat-docker/blob/latest/README.md#configuration)

&nbsp;

#### Database Initialisation

If you are starting with a completely blank postgresql database, you will need to run the SQL script from [src/main/db/postgresql.sql](./src/main/db/postgresql.sql) to set up the database schema.

This must be done manually - the PID Service application does not do it automatically.

**NOTE:** If your postgresql server is also a docker container, you can just add this script to the `/docker-entrypoint-initdb.d/` directory to have the database auto-initialise on the first launch. See: [The official postgres image docs](https://github.com/docker-library/docs/blob/master/postgres/README.md#initialization-scripts) for details.

&nbsp;

#### Hostname and Path

Whatever subdomain you deploy your PIDSvc at, it will intercept requests for *every* HTTP request to that subdomain, not just the ones that match its context path.

The Graphical User Interface and API will be accessible at the `/pidsvc` path, while all other paths will be matched with the configured patterns and handled appropriately.

**NOTE:** Please make sure you add proper security to the `pidsvc` path: it is NOT appropriate to leave it accessible to anonynous users in production, and the application does not handle this by default.

&nbsp;

### Development Environment

The [docker compose file](./docker-compose.yml) in this repository's root defines a containerised development environment for this application.

#### Build the PID Service Image

```bash
docker compose build
```

&nbsp;

#### Auto-initialize the PostgreSQL Container

By default, this will initialize an empty PID Service database, with the schema all set up but no path patterns or rules configured.

If you *also* want to restore a backup of your production database created with `pg_dump`, you should:

- Place your `pg_dump` output file into the [docker-init/postgresql/](./docker-init/postgresql/) subdirectory of this repository
- Create a `.env` file in the repository root, and in that file, define two additional environment variables:
  - `POSTGRES_DUMP_FILE` should be set to the name of your `pg_dump` file.  If this file has been compressed with `gzip`, make sure the filename and the value of this variable both have a `.gz` extension.
  - `POSTGRES_DUMP_FORMAT` should be set to the format that was used when the `pg_dump` file was created. If not set, `tar` will be assumed.


```bash
# Ensure all containers are stopped and volumes removed
# (Because auto-initialization only runs if the postgres
# container's storage volume is empty)
docker compose down -v

# Launch the PostgreSQL container in the background
docker compose up -d pidsvc-db

# Watch the PostgreSQL containers logs to be sure that
# the initialisation has suceeded. Ctrl-c to stop watching.
docker compose logs -f pidsvc-db
```

&nbsp;

#### Launch the PID Service Tomcat Container

```bash
docker compose up pidsvc-tomcat
```

The PID Service's administration GUI will be accessible at <http://localhost:8080/pidsvc/>
