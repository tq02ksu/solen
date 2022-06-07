create table device (
  device_id varchar(32) not null primary key comment '设备ID',
  status tinyint not null default 0 comment '状态, ',
  lac int not null default 0 comment '经度',
  ci int not null default 0 comment '纬度',
  id_code varchar(32) not null comment '设备id code',
  input_stat tinyint not null default 0 comment '输入端状态',
  output_stat tinyint not null default 0 comment '输出端状态',
  rssi tinyint not null comment '信号强度',
  voltage double not null default 0.0 comment '电压',
  temperature double not null deafult 0.0 comment '',
  gravity t
) comment '设备' ;


create table connection(
  connection_id varchar(32) not null primary key comment '连接ID',
  device_id varchar(32) not null comment '设备id',

);

create cached table event (
  event_id varchar(32) not null primary key
);