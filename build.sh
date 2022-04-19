cd Frontend
rm -rf build
npm run build
cd ..
rm -rf Backend/src/main/resources/static/*
mv Frontend/build/* Backend/src/main/resources/static/
cd Backend
./gradlew clean build
cd build/libs
mv $(ls -I '*plain*') ../../../app.jar