Formplayer
===========

Formplayer is a RESTful XForm entry service written on the [Spring Framework](https://projects.spring.io/spring-framework/).
Given a [user restore](https://confluence.dimagi.com/display/commcarepublic/OTA+Restore+API) and
an [XForm](http://dimagi.github.io/xform-spec/) Formplayer enables form entry via JSON calls and responses (detailed below).
These files will often be hosted by a [CommCareHQ](https://www.github.com/dimagi/commcare-hq) server instance.
Formplayer relies on the [CommCare](https://www.github.com/dimagi/commcare-core) libraries (included as subrepositories).
Formplayer is built via [Gradle](https://spring.io/guides/gs/gradle/) (wrapper files included).

See also the [dev guide](DEV_GUIDE.md) for more resources to get started.

### Dependencies
+ Java (OpenJDK 17)
+ Formplayer caches session instances via Redis
+ Formplayer stores session instances via Postgres
+ Formplayer builds SQLite database for each restored user

Building and Running
------------
Clone formplayer repository

    $ git clone https://github.com/dimagi/formplayer.git

Download submodule dependencies

    $ git submodule update --init --recursive

Mac users may need to comment out references to micrometer in the following files in order to run formplayer:

    build.gradle

    src/main/java/org/commcare/formplayer/application/WebAppContext.java

To make properties file:

    $ cp config/application.example.properties config/application.properties  # Update properties as necessary (the defaults are fine for running locally)

In particular, make sure the `server.port` value is the same as the port from `FORMPLAYER_URL` in commcarehq's `localsettings.py`.

Make sure you have the formplayer database created. You will be asked to provide a password after running this command; assuming you are running formplayer locally, you should use the password for the postgres user associated with the locally-running instance of commcare hq (which can be found in the `DATABASES` section of your `localsettings.py` file).

    $ createdb formplayer -U commcarehq -h localhost  # Update connection info as necessary (the defaults are fine for running locally)

If you are running postgres in Docker, you may need to run this in the Docker shell, using `./scripts/docker bash postgres` from the commcarehq repository.

To run (with tests):

    $ ./gradlew build && java -jar build/libs/formplayer.jar

To run without tests:

    $ ./gradlew assemble && java -jar build/libs/formplayer.jar

To test:

    $ ./gradlew test

    # to run a single test class
    $ ./gradlew :test --tests=NewFormTests
    
    # to run a single test method
    $ ./gradlew :test --tests=NewFormTests.testNewForm

    # to run in continuous mode
    $ ./gradlew test -t
    
    # to run tests from libs/commcare
    $ ./gradlew :commcare:test --tests org.commcare.cases.test.CaseXPathQueryTest

When building on Linux it is sometimes necessary to run:

    $ gradle wrapper

#### Troubleshooting

*Could not resolve project :commcare*

Run `git submodule update --init`

*Compilation error*

e.g. `no suitable constructor found for OutputFormat(Document)`

You're likely running the wrong version of Java. Check with `java -version` which should show `17`

- Install OpenJDK 17
- Configure gradle to use the new Java
  - Update `~/.gradle/gradle.properties` with `org.gradle.java.home=/JDK_PATH`
  - OR run append this to gradle commands: `-Dorg.gradle.java.home=/JDK_PATH`

*Could not find tools.jar on MacOS*

A Big Sur update caused the built-in Java to take precedence. Follow the steps in the number one answer [here](https://stackoverflow.com/questions/64968851/could-not-find-tools-jar-please-check-that-library-internet-plug-ins-javaapple) to add the non "Internet Plug-Ins" Java to your path.

### Running in IntelliJ

In order to set breakpoints, step through code, and link the runtime with source you'll need to run the code in an IDE. We use IntelliJ. To setup

1. Download [IntelliJ IDE](https://www.jetbrains.com/idea/download/#section=mac)
2. Open IntelliJ and select "Import Project"
3. Navigate to the cloned `formplayer` repository and select `build.gradle` at the root
4. De-select "Use auto-import" and "Create directories for empty content roots automatically" and *select* "Use gradle wrapper"
5. Click "OK"
6. Create a run configuration:
   1. Open the run configurations (by clicking on the drop down menu next to the play button in the top).
   2. Click the + in the upper left corner and then "JAR Application"
   3. Pick any name (e.g. "Formplayer")
   4. Set to jar path to <path to repo>/formplayer/build/libs/formplayer.jar
   5. Hit the + in the "before launch" section and add project: formplayer, task: bootJar 
      (faster than assemble since it does not run tests)
   6. save

After following these steps IntelliJ may need further configuration to work smoothly with Gradle. You might also want to install 
the [Lombok plugin](https://plugins.jetbrains.com/plugin/6317-lombok) for your IDE to resolve Lombok references.

Note: You can also use Android Studio as your IDE and follow the same steps as above.

#### Keeping your application.properties up to date

Properties are occasionally added to application.example.properties that will be required to run on the latest version.

If you experience an error after updating, try running

```bash
diff -u config/application{.example,}.properties
```

Lines your file is missing will begin with a `-`.

### Running in Docker

If you want to run Formplayer in Docker as a service of CommCare HQ, follow these steps from
your commcare-hq repository.

If you are making changes to Formplayer you probably just want to run it from the shell and not
in Docker. In that case you'll also need to stop the Formplayer Docker container if it is running:

```
docker stop hqservice_formplayer_1
```

#### Running CommCare HQ in Docker

Just run `scripts/docker runserver -d` from the CommCare HQ repo root. You can then access CommCare HQ at
`http://localhost:8000`.

#### Running CommCare HQ outside of Docker

1. Start the CommCare services: `scripts/docker up -d`
2. Run CommCare HQ: `python manage.py runserver 0.0.0.0:8000`
   - Note that you must not bind the process to `localhost` or `127.0.0.1` otherwise Formplayer will not be able to
    communicate with CommCare HQ.

### Contributing

For PRs that just modify code in the Formplayer repo, submit a PR to Formplayer on a separate branch.


#### Contributing changes to commcare

Formplayer also has a dependency on the commcare-core repository. The commcare-core `master` branch is not
stable and Formplayer uses a different branch. The submodule repo `libs/commcare` should always be pointing to
the `formplayer` branch.

#### Updating the CommCare version

When updating Formplayer to have a new release of a CommCare version (e.g. 2.34 to 2.35), a PR should be opened from the `commcare_X.Y` branch into
the `formplayer` branch. Once QA has been finished, merge the PR and update the Formplayer submodule.

### IntelliJ Code Style Settings

In order to comply with code style guidelines we follow, please use [Commcare Coding Style file](https://github.com/dimagi/formplayer/blob/master/.ide_settings/codestyles/CommCare%20Coding%20Style.xml)  as your respective code style in IntelliJ. To do so follow these instructions

1. Go to IntelliJ preferences -> Editor -> Code Style.
2. Click on the settings icon to the right of the “Scheme” label at the top and select “Import Scheme” from the drop-down.
3. Select the [Commcare Coding Style file](https://github.com/dimagi/formplayer/blob/master/.ide_settings/codestyles/CommCare%20Coding%20Style.xml) file from the file chooser
4. Select “Ok” on the “Import Scheme” dialog.
5. Click on “Apply” and then “OK” to save the settings.

These instructions might differ depending on the IntelliJ version you are on.
