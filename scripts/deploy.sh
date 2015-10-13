#!/bin/sh

# detect this script location (also resolve links since $0 may be a softlink)
PRG="$0"
while [ -h $PRG ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
SCRIPTS_DIR=$(dirname "$PRG")
AWALE_DIR=$(cd "$SCRIPTS_DIR"/.. && pwd)

activator dist
scp $AWALE_DIR/target/universal/awale-1.0-SNAPSHOT.zip yamo@penguen:/home/yamo

ssh -T yamo@penguen << "EOF"
  kill -9 $(cat awale-1.0-SNAPSHOT/RUNNING_PID)
  rm awale-1.0-SNAPSHOT/RUNNING_PID
  unzip -o awale-1.0-SNAPSHOT.zip
  ./awale-1.0-SNAPSHOT/bin/awale
EOF
