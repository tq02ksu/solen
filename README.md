# solen
commons iot framework with solen slot machine ic to controll and monitor massive terminals

## protocol
Solen provide all interface is rest style. Http Header must have a 'Content-Type: application/json' entry.
 for example:
```bash
curl -H 'Content-Type: application/json' http://ip:port/api/listAll
```
##### terminal list
终端列表
<pre>
 path: GET /api/list
 response: ['deviceId1', 'deviceId2' ]
</pre>

##### terminal list with status
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
