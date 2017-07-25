# jetty-load-base
A jetty-base used for load testing

## To Build
```
mvn clean install
```

## To Run the Server
You will need to run either the 9.2 or the 9.4 server:
### To Run 9.2 Server

```
cd 9.2/target/jetty-base
java -jar ../java-distribution*/start.jar

```

### To Run 9.4 Server
```
cd 9.4/target/jetty-base
java -jar ../java-home*/start.jar

```


## To populate the Database
```
cd generate/target
populate.sh
```


## To run the load generator
```
cd generate/target
run.sh
```




