#!/bin/bash

./surface_normalization.scala $1.obj $1.obj
sed -n 's/v \([Ee0-9\.\-]*\) \([Ee0-9\.\-]*\) \([Ee0-9\.\-]*\).*/\1,\2,\3/p' < $1.obj > $1.csv
./pyMLP.py -v -i $1.pdb -o $1_lip.dx
python pdb2pqr-1.8/pdb2pqr.py --ff=PARSE --apbs-input $1.pdb $1.pqr
LC_ALL=en_UK.utf-8 awk -v name=$1 '{ if ($0 ~ /[fc]glen.*/)  printf "    %s %f %f %f\n", $1, $2 + 200, $3 + 200, $4 + 200; else if($0 ~ /write pot dx.*/) printf "    write pot dx %s_pot\n", name ; else print $0;  }' $1.in > $1.tmp.in && mv $1.tmp.in $1.in
./apbs $1.in
./multivalue $1.csv $1_lip.dx $1_lip.csv
./multivalue $1.csv $1_pot.dx $1_pot.csv
rm $1_lip.dx
rm $1_pot.dx
rm io.mc
rm $1-input.p
mkdir $1_data
mv $1.obj $1_data
mv $1.pdb $1_data
mv $1.pqr $1_data
mv $1.csv $1_data
mv $1.in $1_data
mv $1_lip.csv $1_data
mv $1_pot.csv $1_data

