# SOLEN
commons iot framework with solen slot machine ic to controll and monitor massive terminals

## Protocol
Solen provide all interface is rest style. Http Header must have a 'Content-Type: application/json' entry.
 for example:
```bash
curl -H 'Content-Type: application/json' http://ip:port/api/listAll
```

##### terminal list (all status)
终端列表(全状态)
<pre>
 path: GET /api/listAll <br/>
 response:
   [
     {
       "deviceId" : "xxx",
       "lac" : xx,
       "ci" : xx,
       "channel": {
         "registered" : true,
         "shutdown": false,
         "active": true,
       },
       "idCode" : xx,
       "inputStat" : 0,
       "outputStat" : 1,
     },
     ...
   ]
</pre>

##### terminal list with status
终端详情
<pre>
 path: GET /api/device/{deviceId} <br/>
 response:
     {
       "deviceId" : "xxx",
       "lac" : xx,
       "ci" : xx,
       "channel": {
         "registered" : true,
         "shutdown": false,
         "active": true,
       },
       "idCode" : xx,
       "inputStat" : 0,
       "outputStat" : 1,
       "reports" : [
         {
           "time" : "2019-08-01 10:00:00,112",
           "content" : "adf"
         }, {
           "time" : "2019-08-01 10:00:00,112",
           "content" : "adf"
         }
       ]
     }
</pre>

##### delete terminal
删除终端
<pre>
 path: DELETE /api/device/{deviceId}[?force=true] <br/>
 默认不可以删除连接状态正常的终端，加force参数后，可以强制删除
 response:
 不存在时返回404, 删除正常返回200, 结果是已删除的设备详情。
     {
       "deviceId" : "xxx",
       "lac" : xx,
       "ci" : xx,
       "channel": {
         "registered" : true,
         "shutdown": false,
         "active": true,
       },
       "idCode" : xx,
       "inputStat" : 0,
       "outputStat" : 1,
       "reports" : [
         {
           "time" : "2019-08-01 10:00:00,112",
           "content" : "adf"
         }, {
           "time" : "2019-08-01 10:00:00,112",
           "content" : "adf"
         }
       ]
     }
</pre>

##### send control command
发送控制命令 (cmd=3)
<pre>
  path: POST /api/sendControl
  request:
    {
      "deviceId" : "ss",
      "ctrl" : 0/1
    }
  response:
  状态码200表示成功, 结果是调试信息  
  超过20秒终端没有响应, 会返回408 请求超时
  一个设备超过3个请求， 会返回429 to many request for device
</pre>

##### send ascii
发送ascii信息 (cmd=129)
<pre>
  path: POST /api/sendAscii
  request:
    {
      "deviceId" : "ss",
      "data" : "message body"
    }
  response:
    状态码200表示成功, 结果是调试信息  
</pre>
