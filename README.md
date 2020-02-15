# Squit ![CI](https://github.com/SmartsquareGmbH/squit/workflows/CI/badge.svg)

`Squit` is a `Gradle` plugin for file-based, automated testing of `JSON`, `XML`, `SOAP` and other apis.<br>
It features high customizability and speed.

## Table of Contents

- [Integration](#integration)
- [Project structure](#project-structure)
- [Usage](#usage)
  - [Configuration](#configuration)
    - [Templating](#templating)
  - [Database modifications](#database-modifications)
  - [Pre- and Post-processing](#pre--and-post-processing)
    - [Groovy processing](#groovy-processing)
    - [Interface processing](#interface-processing)
  - [Pre- and Post-runners](#pre--and-post-runners)
  - [Tagging](#tagging)
  - [Squit Dsl](#squit-dsl)
  - [Supported request formats](#supported-request-types)

## Integration

Add the [plugin](https://plugins.gradle.org/plugin/de.smartsquare.squit) to your `buildscript`:

```groovy
buildscript {
    repositories {
        gradlePluginPortal()
        jcenter()
    }

    dependencies {
        classpath "de.smartsquare:squit:3.0.0"
    }
}
```

and apply it:

```groovy
apply plugin: "de.smartsquare.squit"
```

The minimum supported Gradle version is `5.1.1`.

## Project structure

Projects are structured in arbitrarily deep folders. The plugin expects the root to be in the `src/squit` folder per default.

A single test is represented by one leaf folder. That folder **must** contain:

- A `response` file (the file ending depends on the type of test).

Further it **can** contain:

- A `test.conf` file.
- A `description.md` file.
- A `request` file (the file ending depends on the type of test).
- `db_$name_pre.sql` files.
- `db_$name_post.sql` files.

The `request` file contains whatever payload you want to send to your backend. The `response` file contains the expected response.

A `test.conf` file is required at least once on the path of your test. That means that it is resolved recursively, starting at the leaf, e.g. your test folder. The `test.conf` can and must contain various properties, which are discussed in the `Configuration` section. These properties are then merged if not existing while going up the folder tree.<br>
This allows for convenient definition of properties for multiple tests, with the ability to override properties in special cases.

The `description.md` file is an optional file containing additional descriptions for tests in the [Markdown](https://en.wikipedia.org/wiki/Markdown) format.
If the tests are nested inside each other and there are multiple description files on the path, they are merged together from top to bottom.

A simple example looks like this:

```
- src
--- test
----- my_suite
------- test1 (folder)
--------- request.xml
--------- response.xml
--------- test.conf
--------- description.md
------- test2 (folder)
--------- request.xml
--------- response.xml
------- test.conf
```

This shows a valid project structure for `Squit`. `my_suite` contains all our tests (in this case only two: `test1` and `test2`).

> You _can_ have more directories beneath `my_suite` (e.g. `another_suite`) and as aforementioned can also nest more deeply.
> At least one suite folder is required though, you can't have your tests directly in the `src/squit` folder.

`my_suite` also contains a `test.conf` file, which could look like this:

```properties
endpoint = "http://localhost:1234/endpoint"
```

`Squit` would then use `http://localhost:1234/endpoint` as the endpoint to call when running all tests in `my_suite`.<br>
As the example shows, `test1` also contains a `test.conf` file. This one could be used to override the `endpoint` property of the `test.conf` file in the `my_suite` folder.

> It is not **required** to have a `test.conf` file there, often it is enough to have one for all your tests in the root folder.

## Usage

The plugin is composed of various `Gradle` tasks. For daily usage, only the `squitTest` is relevant.<br>
The following table lists all tasks and their purpose:

| Task name          | Description                                                                                                         |
| ------------------ | ------------------------------------------------------------------------------------------------------------------- |
| `squitPreProcess`  | Pre processes the test sources in a configurable manner.                                                            |
| `squitRunRequests` | Runs the actual requests against your backend.                                                                      |
| `squitPostProcess` | Post processes the responses in a configurable manner.                                                              |
| `squitTest`        | Compares the expected and actual response and fails the build if differences were found. Also generates the report. |

To run all your tests, execute `./gradlew squitTest`.

> You do NOT need to run the clean task every time you rerun the tests. Squit and Gradle will always run the tests,
> but only pre-process again if files have actually changed, saving execution time.

### Configuration

The plugin features a variety of configuration possibilities. As aforementioned, these are collected in `test.conf` (or `local.conf`) files.
`test.conf` files are in the [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) format and support all of its features.
As of the current version, these are the supported properties:

| Name                   | Description                                                                                                                                        | Example                                                    |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| endpoint               | The endpoint of your backend to call.                                                                                                              | `endpoint = "http://localhost:1234/api"`                   |
| mediaType              | The media type of your content to send.                                                                                                            | `mediaType = "application/soap+xml"`                       |
| method                 | The method for the request to use. The default is POST and requires a request.xml. Methods like GET do not require one.                            | `method = "GET"`                                           |
| exclude                | Excludes or un-excludes the test or test group.                                                                                                    | `exclude = true`                                           |
| ignore                 | Ignores or un-ignores the test or test group. This means that the test is run, but does not show up in anything generated at the end of the build. | `ignore = true`                                            |
| databaseConfigurations | An array of database configurations to use for pre- and post scripts. See below for details.                                                       | `databaseConfigurations = [ { / *content */ } ]`           |
| preProcessors          | An array of pre processor classes to use.                                                                                                          | `preProcessors = ["com.example.ExamplePreProcessor"]`      |
| postProcessors         | An array of post processor classes to use.                                                                                                         | `postProcessors = ["com.example.ExamplePostProcessor"]`    |
| preProcessorScripts    | An array of paths to groovy pre processor scripts to use.                                                                                          | `preProcessorScripts = [./scripts/pre_processor.groovy]`   |
| postProcessorScripts   | An array of paths to groovy post processor scripts to use.                                                                                         | `postProcessorScripts = [./scripts/post_processor.groovy]` |
| preRunners             | An array of pre runner classes to use.                                                                                                             | `preRunners= ["com.example.ExamplePreRunner"]`             |
| postRunners            | An array of post runner classes to use.                                                                                                            | `postRunners = ["com.example.ExamplePostRunner"]`          |
| preRunnerScripts       | An array of paths to groovy pre runner scripts to use.                                                                                             | `preRunnerScripts = [./scripts/pre_runner.groovy]`         |
| postRunnerScripts      | An array of paths to groovy post runner scripts to use.                                                                                            | `postRunnerScripts = [./scripts/post_runner.groovy]`       |
| headers                | A map of headers to use for requests.                                                                                                              | `headers = { "some-header": "value" }`                     |
| title                  | An optional alternative title for the test.                                                                                                        | `title = "Something"`                                      |
| expectedResponseCode   | An optional expected HTTP response code. Default is the 200-range.                                                                                 | `expectedResponseCode = 400`                               |

> The parameter `endpoint` is required and the build will fail if it is missing for a test.

#### Templating

It may be useful to have a placeholder in a `test.conf` file and fill it at runtime, for example when the port of an endpoint is dynamic or when running in a CI environment.

The `HOCON` config format which `Squit` uses comes with support out of the box for it:

```properties
endpoint = "http://localhost:"${port}"/someEndpoint"
```

`port` could then be replaced when invoking `Squit` like this:

```bash
./gradlew squitTest -Psquit.port=1234
```

> This mechanism can also be used to create global configuration properties, which are then used in configuration files
deeper in the hierarchy.

#### Local configuration

Squit also allows for configuration to be stored in a `local.conf` file. `local.conf` files have a higher priority than
`test.conf` file and thus override `test.conf` files. This can be useful for overriding values of a versioned
`test.conf` without having to check that change into a VCS for every collaborator on the project. 

### Database modifications

As part of your tests, you may want to modify your database into a specific state. `Squit` allows you to do so with ordinary `sql` scripts which can be run before and after a test.

To do so, you have to add a database configuration to your `test.conf` file(s) and specify the jdbc driver to use.

A simple example would look like this:

```properties
# test.conf

databaseConfigurations = [
  {name = "mydb", jdbc = "jdbc:oracle:thin:@localhost:1521:xe", username = "someusername", password = "thepassword"}
  //=More are possible
]=
```

```groovy
// build.gradle

squit {
    jdbcDrivers = ['oracle.jdbc.driver.OracleDriver'] // You can add more if needed.
}
```

As you can see, the database properties follow a specific naming scheme. To be recognized, your database configurations must start with `db_` and you need all three shown variants (`_jdbc`, `_username`, `_password`). The name in the middle can be arbitrarily chosen and is later used to find the `sql` scripts.

You would then have to add the jdbc driver to your classpath. This can be done like this (Assuming the `jar` is in the `libs` directory of your project):

```groovy
buildscript {
    dependencies {
        classpath files("libs/ojdbc6.jar")
    }
}
```

The `sql` files are added per test. They are required to be named after the configuration you added in the `test.conf` file, ending with either `_pre.sql` or `_post.sql`. The example from before would be `mydb_pre.sql` and `mydb_post.sql`.

You can also add a `sql` script in a higher level of your project structure to merge it into existing scripts. `_pre.sql` scripts are prepended and `_post.sql` scrips are appended.<br>

The last option is to have scripts which are only run once. For this, you name the script `example_pre_once.sql` or `example_post_once.sql`.

### Pre- and Post-processing

`Squit` allows you to pre- and post-process the requests and actual responses. This may be required for incremental ids you have no control over, dates or other things.

There are currently two ways to do so: Using `Groovy` scripts or implementing a specific `interface`.

#### Groovy processing

`Groovy` processing is the more easy, but less flexible option.

You add a `.groovy` script somewhere in your project and supply `Squit` with the path:

```properties
# test.conf

preProcessorScripts = [./some/path/pre_process.groovy]
```

As for the pre process step, the script gets passed `request` and `expectedResponse` objects, which types depend on the request type. See [supported request types](#supported-request-types).

A simple script could look like this:

```groovy
import java.time.LocalDate

request.selectNodes("//Date").each {
    it.text = LocalDate.now().toString()
}
```

> The passed objects for the post-processor are called `actualResponse` and `expectedResponse`. Note that the `expectedResponse` is only for reference here and changes to it are not reflected.

#### Interface processing

Implementing the `Squit` interfaces is harder to set up, but much more flexible than the scripting approach.

To do so, you need to set up a [buildSrc](https://docs.gradle.org/current/userguide/organizing_build_logic.html#sec:build_sources) project or an external project, which is added to the classpath. In the following example, a `buildSrc` project is set up:

Create the `buildSrc` folder and set up a normal project in your preferred JVM language. After that, you add the `Squit` library to your dependencies:

```groovy
repositories {
    gradlePluginPortal()
    jcenter()
}

dependencies {
    compile 'de.smartsquare:squit:3.0.0'
}
```

Then you can implement one of the `interfaces`. An example for the `SquitXmlPreProcessor` equivalent to the scripting example could look like this:

```java
import de.smartsquare.squit.SquitPreProcessor;
import org.dom4j.Document;

import java.time.LocalDate;

public class MyPreProcessor implements SquitXmlPreProcessor {

    @Override
    public void process(Document request, Document expectedResponse) {
        request.selectNodes("//Date")
                .forEach(it -> it.setText(LocalDate.now().toString()));
    }
}
```

> The other interfaces as of the current version are `SquitXmlPostProcessor`, `SquitJsonPostProcessor`, `SquitJsonPreProcessor`.

The last step is to add the class to your `test.conf`, similar to the approach with `groovy` scripts:

```properties
# test.conf

preProcessors = ["com.example.MyPreProcessor"]
```

### Pre- and Post-runners

Similar to [Pre- and Post-processing](#pre--and-post-processing), it is possible to specify implementations or scripts,
which can run arbitrary code before and after each request. The setup is analogous to pre- and post-processors.
See the [configuration](#configuration) section for the different options to enable them in your `test.conf`.

### Tagging

Tags allow to run only a subset of your tests to save time and resources if needed.

You can specify tags in corresponding `test.conf` files. An example could look like this:

```properties
tags = ["fast", "mysuite"]
```

All tests covered by this `test.conf` file would then be tagged as `fast` and `mysuite`.<br>
To run only tests with the tag `fast`, you would invoke `Squit` like so:

```bash
./gradlew squitTest -Ptags=fast
```

You can also specify more tags by separating with a `,`. Tags are then linked like an "and".
If you specify `-Ptags=fast,mysuite` a test would need to have both tags to be included.
If you want to have the semantics of an "or", use `-PtagsOr`.

`Squit` also automatically tags your tests named on the folders they reside in. If you have a test in the folder `test1`, it would have the tag `test1` and could be run exclusively by invoking `./gradlew squitTest -Ptags=test1`

### Supported request types

As of the current version, Squit supports these request formats:

| Media Type         | File ending | Pre- and post-processor input                                                               |
| ------------------ | ----------- | ------------------------------------------------------------------------------------------- |
| `application/xml`  | `.xml`      | [Dom4J Documents](http://static.javadoc.io/org.dom4j/dom4j/2.1.0/org/dom4j/Document.html)   |
| `application/json` | `.json`     | [Gson JsonElements](https://google.github.io/gson/apidocs/com/google/gson/JsonElement.html) |
| All others         | `.txt`      | :x:                                                                                         |


### Squit Dsl

Here is a complete example of the `Squit` dsl:

```groovy
squit {
    // The jdbc drivers to use. Must be on the classpath.
    jdbcDrivers = ['oracle.jdbc.driver.OracleDriver']

    // The path of your test sources. src/squit is the default.
    sourceDir "src/squit"

    // The path to save reports in. build/squit/reports is the default.
    reportDir "build/squit/reports"

    // The timeout for requests before squit fails. The default is 10.
    timeout = 60

    // If Squit should only print if a tests fails.
    silent = false

    // If the task should pass on test failures or not.
    ignoreFailures = false

    xml {
        // If the xml diffing should be strict.
        // If not, differences like namespace prefixes are not reported as differences.
        strict = true

        // If xml should be canonicalized for the html report.
        // This means that both expected and actual response are transposed into a common format.
        canonicalize = true
    }

    json {
        // If json should be canonicalized for the html report.
        // This means that both expected and actual response are transposed into a common format.
        canonicalize = true
    }
}
```

> All of the shown settings are optional.
