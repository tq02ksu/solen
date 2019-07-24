#!/bin/bash
#
# script for check current state.
# never used on jarvis platform.
#
cd $(dirname $0)/..

[ -f bin/env.sh ] && . bin/env.sh >& /dev/null

action=${1:-start}

function start() {
  ## check process
  if [ ! -f log/instance.pid ]; then
    echo "log/instance.pid not found!"
    exit 1
  fi

  ps aux | awk -vpid=$(cat log/instance.pid) '$2==pid {print}' | grep $(cat log/instance.pid) >& /dev/null
  if [ $? -ne 0 ]; then
    echo "process not found, pid=$(cat log/instance.pid)"
    exit 2
  fi
  ## check port
  echo -n "checking port "
  for ((i=0;i<600;i++)); do
    /usr/sbin/ss -nl | awk -vPORTS="$PORT,$PORT_DEBUG,$PORT_JMX,$PORT_RPC" '
         { split($3, arr, ":"); listen[arr[2]] = 1 }
         END {
           split(PORTS, p, ",");
           for (i in p) { if( p[i] && ! listen[p[i]] ) { fail=fail" "p[i] } }
           if (fail) { exit 1 }
         }'
    if [ $? -ne 0 ]; then
      echo -n .
      sleep 0.5
    else
      echo [done]
      break
    fi
  done

  ## check actuator
  echo -n 'checking health '
  curl -sf http://localhost:$PORT/actuator/health >& /dev/null
  if [ $? -ne 0 ]; then
    echo '[FAILED]'
    exit 4
  fi
  echo '[done]'
}

function stop() {
  ## check process
  if [ -f log/instance.pid ]; then
    ps aux | awk -vpid=$(cat log/instance.pid) '$2==pid {print}' | grep $(cat log/instance.pid) >& /dev/null
    if [ $? -eq 0 ]; then
      echo "process found, stop failed, pid=$(cat log/instance.pid)"
      exit 1
    fi
  fi

  ## check port
  for ((i=0;i<20;i++)); do
    /usr/sbin/ss -nl | awk -vPORTS="$PORT,$PORT_DEBUG,$PORT_JMX,$PORT_RPC" '
         { split($3, arr, ":"); listen[arr[2]] = 1 }
         END {
           split(PORTS, p, ",");
           for (i in p) { if( p[i] && listen[p[i]] ) { fail=fail" "p[i] } }
           if (fail) { exit 1 }
         }'
    if [ $? -ne 0 ]; then
      echo -n .
      sleep 0.5
    else
      break
    fi
  done
}

case $action in
  start)
    start $@
    ;;
  stop)
    stop $@
    ;;
esac
