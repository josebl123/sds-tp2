#!/bin/bash

if [ "$#" -eq 0 ]; then

    java -cp engine/target/classes ar.edu.itba.sds.Simulation
else
    echo "Ejecutando con parametros personalizados..."
    java -cp engine/target/classes ar.edu.itba.sds.Simulation "$@"
fi