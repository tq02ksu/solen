create table if not exists device (
    device_id varchar(32) not null primary key , -- comment '设备ID'
    status varchar(32) not null , -- default 0comment '状态, '
    lac int not null, --  default 0comment '基站lac'
    ci int not null, -- default 0 comment '基站ci'
    id_code varchar(32) not null, -- comment '设备id code',
    input_stat tinyint not null, -- default 0, -- comment '输入端状态',
    output_stat tinyint not null, -- default 0, -- comment '输出端状态',
    rssi tinyint not null, -- default 0, -- comment '信号强度',
    voltage double not null, -- default 0.0, -- comment '电压',
    temperature double not null, -- default 0.0, -- comment '',
    gravity tinyint not null, -- default 0, -- comment '重力感应',
    uptime int not null, -- default 0 -- comment 开机时间
    lat double not null, -- default 0 comment '纬度'
    lng double not null -- default 0 comment '经度'
); -- comment '设备' ;


create table if not exists connection (
  connection_id varchar(32) not null primary key, -- comment '连接ID',
  device_id varchar(32) not null -- comment '设备id',
);

create index if not exists idx_connection_device_id on connection ( device_id );

create cached table if not exists event (
  event_id varchar(32) not null primary key,
  device_id varchar(32) not null,
  type tinyint not null,
  time datetime not null,
  details varchar(1024) not null
);

create index if not exists idx_event_device_id on event ( device_id );
create index if not exists idx_event_type on event ( type );