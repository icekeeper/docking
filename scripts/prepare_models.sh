#!/bin/bash

(echo "load $1" && \
 sleep 2 && \
 echo "load $2" && \
 sleep 2 && \
 echo "reset" && \
 echo "h_add" && \
 sleep 1 && \
 echo "show surface" && \
 sleep 3 && \
 echo "disable ${1%.*}" && \
 echo "save ${2%.*}.obj" && \
 sleep 2 && \
 echo "enable ${1%.*}" && \
 echo "disable ${2%.*}" && \
 echo "save ${1%.*}.obj" && \
 sleep 2 && \
 echo "quit") | pymol -cqp
./compute_surface_data.sh ${1%.*}
./compute_surface_data.sh ${2%.*}