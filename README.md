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

## Usage

The plugin is composed of various `Gradle` tasks. For daily usage, only the `squitTest` is relevant.<br>
The following table lists all tasks and their purpose:

Task name           | Description
------------------- | -------------------------------------------------------------------------------------------------------------------
`squitPreProcess`   | Pre processes the test sources in a configurable manner.
`squitRunRequests`  | Runs the actual requests against your backend.
`squitPostProgress` | Post processes the responses in a configurable manner.
`squitTest`         | Compares the expected and actual response and fails the build if differences were found. Also generates the report.

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

## Configuration

The plugin features a variaty of configuration possibilities. As aforementioned, these are collected in `config.properties` files. As of the current version, these are the supported ones:

Name            | Description                                                                                                                                                       | Example
--------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------
endpoint        | The endpoint of your backend to call.                                                                                                                             | `endpoint=http://localhost:1234/api`
mediaType       | The media type of your content to send.                                                                                                                           | `mediaType=application/soap+xml`
ignore          | Ignores or un-ignores the test or test group.                                                                                                                     | `ignore=true`
ignoreForReport | Ignores or un-ignores the test or test group for the report. This means that the test is run, but does not show up in anything generated at the end of the build. | `ignoreForReport=true`
db_$name_jdbc | The jdbc connection for database scripts to use | `db_myoracledb_jdbc=jdbc:oracle:thin:@localhost:1521:xe`
db_$name_username | The username for database scripts to use | `db_myoracledb_username=someuser`
db_$name_password | The password for database scripts to use | `db_myoracledb_password=somepassword`

> The parameters `endpoint` and `mediaType` are required and the build will fail if at least one is missing for a test.
