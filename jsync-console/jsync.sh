#!/bin/bash
#
# Thomas Freese
echo jSync

#BASEDIR=$PWD #Verzeichnis des Callers, aktuelles Verzeichnis
BASEDIR=$(dirname $0) #Verzeichnis des Skripts
cd $BASEDIR

if [ ! -f target/classes/de/freese/jsync/JSyncConsole.class ]; then
    mvn compile;
fi

# Ausführung in der gleichen Runtime-Instanz wie Maven.
#mvn -q exec:java -Dexec.mainClass="de.freese.jsync.JSync" -Dexec.args="$1 $2 $3 $4 $5 $6" -Dexec.classpathScope=runtime

# Ausführung in einer separaten Runtime-Instanz.
mvn -q exec:exec -Dexec.executable="target/classes/de/freese/jsync/JSyncConsole.class" -Dexec.args="$@" -Dexec.classpathScope=runtime
    
cd ~
