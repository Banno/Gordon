## Contributing

Contributions are welcome. You can use the included `test_...` modules to locally test changes to the Gordon plugin.
1. Make changes to Gordon plugin
2. Run `./gradlew publishToMavenLocal`
3. Uncomment the `com.banno.gordon` plugin in test module buildscripts and change the version to the one you just deployed (which can be found in `gordon-plugin/gradle.properties`)
4. Test your changes by running tests using the locally-deployed Gordon
   - `./gradlew test_app:gordon` should have some tests that pass, some that fail, some that are ignored, and a flaky one that might pass or fail
   - `./gradlew test_feature:gordon` should have 2 tests that pass and 1 that is ignored
   - `./gradlew test_library:gordonBar` should have 2 tests that pass and 1 that is ignored
   - `./gradlew test_library:gordonBaz` should have 3 tests that are ignored
5. Make any required changes to the plugin's unit tests and/or add new tests to cover any new behavior
6. Run `./gradlew lintKotlin test`
7. Open a pull request!
