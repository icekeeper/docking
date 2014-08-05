#!/bin/bash

function process {
    python pdb2pqr-1.8/pdb2pqr.py --ff=PARSE $1.pdb $1.pqr
    ./surface_normalization.scala $1.obj $1.obj
}


pymol -cqr prepare_data.py -- $1
process $1_r_b
process $1_l_b
process $1_r_u
process $1_l_u

mkdir $1_data
mv $1*.pdb $1_data
mv $1*.pqr $1_data
mv $1*.obj $1_data
mv *.aln $1_data