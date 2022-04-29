# Route my way - public transport journey planner

Practical part of my Bachelors thesis implementing Profile Connection Scan Algorithm for public transport journey planning. 

# How to run
## Configuration
Change your configuration in `Backend/src/main/resources/application.yaml`. If you plan on using another RDBMS, add it to build.gradle and change driver in `application.yaml`. Then check if provided sql file in `Backend/src/main/resources/db/migrations` is compatible with your chosen DB system (should be, just to be sure).

## Build manually 
First build Frontend using npm. Run `npm run build`. Then copy contents of the created `Frontend/build` to `Backend/src/main/resources/static`. After that build your Backend using gradle. Run `gradlew build`. This command also creates a fat jar in `Backend/build/libs`. You can run this jar as any other jvm application.


## Build and run using bash script
Run bash script `build.sh` in the root directory. At the end of the run file `App.jar` will be created in root folder. You can run it yourself as any other jar file or use provided bash scrint `run.sh`.

## Build and run using Docker
Provided Dockerfile containt build and run parts. All you need to do is to run classic `docker build .` command in the root directory. After that just run the created image.

## Run
After starting the application you can open a demo web page at `http://localhost:<your_port>`. 

# Data upload
To upload your data in GTFS format, you need to provide a password. That is configurable in `Backend/src/main/resources/application.yaml`. Default password is `admin`. This process takes some time, usually up to 5 minutes depending on your configuration. After that you can search journeys between stops. The Frontend application provides some autocomplete capabilities, but those are limited. Selecting number of generated journeys is just for debug purposes as algorithm used to compute them can reliably generate only one journey at a time.


# Tests
This project contains some tests for checking correctness of the algorithm. They compare output of this application with Google Directions API output. As this Google service is not free, you need to provide your own API key to them. This key has to be inside of an environmental variable GOOGLE_API_KEY.