# Squit

`Squit` is a `Gradle` plugin for automated testing of `Xml` and/or `Soap` based apis.<br>
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
  - [Tagging](#tagging)
  - [Squit Dsl](#squit-dsl)

## Integration

Add the [plugin](https://plugins.gradle.org/plugin/de.smartsquare.squit) to your `buildscript`:

```groovy
buildscript {
    repositories {
        maven { url "https://plugins.gradle.org/m2/" }
        jcenter()
    }

    dependencies {
        classpath "gradle.plugin.de.smartsquare:squit-plugin:1.1.0"
    }
}
```

and apply it:

```groovy
apply plugin: "de.smartsquare.squit"
```

## Project structure

Projects are structured in arbitrarily deep folders. The plugin expects the root to be in the `src/test` folder per default.

A single test is represented by one leaf folder. That folder **must** contain:

- A `request.xml` file.
- A `response.xml` file.

Further it **can** contain:

- A `test.conf` file.
- `db_$name_pre.sql` files.
- `db_$name_post.sql` files.

The `request.xml` file contains whatever payload you want to send to your backend. The `response.xml` file contains the expected response.

A `test.conf` file is required at least once on the path of your test. That means that it is resolved recursively, starting at the leaf, e.g. your test folder. The `test.conf` can and must contain various properties, which are discussed in the `Configuration` section. These properties are then merged if not existing while going up the folder tree.<br>
This allows for convenient definition of properties for multiple tests, with the ability to override properties in special cases.

A simple example looks like this:

```
- src
--- test
----- my_suite
------- test1 (folder)
--------- request.xml
--------- response.xml
--------- test.conf
------- test2 (folder)
--------- request.xml
--------- response.xml
------- test.conf
```

This shows a valid project structure for `Squit`. `my_suite` contains all our tests (in this case only two: `test1` and `test2`).

> You *can* have more directories beneath `my_suite` (e.g. `another_suite`) and as aforementioned can also nest more deeply.<br>

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

Task name          | Description
------------------ | -------------------------------------------------------------------------------------------------------------------
`squitPreProcess`  | Pre processes the test sources in a configurable manner.
`squitRunRequests` | Runs the actual requests against your backend.
`squitPostProcess` | Post processes the responses in a configurable manner.
`squitTest`        | Compares the expected and actual response and fails the build if differences were found. Also generates the report.

### Configuration

The plugin features a variety of configuration possibilities. As aforementioned, these are collected in `test.conf` files.
`test.conf` files are in the [hocon](https://github.com/lightbend/config/blob/master/HOCON.md) format and support all of its features.
As of the current version, these are the supported properties:

Name                   | Description                                                                                                                                        | Example
---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------
endpoint               | The endpoint of your backend to call.                                                                                                              | `endpoint = "http://localhost:1234/api"`
mediaType              | The media type of your content to send.                                                                                                            | `mediaType = "application/soap+xml"`
exclude                | Excludes or un-excludes the test or test group.                                                                                                    | `exclude = true`
ignore                 | Ignores or un-ignores the test or test group. This means that the test is run, but does not show up in anything generated at the end of the build. | `ignore = true`
databaseConfigurations | An array of database configurations to use for pre- and post scripts. See below for details.                                                       | `databaseConfigurations = [ { / *content */ } ]`
preProcessors          | An array of pre processor classes to use                                                                                                           | `preProcessors = ["com.example.ExamplePreProcessor"]`
postProcessors         | An array of post processor classes to use                                                                                                          | `postProcessors = ["com.example.ExamplePostProcessor"]`
preProcessorScripts    | An array of paths to groovy pre processor scripts to use                                                                                           | `preProcessorScripts = [./scripts/pre_processor.groovy]`
postProcessorScripts   | An array of paths to groovy post processor scripts to use                                                                                          | `postProcessorScripts = [./scripts/post_processor.groovy]`

> The parameter `endpoint` is required and the build will fail if it is missing for a test.

#### Templating

It may be useful to have a placeholder in a `test.conf` file and fill it at runtime, for example when the port of an endpoint is dynamic or when running in a CI environment.

`Squit` features a simple templating engine, borrowed from [Groovy](http://docs.groovy-lang.org/next/html/documentation/template-engines.html). An example for such a template would look like this:

```properties
endpoint = "http://localhost:"${port}"/someEndpoint"
```

`port` could then be replaced when invoking `Squit` like this:

```bash
./gradlew squitTest -Pport=1234
```

### Database modifications

As part of your tests, you may want to modify your database into a specific state. `Squit` allows you to do so with ordinary `sql` scripts which can be run before and after a test.

To do so, you have to add a database configuration to your `test.conf` file(s) and specify the jdbc driver to use.

A simple example would look like this:

```
# test.conf

databaseConfigurations = [
  {name = "mydb", jdbc = "jdbc:oracle:thin:@localhost:1521:xe", username = "someusername", password = "thepassword"}
  // More are possible
]
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

`Squit` allows you to pre- and post-process the requests and actual responses. This max be required for incremental ids you have no control over, dates or other things.

There are currently two ways to do so: Using `Groovy` scripts or implementing a specific `interface`.

#### Groovy processing

`Groovy` processing is the more easy, but less flexible option.

You add a `.groovy` script somewhere in your project and supply `Squit` with the path:

```
# test.conf

preProcessorScripts = [./some/path/pre_process.groovy]
```

As for the pre process step, the script gets passed `request` and `expectedResponse` objects, which are [Dom4J Documents](https://dom4j.github.io/).

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
    jcenter()
}

dependencies {
    compile 'de.smartsquare:squit-library:1.1.0'
}
```

Then you can implement one of the `interfaces`. An example for the `SquitPreProcessor` equivalent to the scripting example could look like this:

```java
import de.smartsquare.squit.SquitPreProcessor;
import org.dom4j.Document;

import java.time.LocalDate;

public class MyPreProcessor implements SquitPreProcessor {
    @Override
    public void process(Document request, Document expectedResponse) {
        request.selectNodes("//Date")
                .forEach(it -> it.setText(LocalDate.now().toString()));
    }
}
```

> The other interface as of the current version is the `SquitPostProcessor`.

The last step is to add the class to your `test.conf`, similar to the approach with `groovy` scripts:

```
# test.conf

preProcessors = ["com.example.ExamplePreProcessor"]
```

### Tagging

Tags allow to run only a subset of your tests to save time and resources if needed.

You can specify tags in corresponding `test.conf` files. An example could look like this:

```
tags = ["fast", "mysuite"]
```

All tests covered by this `test.conf` file would then be tagged as `fast` and `mysuite`.<br>
To run only tests with the tag `fast`, you would invoke `Squit` like so:

```bash
./gradlew squitTest -Ptags=fast
```

> You can also specify more tags by separating with a `,`.

`Squit` also automatically tags your tests named on the folders they reside in. If you have a test in the folder `test1`, it would have the tag `test1` and could be run exclusively by invoking `./gradlew squitTest -Ptags=test1`

### Squit Dsl

Here is a complete example of the `Squit` dsl:

```groovy
squit {
    // The jdbc drivers to use. Must be on the classpath.
    jdbcDrivers = ['oracle.jdbc.driver.OracleDriver']

    // The path of your test sources. src/test is the default.
    sourcesPath = projectDir.toPath().resolve("src").resolve("test")

    // The path to save reports in. build/squit/reports is the default.
    reportsPath = buildDir.toPath().resolve("squit").resolve("reports")

    // The timeout for requests before squit fails. The default is 10.
    timeout = 60
}
```

> All of the shown settings are optional.
