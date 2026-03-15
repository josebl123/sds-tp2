#!/bin/bash

echo "Compilando el proyecto con Maven..."
mvn -f engine/pom.xml clean compile
echo "Compilacion exitosa. Clases generadas en engine/target/classes/"