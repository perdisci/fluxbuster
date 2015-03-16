## Introduction ##

Fluxbuster is designed to cluster and classify candidate fast flux networks from
SIE DNS traffic data. Data is read from timestamped compressed data files
and stored in a relational database. The clustering process uses an
agglomerative hierarchical clustering algorithm. Classification involves a
pre-constructed decision tree.


## Compilation ##

Build Requirements:
  1. [Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html) JDK 1.6 or greater
  1. [ant](http://ant.apache.org/)

To compile Fluxbuster from the directory containing the build.xml file execute
the following command:

```
ant build
```

This will build all of the class files and place them into the bin directory.

To clean up the build from the same directory execute the following command:

```
ant clean
```

**NOTE**: all of the libraries necessary to build and execute Fluxbuster are
included in the lib directory.

## Documentation ##

The javadocs for Fluxbuster are located in the doc directory.

They can be generated from the source code by executing:

```
ant doc
```

To clean up the javadocs execute:

```
ant clean-doc
```


## Installation ##

As Fluxbuster only currently supports PostgreSQL, PostgreSQL version 8.4 or
greater should be installed to your database host. After this is accomplished
complete the following steps to install the Fluxbuster database.

  1. Create a database user to own the Fluxbuster database. If Fluxbuster will be installed on the same host as the database the Fluxbuster user would only need local access to the database, otherwise the user must be given permission to access the database from the host running Fluxbuster.
  1. Create a database to house the Fluxbuster db schema.
  1. Grant your database user ownership of the newly created database.
  1. As the Fluxbuster user execute the fluxbuster\_schema.sql file found in the resources directory on the Fluxbuster database. This will create the tables, indexes, etc. required by Fluxbuster.


Fluxbuster includes one vital component as a python script. This script
found in the resources directory extract\_candidate\_flux\_domains.py parses
SIE information to produce the input data files for Fluxbuster. This script
requires the following configuration to execute.

  1. Install Python 2.X (2.5 or greater)
  1. Install make sure the following modules are installed.
    * os
    * sys
    * gc
    * socket
    * time
    * math
    * nmsg
    * wdns
    * time
    * gzip
    * threading
    * Queue
    * datetime
    * sets

NOTE: nmsg and wdns can be located at [Farsight Security](https://dl.farsightsecurity.com/dist/)

Once python and the required modules have been installed the
extract\_candidate\_flux\_domains.py should be run as a daemon process. You
should set the LOG\_DIR variable in the script to the location where you
wish to place the parsed data files.

## Configuration ##

Fluxbuster is configured through several .properties files in the bin directory once the program has been built.  What follows is a description of each of the attributes in the .properties files.

#### `fluxbuster.properties` ####

  * **SELECTED\_CFD\_FILE** : The path to a list of domains that should be clustered regardless of their candidate score. If this is properties is not specified it is ignored.
  * **GOOD\_CANDIDATE\_THRESHOLD** : The candidate score threshold. Those candidate domains with a candidate score greater than the threshold are considered for clustering. This value should be between 0.0 and 1.0. ex. 0.5
  * **MAX\_CANDIDATE\_DOMAINS** : The total number of candidate domains to cluster. ex. 1000
  * **DIST\_MATRIX\_MULTITHREADED** : Should the calculation of the distance matrix be multithreaded. Valid values are 'true' or 'false'.
  * **DIST\_MATRIX\_NUMTHREADS** : The number of threads to use in multithreaded distance matrix calculation. This value must be a positive integer. If DIST\_MATRIX\_MULTITHREADED is set to 'false' this value is ignored.
  * **LINKAGE\_TYPE** : The linkage type to used during hierarchical clustering. Valid values are 'Single' or 'Complete'
  * **MAX\_CUT\_HEIGHT** : The maximum cut height used during hierarchical clustering.  The value should be between 0.0 and 1.0. ex. 0.75
  * **CANDIDATE\_FLUX\_DIR** : The directory containing the SIE source files.  This directory must contains the logfiles produced by extract\_candidate\_flux\_domains.py
  * **DBINTERFACE\_CONNECTINFO** : The jdbc connection string to the fluxbuster database.  ex. jdbc:postgresql://host.example.com:54321/fluxbuster?user=sample&password=secret
  * **DBINTERFACE\_DRIVER** : The full class name of the JDBC driver class to use.  ex. org.postgresql.Driver
  * **DBINTERFACE\_CLASS** : The full class name of the DBInterface implementation to use.  ex. edu.uga.cs.fluxbuster.db.PostgresDBInterface
  * **FEATURES\_PATH** : A path to store the constructed features file. This file is generated dynamically each time the classifier is called.  ex. /tmp/unlabeled\_tmp.arff

The following are options related BoneCP.  In most cases the default options will suffice.  For further information about each option see:  http://jolbox.com/bonecp/downloads/site/apidocs/com/jolbox/bonecp/BoneCPConfig.html

  * **DBINTERFACE\_PARTITIONS** : BoneCPConfig.setPartitionCount
  * **DBINTERFACE\_MIN\_CON\_PER\_PART** : BoneCPConfig.setMinConnectionsPerPartition
  * **DBINTERFACE\_MAX\_CON\_PER\_PART** : BoneCPConfig.setMaxConnectionsPerPartition
  * **DBINTERFACE\_RETRY\_ATTEMPTS** : BoneCPConfig.setAcquireRetryAttempts
  * **DBINTERFACE\_RETRY\_DELAY** : BoneCPConfig.setAcquireRetryDelayInMs

#### `commons-logging.properties` ####

  * **org.apache.commons.logging.Log** : Sets the logging implementation class. By default this is set to `org.apache.commons.logging.impl.SimpleLog` which can be configured in the simplelog.properties file.

#### `simplelog.properties` ####

  * **org.apache.commons.logging.simplelog.defaultlog** : the default minimum logging level.
  * **org.apache.commons.logging.simplelog.showdatetime** : display a timestamp for each log entry.
  * **org.apache.commons.logging.simplelog.showlogname** : Set to true if you want the Log instance name to be included in output messages.

## Usage ##

First, make sure extract\_candidate\_flux\_domains.py is running, and producing the files containing the candidate flux domains.

You can execute Fluxbuster via the fluxbuster.sh bash script.

<pre>usage: fluxbuster.sh<br>
<br>
If none of the options g, f, s, c are specified then the program will execute as<br>
if all of them have been specified.  Otherwise, the program will only execute<br>
the options specified.<br>
<br>
-?,--help                Print help message.<br>
-c,--classifiy-clusters  Classifiy clusters. (Optional)<br>
-d,--start-date <arg>    The start date of the input data.  Should be in<br>
yyyyMMdd format.<br>
-e,--end-date <arg>      The end date of the input data.  Should be in yyyyMMdd<br>
format.<br>
-f,--calc-features       Calculate cluster features. (Optional)<br>
-g,--generate-clusters   Generate clusters. (Optional)<br>
-s,--calc-similarity     Calculate cluster similarities. (Optional)<br>
</pre>

1. Get all the domains and to which cluster they belong for a the run on
2010-11-13.

```
select
  clusters.cluster_id,
  domains.domain_name,
  domains.second_level_domain_name
from
  clusters
    join
  domains
    on
  clusters.log_date = domains.log_date
  and clusters.domain_id = domains.domain_id
where
  clusters.log_date = '2010-11-13'
```

2. Get all of the cluster features for each cluster for the run on 2010-11-13.

```
select
  cluster_feature_vectors.*
from
  clusters
    join
  cluster_feature_vectors
    on
  clusters.log_date = cluster_feature_vectors.log_date
  and clusters.cluster_id = cluster_feature_vectors.cluster_id
where
  clusters.log_date = '2010-11-13'
```

3. Get all of the classified clusters for the run on 2010-11-15.

```
select
  distinct(cluster_classes.*)
from
  clusters
    join
  cluster_classes
    on
  clusters.log_date = cluster_classes.log_date
  and clusters.cluster_id = cluster_classes.cluster_id
where
  clusters.log_date = '2010-11-15'
```