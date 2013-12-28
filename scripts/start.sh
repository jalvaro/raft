#!/bin/bash
#$1: numHosts
#optional args:
#-p <port of TestServer>: listening port for TestServer
#-h <IP address of TestServer>: IP Address of TestServer
#-pResults <percentageRequiredResults>: percentage of received results prior to perform evaluation (e.g. 50 means 50%, 75 means 75%)
#--remoteMode: Server will run in different computers (or more than one Server in a single computer but this computer having the same internal and external IP address)
#--localMode: (default running mode. If no mode is specified it will suppose local mode) all Serves will run in the same computers
#--menu: run in menu mode
#--logResults: appends the result of the each execution to a file named as the groupId
#-path <path>: path to directory where store results (if --logResults is activated)
#--remoteTestServer: indicates that the TestServer runs in a different computer that Servers
#--noremove: deactivates the generation by simulation of operations that remove recipes

# killall java

#killall -9 rmiregistry >& /dev/null
#rmiregistry &
#(cd ../bin; rmiregistry &)

#sleep 1

LOCAL_TEST_SERVER="true"
PHASE1="false"
for TOKEN in $*
do
	if [ $TOKEN = "--remoteTestServer" ]; then
		LOCAL_TEST_SERVER="false"
	fi
	if [ $TOKEN = "--phase1" ]; then
		PHASE1="true"
	fi
done

if [ $PHASE1 = "true" ]; then
# phase 1
	java -cp ../bin recipesService.test.Phase1TestServer $* &
	java -cp ../bin:../../LSim-libraries/* recipesService.Phase1 $*
else
	# phase 2 to 4
	if [ $LOCAL_TEST_SERVER = "true" ]; then
		java -cp ../bin:../../LSim-libraries/* recipesService.test.server.TestServer $* &
		sleep 1
	fi

	sleep 1

	java -cp ../bin recipesService.test.server.SendArgsToTestServer $*

	sleep 3

	for (( i = 0 ; i < $1; i++ ))
	do
	FILE="../results/f_$i"
		if [ $1 -le 4 ]; then	
			# runs each java process in a different terminal emulator window
			# in case you want to run all processes in the same terminal emulator window run:
			#		java -cp ./bin:../2013p-practica-SD--LSim-lib/* recipesService.Server $1 $2 &
			#gnome-terminal -x java -cp ../bin:../../LSim-libraries/* recipesService.Server $* &>$FILE &
#			java -classpath ../bin:../../LSim-libraries/* recipesService.Server $* &
				java -classpath ../bin:../../LSim-libraries/* recipesService.Server $* &
#				gnome-terminal -x java -classpath ../bin:../../LSim-libraries/* -Djava.rmi.server.codebase=file:../bin/ recipesService.Server $* >$FILE &
			#gnome-terminal -x java -cp ../bin:../../LSim-libraries/* recipesService.Server $* &
		else
			# runs all java processes in the same terminal emulator window
	#		java -cp ../bin:../../LSim-libraries/* recipesService.Server $* >$FILE &

#			java -classpath ../bin:../../LSim-libraries/* recipesService.Server $* &
			java -classpath ../bin:../../LSim-libraries/* -Djava.rmi.server.codebase=file:../bin/ recipesService.Server $* &
			#java -cp ../bin:../../LSim-libraries/* recipesService.Server $* &
		fi
	done
fi

wait

