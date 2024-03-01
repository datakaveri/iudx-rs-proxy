

![IUDX](./docs/iudx.png)

# iudx-resource-proxy-server

The resource server proxy is [Data Exchange](https://iudx.org.in) data discovery portal.
The consumers can access data from the resource server proxy using HTTPs.

<p align="center">
<img src="./docs/RS Proxy.png">
</p>


## Features

- Provides data access from available resources using standard APIs.
- Search and count APIs for searching through available data: Support for Complex (Temporal +  Attribute), Temporal (Before, during, After) and Attribute searches.
- Integration with authorization server (token introspection) to serve private data as per the access control policies set by the provider
- Secure data access over TLS 
- Scalable, service mesh architecture based implementation using open source components: Vert.X API framework, Postgres for database.
- Hazelcast and Zookeeper based cluster management and service discovery


## Prerequisites 
### External Dependencies Installation 

The resource server proxy connects with various external dependencies namely
- ELK stack 
- PostgreSQL
- ImmuDB


Find the installations of the above along with the configurations to modify the database url, port and associated credentials in the appropriate sections
 [here](SETUP.md)

## Get Started

### Docker based
1. Install docker and docker-compose
2. Clone this repo
3. Build the images 
   ` ./docker/build.sh`
4. Modify the `docker-compose.yml` file to map the config file you just created
5. Start the server in production (prod) or development (dev) mode using docker-compose 
   ` docker-compose up prod `


### Maven based
1. Install java 13 and maven
2. Use the maven exec plugin based starter to start the server 
   `mvn clean compile exec:java@proxy-server`
   
### JAR based
1. Install java 11 and maven
2. Use maven to package the application as a JAR
   `mvn clean package -Dmaven.test.skip=true`
3. 2 JAR files would be generated in the `target/` directory
   - `iudx.rs.proxy-cluster-0.0.1-SNAPSHOT-fat.jar` - clustered vert.x containing micrometer metrics
   - `iudx.rs.proxy-dev-0.0.1-SNAPSHOT-fat.jar` - non-clustered vert.x and does not contain micrometer metrics

#### Running the clustered JAR

**Note**: The clustered JAR requires Zookeeper to be installed. Refer [here](https://zookeeper.apache.org/doc/r3.3.3/zookeeperStarted.html) to learn more about how to set up Zookeeper. Additionally, the `zookeepers` key in the config being used needs to be updated with the IP address/domain of the system running Zookeeper.

The JAR requires 3 runtime arguments when running:

* --config/-c : path to the config file
* --hostname/-i : the hostname for clustering
* --modules/-m : comma separated list of module names to deploy

e.g. `java -jar target/iudx.rs.proxy-cluster-0.0.1-SNAPSHOT-fat.jar  --host $(hostname) -c configs/config.json -m iudx.rs.proxy.database.DatabaseVerticle,iudx.rs.proxy.authenticator.AuthenticationVerticle 
,iudx.rs.proxy.metering.MeteringVerticle,iudx.rs.proxy.database.postgres.PostgresVerticle`

Use the `--help/-h` argument for more information. You may additionally append an `RS_JAVA_OPTS` environment variable containing any Java options to pass to the application.

e.g.
```
$ export RS_JAVA_OPTS="-Xmx4096m"
$ java $RS_JAVA_OPTS -jar target/iudx.rs.proxy-cluster-0.0.1-SNAPSHOT-fat.jar ...
```

#### Running the non-clustered JAR
The JAR requires 1 runtime argument when running:

* --config/-c : path to the config file

e.g. `java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.Log4j2LogDelegateFactory -jar target/iudx.rs.[proxy]-dev-0.0.1-SNAPSHOT-fat.jar -c configs/config.json`

Use the `--help/-h` argument for more information. You may additionally append an `RS_JAVA_OPTS` environment variable containing any Java options to pass to the application.

e.g.
```
$ export RS_JAVA_OPTS="-Xmx1024m"
$ java $RS_JAVA_OPTS -jar target/iudx.rs.proxy-dev-0.0.1-SNAPSHOT-fat.jar ...
```

### Encryption
All the count and search APIs have a feature to get encrypted data.
The user could provide a `publicKey` in the header, with the value that is generated from [lazySodium sealed box](https://github.com/terl/lazysodium-java/wiki/Getting-started) or [PyNaCl sealed box](https://pynacl.readthedocs.io/en/latest/).
The header value should be in _url-safe base64 format_.
The encrypted data could be decrypted using the lazysodium sealed box by supplying the private and public key.

## Contributing
We follow Git Merge based workflow 
1. Fork this repo
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name, or refer to a milestone name as mentioned in Github -> Projects 
3. Commit to your fork and raise a Pull Request with upstream

## License
[View License](./LICENSE)
