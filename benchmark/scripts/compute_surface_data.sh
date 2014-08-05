#!/bin/bash

./surface_normalization.scala $1.obj $1.obj
python pdb2pqr-1.8/pdb2pqr.py --ff=PARSE $1.pdb $1.pqr
mkdir $1_data
mv $1.obj $1_data
mv $1.pdb $1_data
mv $1.pqr $1_data
mv $1.csv $1_data
mv $1.in $1_data
mv $1_pot.csv $1_data

