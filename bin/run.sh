#!/bin/bash
# script for run program.
# never used on jarvis platform.
#
cd $(dirname $0)/..

[ -f bin/envrc.sh ] && . bin/envrc.sh >& /dev/null
[ -f bin/env.sh ] && . bin/env.sh >& /dev/null

# check java
if [ -n "$JAVA_HOME" -a -x "$JAVA_HOME/bin/java" ]; then
  export PATH=$JAVA_HOME/bin:$PATH
fi
if [ -d jdk8 ]; then
  JAVA_HOME=jdk8
  PATH=$JAVA_HOME/bin:$PATH
fi
which java >& /dev/null

if [ $? -ne 0 ]; then
  echo "java not found"
  exit 1
fi

java -version 2>&1 | awk '/java version/ {if ($3 ~ /^"9/ || $3 ~ /^"1.8/ ) {exit 0} else {exit 1} }' >& /dev/null
if [ $? -ne 0 ]; then
  echo "java version must newer than 1.8"
  exit 1
fi

if [ -n "$PORT_JMX" ]; then
  JAVA_JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=$PORT_JMX \
     -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
fi

if [ -n "$PORT_DEBUG" ]; then
  JAVA_DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=$PORT_DEBUG,suspend=n"
fi

JAVA_OPTS=-Djava.io.tmpdir=log/temp

JAVA_GC_OPTS="-XX:+PrintGC -verbose:gc -Xloggc:log/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+ExplicitGCInvokesConcurrent"

function start() {
  [ -d log ] || mkdir log
  [ -d log/temp ] || mkdir log/temp
  if [ -f log/instance.pid ]; then
    ps aux | awk -vpid=$(cat log/instance.pid) '$2==pid {print}' | grep $(cat log/instance.pid) > /dev/null
    if [ $? -ne 0 ]; then
      rm log/instance.pid
    fi
  fi

  if [ -f log/console.log ]; then
    for ((i=0;;i++)); do
      [ ! -f log/console.log-$(date +%Y-%m-%d)-$i ] && mv log/console.log log/console.log-$(date +%Y-%m-%d)-$i && break
    done
  fi
  if [ -f log/gc.log ]; then
    for ((i=0;;i++)); do
      [ ! -f log/gc.log-$(date +%Y-%m-%d)-$i ] && mv log/gc.log log/gc.log-$(date +%Y-%m-%d)-$i && break
    done
  fi
  export SERVER_PORT=$PORT
  java $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS $JAVA_GC_OPTS $JAVA_JMX_OPTS $JAVA_OPTS \
      -jar lib/$EXEC_NAME.jar $@ >& log/console.log &
  echo $! > log/instance.pid

  echo "$APP started!"
}

function stop() {
  if [ -d log -a -f log/instance.pid ]; then
    ps aux | awk -vpid=$(cat log/instance.pid) '$2==pid {print}' | grep $(cat log/instance.pid) >& /dev/null
    if [ $? -ne 0 ]; then
      echo "already stopped!"
      return
    fi

    echo -n "stopping process "
    kill $(cat log/instance.pid)

    for ((i=0;i<40;i++)); do
      if [ $i -eq 30 ]; then
        kill -9 $(cat log/instance.pid)
      fi
      ps aux | awk -vpid=$(cat log/instance.pid) '$2==pid {print}' | grep $(cat log/instance.pid) >& /dev/null
      if [ $? -eq 0 ]; then
        echo -n .
        sleep 0.5
      else
        echo " [done]!"
        rm log/instance.pid
        break
      fi
    done
    sh bin/check.sh stop
  fi
}

action=$1
shift
case $action in
start)
  start $@
  ;;
stop)
  stop $@
  ;;
restart)
  stop $@
  start $@
  ;;
*)
  echo "Usage: $0 {start|stop|restart}"
  exit 1
esac
