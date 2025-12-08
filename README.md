# dojo_java
Excercises to excel at the programming language Java

Getting started
https://dev.java/
https://dev.java/learn/
https://dev.java/learn/language-basics/
https://docs.oracle.com/en/java/javase/25/index.html
https://docs.oracle.com/en/java/javase/25/docs/api/index.html

https://docs.gradle.org/current/userguide/userguide.html

create new project
```
mkdir project
cd project
gradle init --type java-application --dsl kotlin
```

view dependencies
```.\gradlew :app:dependencies```

using gradle for build & test (Windows)
```
.\gradlew.bat clean build
.\gradlew.bat test
```

run
```.\gradlew.bat run```

compile & run using Java SDK
```
javac <file1> <file2>
java '<class>'
```
