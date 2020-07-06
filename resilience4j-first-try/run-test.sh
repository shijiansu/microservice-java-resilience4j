#!/bin/bash

mvn clean install -f ./resilience4j-first-try-common/pom.xml

./mvnw clean test
