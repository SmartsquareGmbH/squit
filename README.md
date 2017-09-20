# Squit

`Squit` is a `Gradle` plugin for automated testing of `Xml` and/or `Soap` based apis.<br>
It features high customizability and speed.

## Integration

Add the plugin to your `buildscript`:

```groovy
buildscript {
    repositories {
        mavenLocal()
    }

    dependencies {
        classpath "de.smartsquare:squit-plugin:1.0-SNAPSHOT"
    }
}
```

and apply it:

```groovy
apply plugin: 'squit'
```

## Project structure

Projects are structured in arbitarilly deep folders. The plugin expects the root to be in the `src/test` folder.

A single test is represented by one leaf folder. That folder **must** contain:

- A `request.xml` file.
- A `response.xml` file.

Further it **can** contain:

- A `config.properties` file.
- `db_$name_pre.sql` files.
- `db_$name_post.sql` files.

The `request.xml` file contains whatever payload you want to send to your backend. The `response.xml` file contains the expected response.

A `config.properties` file is required at least once on the path of your test. That means that it is resolved recursively, starting at the leaf, e.g. your test folder. The `config.properties` can and must contain various properties, which are discussed in the `Configuration` section. These properties are then merged if not existing while going up the folder tree.<br>
This allows for convenient definition of properties for multiple tests, with the ability to override properties in special cases.

A simple example looks like this:

```
- src
--- test
----- my_tests
------- test1 (folder)
--------- request.xml
--------- response.xml
--------- config.properties
------- test2 (folder)
--------- request.xml
--------- response.xml
------- config.properties
```

This shows a valid project structure for `Squit`. `my_tests` contains all our tests (in this case only two: `test1` and `test2`). `my_test` also contains a `config.properties` file, which could look like this:

```properties
endpoint=http://localhost:1234/endpoint
```

`Squit` would then use `http://localhost:1234/endpoint` as the endpoint to call when running all tests in `my_tests`.<br>
As the example shows, `test1` also contains a `config.properties` file. This one could be used to override the `endpoint` property of the `config.proeprties` file in the `my_tests` folder.

> It is not **required** to have a `config.properties` file there, often it is enough to have one for all your tests in the root folder.

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

The plugin features a variaty of configuration possibilities. As aforementioned, these are collected in `config.properties` files. As of the current version, these are the supported ones:

Name              | Description                                                                                                                                                       | Example
----------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------
endpoint          | The endpoint of your backend to call.                                                                                                                             | `endpoint=http://localhost:1234/api`
mediaType         | The media type of your content to send.                                                                                                                           | `mediaType=application/soap+xml`
ignore            | Ignores or un-ignores the test or test group.                                                                                                                     | `ignore=true`
ignoreForReport   | Ignores or un-ignores the test or test group for the report. This means that the test is run, but does not show up in anything generated at the end of the build. | `ignoreForReport=true`
db_$name_jdbc     | The jdbc connection for database scripts to use                                                                                                                   | `db_myoracledb_jdbc=jdbc:oracle:thin:@localhost:1521:xe`
db_$name_username | The username for database scripts to use                                                                                                                          | `db_myoracledb_username=someuser`
db_$name_password | The password for database scripts to use                                                                                                                          | `db_myoracledb_password=somepassword`

> The parameters `endpoint` and `mediaType` are required and the build will fail if at least one is missing for a test.

#### Templating

It may be useful to have a placeholder in a `config.properties` file and fill it at runtime, for example when the port of an endpoint is dynamic or when running in a CI environment.

`Squit` features a simple templating engine, borrowed from [Groovy](http://docs.groovy-lang.org/next/html/documentation/template-engines.html). An example for such a template would look like this:

```properties
endpoint=http://localhost:$port/someEndpoint
```

`port` could then be replaced when invoking `Squit` like this:

```bash
./gradlew squitTest -Pport=1234
```

### Database modifications

As part of your tests, you may want to modify your database into a specific state. `Squit` allows you to do so with ordinary `sql` scripts which can be run before and after a test.

To do so, you have to add a database configuration to your `config.properties` file(s) and specify the jdbc driver to use.

A simple example would look like this:

```properties
# config.properties

db_mydb_jdbc=jdbc:oracle:thin:@localhost:1521:xe
db_mydb_username=someusername
db_mydb_password=somepassword
```

```groovy
// build.gradle

squit {
    jdbcDriver = "oracle.jdbc.driver.OracleDriver"
}
```

As you can see, the database properties, follow a specific naming scheme. To be recognized, your database configurations must start with `db_` and you need all three shown variants (`_jdbc`, `_username`, `_password`). The name in the middle can be arbitarilly chosen and is later used to find the `sql` scripts.

You would then have to add the jdbc driver to your classpath. This can be done like this (Assuming the `jar` is in the `libs` directory of your project):

```groovy
buildscript {
    dependencies {
        classpath files("libs/ojdbc6.jar")
    }
}
```

The `sql` files are added per test. They are required to be named after the configuration you added in the `config.properties` file, ending with either `_pre.sql` or `_post.sql`. The example from before would be `mydb_pre.sql` and `mydb_post.sql`.

### Pre- and Post-processing

`Squit` allows you to pre- and post-process the requests and actual responses. This max be required for incremental ids you have no control over, dates or other things.

There are currently two ways to do so: Using `Groovy` scripts or implementing a specific `interface`.

#### Groovy processing

`Groovy` processing is the more easy, but less flexible option.

You add a `.groovy` script somewhere in your project and supply `Squit` with the path (In this example, the script is present in the root of the project):

```groovy
squit {
    preProcessorScriptPath = projectDir.toPath().resolve("myscript.groovy")
}
```

As for the pre process step, the script gets passed `request` and `expectedResponse` objects, which are [Dom4J Documents](https://dom4j.github.io/).

A simple script could look like this:

```groovy
import java.time.LocalDate

request.selectNodes("//Date").each {
    it.text = LocalDate.now().toString()
}
```

> Note that the `response` is only for reference, changes to it are not reflected. Use the post-processor for modifying the `expectedResponse`.

> The passed objects for the post-processor are called `actualResponse` and `expectedResponse`. Again, `expectedResponse` is only for reference.

#### Interface processing

Implementing the `Squit` interfaces is harder to set up, but much more flexible than the scripting approach.

To do so, you need to set up a [buildSrc](https://docs.gradle.org/current/userguide/organizing_build_logic.html#sec:build_sources) project or an external project, which is added to the classpath. In the following example, a `buildSrc` project is set up:

Create the `buildSrc` folder and set up a normal project in your prefered JVM language. After that, you add the `Squit` library to your dependencies:

```groovy
dependencies {
    compile 'de.smartsquare:squit-library:1.0-SNAPSHOT'
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

> Other interfaces as of the current version are the `SquitPostProcessor` and the `SquitDatabaseInitializer`.

### Tagging

Tags allow to run only a subset of your tests to save time and resources if needed.

Tags need to be defined in corresponding `config.properties` files before usage. An example could look like this:

```properties
tags=fast,mysuite
```

All tests covered by this `config.properties` file would then be tagged as `fast` and `mysuite`.<br>
To run only tests with the tag `fast`, you would invoke `Squit` like so:

```bash
./gradlew squitTest -Ptags=fast
```

> You can also specify more tags by separating with a `,`.

### Squit Dsl

Here is a complete example of the `Squit` dsl:

```groovy
squit {
    // The jdbc driver to use. Must be on the classpath.
    jdbcDriver = "oracle.jdbc.driver.OracleDriver"

    // The pre-processor to use. Must be on the classpath.
    preProcessorClass = "com.yourcompany.CoolPreProcessor"

    // The post-processor to use. Must be on the classpath.
    postProcessorClass = "com.yourcompany.EvenCoolerPostProcessor"

    // The database initializer to use. Must be on the classpath.
    databaseInitializerClass = "com.yourcompany.VeryCoolDatabaseInitializer"

    // The pre-processing script to use.
    preProcessorScriptPath = projectDir.toPath().resolve("somescript.groovy")

    // The post-processing script to use.
    postProcessorScriptPath = projectDir.toPath().resolve("anotherscript.groovy")

    // The path of your test sources. src/test is the default.
    sourcesPath = projectDir.toPath().resolve("src").resolve("test")

    // The path to save reports in. build/squit/reports is the default.
    reportsPath = buildDir.toPath().resolve("squit").resolve("reports")
}
```

> All of the shown settings are optional.
