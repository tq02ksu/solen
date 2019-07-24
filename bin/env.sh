#!/bin/bash
# script to compatible with multiple pass platform, such as eden, jarvis, opera, k8s, etc.
# initially only support eden.
#
# never used on jarvis platform.
#
# usage: source env.sh
# output: environment variables like:
#   PRODUCT_LINE  产品线
#   APP        模块名
#   INSTANCE   实例标识
#   PLATFORM   平台
#   ENV_TYPE   平台类型 ONLINE/OFFLINE
#   LOGIC_IDC  逻辑机房
#   PORT       主端口
#   PORT_DEBUG DEBUG 端口
#   PORT_JMX   JMX 端口
#   PORT_RPC   RPC 端口
#   JAVA_HOME  JVM目录
#   CONFIG_VERSION 配置版本
#   CONFIG_PROFILE 配置profile
#   JAVA_MEM_OPTS  内存配置

_paas=

# constants
ENV_TYPE_ONLINE=env-type-online
ENV_TYPE_OFFLINE=env-type-offline
# gauss env type

## for test environment
function test() {
  PRODUCT_LINE=cpd
  APP=$DEFAULT_APP
  INSTANCE=$(hostname -i)
  if [ -z "$PLATFORM" ]; then
    PLATFORM=offline
  fi
  ENV_TYPE=env-type-offline
  LOGIC_IDC=hb
  PORT=8080
  PORT_DEBUG=$((RANDOM%200+8800))
  PORT_RPC=7889
  if [ -z "$JAVA_HOME" ]; then
    for exec in $(find /home/work/local -name javac); do
      $exec -version 2>&1 | awk '/javac/ {if ($2 ~ /^9/ || $2 ~ /^1.8/ ) {exit 0} else {exit 1} }' >& /dev/null
      if [ $? -eq 0 ]; then
        JAVA_HOME=${exec%/bin/javac}
        break
      fi
    done
  fi
  CONFIG_VERSION=
  ENV_TYPE=$ENV_TYPE_OFFLINE
  CONFIG_PROFILE=$ENV_TYPE,$PLATFORM
  JAVA_MEM_OPTS=" -server -Xms256m -Xmx256m -Xmn64m -XX:SurvivorRatio=4 -Xss320K -XX:+UseConcMarkSweepGC"
}

export PRODUCT_LINE APP INSTANCE PLATFORM ENV_TYPE LOGIC_IDC PORT PORT_RPC CONFIG_VERSION CONFIG_PROFILE EXEC_NAME
