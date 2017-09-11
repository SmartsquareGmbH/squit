# tim-it-runner

The tim integration test runner is a tool for running various tests against TIM.

## Usage

The tool is a collection of gradle tasks. Relevant ones are:

Task name                 | description
------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------
runTimITs                 | Runs the actual tests.
convertSupplyChainProject | Converts a legacy SOAP UI based test to the required format. The files are required to reside in the `src/main/test/supply-chain` directory.

The `runTimITs` task generates a report in the `build/reports` directory with relevant information for each test. A summary is also generated on the console and the build fails when at least one test fails.

The `build` directory also contains the actual requests made and the expected responses in the `source` directory. This can be interesting, as some transformations are done before (Like replacing transaction ids).

Another thing the `build` directory contains, are the results sent from TIM and the transformed ones in the respective directories `raw` and `processed`.

## Configuration

The tool features a configuration system, based on `config.properties` files. The available parameters are:

Name               | Description                                   | Example
------------------ | --------------------------------------------- | ------------------------------------------------------------------
endpoint           | The TIM endpoint to call.                     | `endpoint=http://localhost:7001/tim/ControlTaxWSSoapHttpPort?WSDL`
timdb_jdbc         | The jdbc connection for the TIM database.     | `timdb_jdbc=jdbc:oracle:thin:@localhost:1521:xe`
timdb_user         | The username of the TIM database.             | `timdb_user=timdb`
timdb_password     | The password of the TIM database.             | `timdb_password=timdb`
taxbasedb_jdbc     | The jdbc connection for the taxbase database. | `taxbasedb_jdbc=jdbc:oracle:thin:@localhost:1521:xe`
taxbasedb_user     | The username of the taxbase database.         | `taxbasedb_user=taxbase`
taxbasedb_password | The password of the taxbase database.         | `taxbasedb_password=taxbase`
ignore             | Ignores the test or a set of tests.           | `ignore=true`

Configs are evaluated recursively. That means that you can configure directly in the actual test directory or in a parent directory containing many tests.

> All parameters apart from `ignore` are required and the build will fail if at least one is missing for a test.
