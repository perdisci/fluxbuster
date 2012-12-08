#!/bin/bash

FLUXBUSTER_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $FLUXBUSTER_DIR/bin
java -cp .:../lib/* edu.uga.cs.fluxbuster.FluxbusterCLI $@
