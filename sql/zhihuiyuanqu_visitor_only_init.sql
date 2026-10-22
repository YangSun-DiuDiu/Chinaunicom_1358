-- ============================================================
-- 智慧园区访客管理系统 - 精简初始化脚本（仅访客管理）
-- 生成日期: 2026-10-21
-- 适用版本: RuoYi-Vue 裁剪版（后端 ruoyi-admin + 前端 ruoyi-ui，只保留系统管理基础 + 访客管理）
-- 内容: 基础系统表(14) + 访客业务表(3) + 短信表(5) + 菜单/角色/账号/参数
-- 说明: 设备/考勤/会议/宿舍/文档/通知/监控/工具等业务全部裁剪，本脚本不含对应表与菜单
-- ============================================================

-- ============================================================
-- 一、基础系统表
-- ============================================================

-- ----------------------------
-- 1、部门表
-- ----------------------------
drop table if exists sys_dept;
create table sys_dept (
  dept_id           bigint(20)      not null auto_increment    comment '部门id',
  parent_id         bigint(20)      default 0                  comment '父部门id',
  ancestors         varchar(50)     default ''                 comment '祖级列表',
  dept_name         varchar(30)     default ''                 comment '部门名称',
  order_num         int(4)          default 0                  comment '显示顺序',
  leader            varchar(20)     default null               comment '负责人',
  phone             varchar(11)     default null               comment '联系电话',
  email             varchar(50)     default null               comment '邮箱',
  status            char(1)         default '0'                comment '部门状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (dept_id)
) engine=innodb auto_increment=200 comment = '部门表';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------
insert into sys_dept values(100,  0,   '0',          '智慧园区',   0, '系统管理员', '15888888888', 'admin@zhihuiyuanqu.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(101,  100, '0,100',      '行政部',     1, '', '', '', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(102,  100, '0,100',      '保安部',     2, '', '', '', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(103,  100, '0,100',      '工程部',     3, '', '', '', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(104,  100, '0,100',      '物业部',     4, '', '', '', '0', '0', 'admin', sysdate(), '', null);


-- ----------------------------
-- 2、用户信息表
-- ----------------------------
drop table if exists sys_user;
create table sys_user (
  user_id           bigint(20)      not null auto_increment    comment '用户ID',
  dept_id           bigint(20)      default null               comment '部门ID',
  user_name         varchar(30)     not null                   comment '用户账号',
  job_number        varchar(50)     default ''                 comment '工号(钉钉job_number)',
  nick_name         varchar(30)     not null                   comment '用户昵称',
  user_type         varchar(2)      default '00'               comment '用户类型（00系统用户）',
  email             varchar(50)     default ''                 comment '用户邮箱',
  phonenumber       varchar(11)     default ''                 comment '手机号码',
  sex               char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
  avatar            varchar(100)    default ''                 comment '头像地址',
  photo             varchar(200)    default ''                 comment '人员照片路径',
  password          varchar(100)    default ''                 comment '密码',
  status            char(1)         default '0'                comment '账号状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  login_ip          varchar(128)    default ''                 comment '最后登录IP',
  login_date        datetime                                   comment '最后登录时间',
  pwd_update_date   datetime                                   comment '密码最后更新时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (user_id)
) engine=innodb auto_increment=100 comment = '用户信息表';

-- ----------------------------
-- 初始化-用户信息表数据（admin/admin123）
-- ----------------------------
insert into sys_user values(1,  100, 'admin', '', '系统管理员', '00', 'admin@zhihuiyuanqu.com', '15888888888', '0', '', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '超级管理员');


-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
drop table if exists sys_post;
create table sys_post
(
  post_id       bigint(20)      not null auto_increment    comment '岗位ID',
  post_code     varchar(64)     not null                   comment '岗位编码',
  post_name     varchar(50)     not null                   comment '岗位名称',
  post_sort     int(4)          not null                   comment '显示顺序',
  status        char(1)         not null                   comment '状态（0正常 1停用）',
  create_by     varchar(64)     default ''                 comment '创建者',
  create_time   datetime                                   comment '创建时间',
  update_by     varchar(64)     default ''			       comment '更新者',
  update_time   datetime                                   comment '更新时间',
  remark        varchar(500)    default null               comment '备注',
  primary key (post_id)
) engine=innodb comment = '岗位信息表';

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1, 'admin', '园区管理员', 1, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(2, 'guard', '保安人员',   2, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(3, 'user',  '普通员工',   3, '0', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 4、角色信息表
-- ----------------------------
drop table if exists sys_role;
create table sys_role (
  role_id              bigint(20)      not null auto_increment    comment '角色ID',
  role_name            varchar(30)     not null                   comment '角色名称',
  role_key             varchar(100)    not null                   comment '角色权限字符串',
  role_sort            int(4)          not null                   comment '显示顺序',
  data_scope           char(1)         default '1'                comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  menu_check_strictly  tinyint(1)      default 1                  comment '菜单树选择项是否关联显示',
  dept_check_strictly  tinyint(1)      default 1                  comment '部门树选择项是否关联显示',
  status               char(1)         not null                   comment '角色状态（0正常 1停用）',
  del_flag             char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by            varchar(64)     default ''                 comment '创建者',
  create_time          datetime                                   comment '创建时间',
  update_by            varchar(64)     default ''                 comment '更新者',
  update_time          datetime                                   comment '更新时间',
  remark               varchar(500)    default null               comment '备注',
  primary key (role_id)
) engine=innodb auto_increment=100 comment = '角色信息表';

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values('1', '超级管理员',  'admin',          1, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '超级管理员');
insert into sys_role values('4', '访客管理员',  'visitorAdmin',   4, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '访客审批权限角色');
insert into sys_role values('5', '园区管理员',  'smartparkAdmin', 5, 4, 1, 1, '0', '0', 'admin', sysdate(), '', null, '园区管理员：可见本部门及以下全部访客数据');


-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
drop table if exists sys_menu;
create table sys_menu (
  menu_id           bigint(20)      not null auto_increment    comment '菜单ID',
  menu_name         varchar(50)     not null                   comment '菜单名称',
  parent_id         bigint(20)      default 0                  comment '父菜单ID',
  order_num         int(4)          default 0                  comment '显示顺序',
  path              varchar(200)    default ''                 comment '路由地址',
  component         varchar(255)    default null               comment '组件路径',
  query             varchar(255)    default null               comment '路由参数',
  route_name        varchar(50)     default ''                 comment '路由名称',
  is_frame          int(1)          default 1                  comment '是否为外链（0是 1否）',
  is_cache          int(1)          default 0                  comment '是否缓存（0缓存 1不缓存）',
  menu_type         char(1)         default ''                 comment '菜单类型（M目录 C菜单 F按钮）',
  visible           char(1)         default 0                  comment '菜单状态（0显示 1隐藏）',
  status            char(1)         default 0                  comment '菜单状态（0正常 1停用）',
  perms             varchar(100)    default null               comment '权限标识',
  icon              varchar(100)    default '#'                comment '菜单图标',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default ''                 comment '备注',
  primary key (menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

-- ----------------------------
-- 初始化-菜单信息表数据（仅系统管理 + 访客管理）
-- ----------------------------
-- 一级菜单
insert into sys_menu values('1', '系统管理', '0', '1', 'system', null, '', '', 1, 0, 'M', '0', '0', '', 'system', 'admin', sysdate(), '', null, '系统管理目录');
insert into sys_menu values('2004', '访客管理', '0', '2', 'visitor', null, '', '', 1, 0, 'M', '0', '0', '', 'people', 'admin', sysdate(), '', null, '访客管理目录');
-- 系统管理-二级菜单
insert into sys_menu values('100', '用户管理', '1', '1', 'user', 'system/user/index', '', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', sysdate(), '', null, '用户管理菜单');
insert into sys_menu values('101', '角色管理', '1', '2', 'role', 'system/role/index', '', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', sysdate(), '', null, '角色管理菜单');
insert into sys_menu values('102', '菜单管理', '1', '3', 'menu', 'system/menu/index', '', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', sysdate(), '', null, '菜单管理菜单');
insert into sys_menu values('103', '部门管理', '1', '4', 'dept', 'system/dept/index', '', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', sysdate(), '', null, '部门管理菜单');
-- 访客管理-二级菜单
insert into sys_menu values('2005', '访客预约', '2004', '1', 'appointment', 'visitor/appointment/index', '', '', 1, 0, 'C', '0', '0', 'visitor:appointment:list', 'edit', 'admin', sysdate(), '', null, '访客预约页面');
insert into sys_menu values('2006', '访客审批', '2004', '2', 'approval', 'visitor/approval/index', '', '', 1, 0, 'C', '0', '0', 'visitor:approval:list', 'check', 'admin', sysdate(), '', null, '访客审批页面');
insert into sys_menu values('2007', '现场登记', '2004', '3', 'register', 'visitor/register/index', '', '', 1, 0, 'C', '0', '0', 'visitor:register:add', 'form', 'admin', sysdate(), '', null, '现场访客登记页面');
insert into sys_menu values('2008', '来访记录', '2004', '4', 'log', 'visitor/log/index', '', '', 1, 0, 'C', '0', '0', 'visitor:log:list', 'list', 'admin', sysdate(), '', null, '来访记录页面');
insert into sys_menu values('2009', '部门登记二维码', '2004', '5', 'deptQrcode', 'visitor/qrcode/index', '', '', 1, 0, 'C', '0', '0', 'visitor:qrcode:list', 'qr-code', 'admin', sysdate(), '', null, '部门访客登记二维码页面');
-- 系统管理-用户管理按钮
insert into sys_menu values('1000', '用户查询', '100', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1001', '用户新增', '100', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1002', '用户修改', '100', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1003', '用户删除', '100', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1004', '用户导出', '100', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1005', '用户导入', '100', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1006', '重置密码', '100', '7', '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', sysdate(), '', null, '');
-- 系统管理-角色管理按钮
insert into sys_menu values('1007', '角色查询', '101', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1008', '角色新增', '101', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1009', '角色修改', '101', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1010', '角色删除', '101', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1011', '角色导出', '101', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', sysdate(), '', null, '');
-- 系统管理-菜单管理按钮
insert into sys_menu values('1012', '菜单查询', '102', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1013', '菜单新增', '102', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1014', '菜单修改', '102', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1015', '菜单删除', '102', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', sysdate(), '', null, '');
-- 系统管理-部门管理按钮
insert into sys_menu values('1016', '部门查询', '103', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1017', '部门新增', '103', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1018', '部门修改', '103', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1019', '部门删除', '103', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', sysdate(), '', null, '');
-- 访客管理-访客预约按钮
insert into sys_menu values('2010', '预约查询', '2005', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2011', '预约新增', '2005', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2012', '预约修改', '2005', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2013', '预约删除', '2005', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:remove', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2014', '预约导出', '2005', '5', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2015', '预约取消', '2005', '6', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:cancel', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2016', '完成来访', '2005', '7', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:complete', '#', 'admin', sysdate(), '', null, '');
-- 访客管理-访客审批按钮
insert into sys_menu values('2017', '审批查询', '2006', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:pending', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2018', '审批通过', '2006', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:approve', '#', 'admin', sysdate(), '', null, '');
-- 访客管理-现场登记按钮
insert into sys_menu values('2019', '登记新增', '2007', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:log:add', '#', 'admin', sysdate(), '', null, '');
-- 访客管理-来访记录按钮
insert into sys_menu values('2020', '记录查询', '2008', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:log:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2021', '记录导出', '2008', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:log:export', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2022', '记录离开', '2008', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:log:exit', '#', 'admin', sysdate(), '', null, '');
-- 访客管理-部门登记二维码按钮
insert into sys_menu values('2023', '二维码查询', '2009', '1', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:query', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2024', '二维码新增', '2009', '2', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:add', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2025', '二维码修改', '2009', '3', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:edit', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('2026', '二维码删除', '2009', '4', '', '', '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:remove', '#', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
drop table if exists sys_user_role;
create table sys_user_role (
  user_id   bigint(20) not null comment '用户ID',
  role_id   bigint(20) not null comment '角色ID',
  primary key(user_id, role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('1', '1');


-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table sys_role_menu (
  role_id   bigint(20) not null comment '角色ID',
  menu_id   bigint(20) not null comment '菜单ID',
  primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- 访客管理员(4): 访客管理目录 + 5个子菜单 + 全部按钮
-- 园区管理员(5): 访客管理目录 + 5个子菜单 + 全部按钮 + 系统管理目录
-- ----------------------------
insert into sys_role_menu values ('4', '2004');
insert into sys_role_menu values ('4', '2005');
insert into sys_role_menu values ('4', '2006');
insert into sys_role_menu values ('4', '2007');
insert into sys_role_menu values ('4', '2008');
insert into sys_role_menu values ('4', '2009');
insert into sys_role_menu values ('4', '2010');
insert into sys_role_menu values ('4', '2011');
insert into sys_role_menu values ('4', '2012');
insert into sys_role_menu values ('4', '2013');
insert into sys_role_menu values ('4', '2014');
insert into sys_role_menu values ('4', '2015');
insert into sys_role_menu values ('4', '2016');
insert into sys_role_menu values ('4', '2017');
insert into sys_role_menu values ('4', '2018');
insert into sys_role_menu values ('4', '2019');
insert into sys_role_menu values ('4', '2020');
insert into sys_role_menu values ('4', '2021');
insert into sys_role_menu values ('4', '2022');
insert into sys_role_menu values ('4', '2023');
insert into sys_role_menu values ('4', '2024');
insert into sys_role_menu values ('4', '2025');
insert into sys_role_menu values ('4', '2026');
insert into sys_role_menu values ('5', '1');
insert into sys_role_menu values ('5', '2004');
insert into sys_role_menu values ('5', '2005');
insert into sys_role_menu values ('5', '2006');
insert into sys_role_menu values ('5', '2007');
insert into sys_role_menu values ('5', '2008');
insert into sys_role_menu values ('5', '2009');
insert into sys_role_menu values ('5', '2010');
insert into sys_role_menu values ('5', '2011');
insert into sys_role_menu values ('5', '2012');
insert into sys_role_menu values ('5', '2013');
insert into sys_role_menu values ('5', '2014');
insert into sys_role_menu values ('5', '2015');
insert into sys_role_menu values ('5', '2016');
insert into sys_role_menu values ('5', '2017');
insert into sys_role_menu values ('5', '2018');
insert into sys_role_menu values ('5', '2019');
insert into sys_role_menu values ('5', '2020');
insert into sys_role_menu values ('5', '2021');
insert into sys_role_menu values ('5', '2022');
insert into sys_role_menu values ('5', '2023');
insert into sys_role_menu values ('5', '2024');
insert into sys_role_menu values ('5', '2025');
insert into sys_role_menu values ('5', '2026');


-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
drop table if exists sys_role_dept;
create table sys_role_dept (
  role_id   bigint(20) not null comment '角色ID',
  dept_id   bigint(20) not null comment '部门ID',
  primary key(role_id, dept_id)
) engine=innodb comment = '角色和部门关联表';

-- ----------------------------
-- 初始化-角色和部门关联表数据（园区管理员 data_scope=4，绑定根部门）
-- ----------------------------
insert into sys_role_dept values ('5', '100');


-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
drop table if exists sys_user_post;
create table sys_user_post
(
  user_id   bigint(20) not null comment '用户ID',
  post_id   bigint(20) not null comment '岗位ID',
  primary key (user_id, post_id)
) engine=innodb comment = '用户与岗位关联表';

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------
insert into sys_user_post values ('1', '1');


-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
drop table if exists sys_oper_log;
create table sys_oper_log (
  oper_id           bigint(20)      not null auto_increment    comment '日志主键',
  title             varchar(50)     default ''                 comment '模块标题',
  business_type     int(2)          default 0                  comment '业务类型（0其它 1新增 2修改 3删除）',
  method            varchar(200)    default ''                 comment '方法名称',
  request_method    varchar(10)     default ''                 comment '请求方式',
  operator_type     int(1)          default 0                  comment '操作类别（0其它 1后台用户 2手机端用户）',
  oper_name         varchar(50)     default ''                 comment '操作人员',
  dept_name         varchar(50)     default ''                 comment '部门名称',
  oper_url          varchar(255)    default ''                 comment '请求URL',
  oper_ip           varchar(128)    default ''                 comment '主机地址',
  oper_location     varchar(255)    default ''                 comment '操作地点',
  oper_param        varchar(2000)   default ''                 comment '请求参数',
  json_result       varchar(2000)   default ''                 comment '返回参数',
  status            int(1)          default 0                  comment '操作状态（0正常 1异常）',
  error_msg         varchar(2000)   default ''                 comment '错误消息',
  oper_time         datetime                                   comment '操作时间',
  cost_time         bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_sys_oper_log_bt (business_type),
  key idx_sys_oper_log_s  (status),
  key idx_sys_oper_log_ot (oper_time)
) engine=innodb auto_increment=100 comment = '操作日志记录';


-- ----------------------------
-- 11、字典类型表
-- ----------------------------
drop table if exists sys_dict_type;
create table sys_dict_type
(
  dict_id          bigint(20)      not null auto_increment    comment '字典主键',
  dict_name        varchar(100)    default ''                 comment '字典名称',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_id),
  unique (dict_type)
) engine=innodb auto_increment=100 comment = '字典类型表';

insert into sys_dict_type values(1, '用户性别', 'sys_user_sex',       '0', 'admin', sysdate(), '', null, '用户性别列表');
insert into sys_dict_type values(2, '菜单状态', 'sys_show_hide',      '0', 'admin', sysdate(), '', null, '菜单状态列表');
insert into sys_dict_type values(3, '系统开关', 'sys_normal_disable', '0', 'admin', sysdate(), '', null, '系统开关列表');
insert into sys_dict_type values(4, '系统是否', 'sys_yes_no',         '0', 'admin', sysdate(), '', null, '系统是否列表');
insert into sys_dict_type values(5, '操作类型', 'sys_oper_type',      '0', 'admin', sysdate(), '', null, '操作类型列表');
insert into sys_dict_type values(6, '系统状态', 'sys_common_status',  '0', 'admin', sysdate(), '', null, '登录状态列表');


-- ----------------------------
-- 12、字典数据表
-- ----------------------------
drop table if exists sys_dict_data;
create table sys_dict_data
(
  dict_code        bigint(20)      not null auto_increment    comment '字典编码',
  dict_sort        int(4)          default 0                  comment '字典排序',
  dict_label       varchar(100)    default ''                 comment '字典标签',
  dict_value       varchar(100)    default ''                 comment '字典键值',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  css_class        varchar(100)    default null               comment '样式属性（其他样式扩展）',
  list_class       varchar(100)    default null               comment '表格回显样式',
  is_default       char(1)         default 'N'                comment '是否默认（Y是 N否）',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_code)
) engine=innodb auto_increment=100 comment = '字典数据表';

insert into sys_dict_data values(1,  1,  '男',   '0', 'sys_user_sex',       '',   '',        'Y', '0', 'admin', sysdate(), '', null, '性别男');
insert into sys_dict_data values(2,  2,  '女',   '1', 'sys_user_sex',       '',   '',        'N', '0', 'admin', sysdate(), '', null, '性别女');
insert into sys_dict_data values(3,  3,  '未知', '2', 'sys_user_sex',       '',   '',        'N', '0', 'admin', sysdate(), '', null, '性别未知');
insert into sys_dict_data values(4,  1,  '显示', '0', 'sys_show_hide',      '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '显示菜单');
insert into sys_dict_data values(5,  2,  '隐藏', '1', 'sys_show_hide',      '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '隐藏菜单');
insert into sys_dict_data values(6,  1,  '正常', '0', 'sys_normal_disable', '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(7,  2,  '停用', '1', 'sys_normal_disable', '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');
insert into sys_dict_data values(8,  1,  '是',   'Y', 'sys_yes_no',         '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '系统默认是');
insert into sys_dict_data values(9,  2,  '否',   'N', 'sys_yes_no',         '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '系统默认否');
insert into sys_dict_data values(10, 99, '其他', '0', 'sys_oper_type',      '',   'info',    'N', '0', 'admin', sysdate(), '', null, '其他操作');
insert into sys_dict_data values(11, 1,  '新增', '1', 'sys_oper_type',      '',   'info',    'N', '0', 'admin', sysdate(), '', null, '新增操作');
insert into sys_dict_data values(12, 2,  '修改', '2', 'sys_oper_type',      '',   'info',    'N', '0', 'admin', sysdate(), '', null, '修改操作');
insert into sys_dict_data values(13, 3,  '删除', '3', 'sys_oper_type',      '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '删除操作');
insert into sys_dict_data values(14, 4,  '授权', '4', 'sys_oper_type',      '',   'primary', 'N', '0', 'admin', sysdate(), '', null, '授权操作');
insert into sys_dict_data values(15, 5,  '导出', '5', 'sys_oper_type',      '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '导出操作');
insert into sys_dict_data values(16, 6,  '导入', '6', 'sys_oper_type',      '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '导入操作');
insert into sys_dict_data values(17, 7,  '强退', '7', 'sys_oper_type',      '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '强退操作');
insert into sys_dict_data values(18, 8,  '生成代码', '8', 'sys_oper_type',  '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '生成操作');
insert into sys_dict_data values(19, 9,  '清空数据', '9', 'sys_oper_type',  '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '清空操作');
insert into sys_dict_data values(20, 1,  '成功',   '0', 'sys_common_status', '',   'primary', 'N', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(21, 2,  '失败',   '1', 'sys_common_status', '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
drop table if exists sys_config;
create table sys_config (
  config_id         int(5)          not null auto_increment    comment '参数主键',
  config_name       varchar(100)    default ''                 comment '参数名称',
  config_key        varchar(100)    default ''                 comment '参数键名',
  config_value      varchar(500)    default ''                 comment '参数键值',
  config_type       char(1)         default 'N'                comment '系统内置（Y是 N否）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (config_id)
) engine=innodb auto_increment=100 comment = '参数配置表';

insert into sys_config values(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',               'skin-blue',     'Y', 'admin', sysdate(), '', null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
insert into sys_config values(2, '用户管理-账号初始密码',         'sys.user.initPassword',            '123456',        'Y', 'admin', sysdate(), '', null, '初始化密码 123456' );
insert into sys_config values(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',              'theme-dark',    'Y', 'admin', sysdate(), '', null, '深色主题theme-dark，浅色主题theme-light' );
insert into sys_config values(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',       'true',          'Y', 'admin', sysdate(), '', null, '是否开启验证码功能（true开启，false关闭）');
insert into sys_config values(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',         'false',         'Y', 'admin', sysdate(), '', null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(6, '用户登录-黑名单列表',           'sys.login.blackIPList',            '',              'Y', 'admin', sysdate(), '', null, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
insert into sys_config values(7, '用户管理-初始密码修改策略',     'sys.account.initPasswordModify',   '1',             'Y', 'admin', sysdate(), '', null, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
insert into sys_config values(8, '用户管理-账号密码更新周期',     'sys.account.passwordValidateDays', '0',             'Y', 'admin', sysdate(), '', null, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
insert into sys_config values(9, '用户管理-密码字符范围',         'sys.account.chrtype',              '0',             'Y', 'admin', sysdate(), '', null, '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');
insert into sys_config values(10, '短信API地址', 'sms.api.url', 'http://localhost:8080/api/sms/send', 'Y', 'admin', sysdate(), '', null, '模拟短信API地址');
insert into sys_config values(11, '短信启用开关', 'sms.enabled', 'true', 'Y', 'admin', sysdate(), '', null, '是否启用短信通知');


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
drop table if exists sys_logininfor;
create table sys_logininfor (
  info_id        bigint(20)     not null auto_increment   comment '访问ID',
  user_name      varchar(50)    default ''                comment '用户账号',
  ipaddr         varchar(128)   default ''                comment '登录IP地址',
  login_location varchar(255)   default ''                comment '登录地点',
  browser        varchar(50)    default ''                comment '浏览器类型',
  os             varchar(50)    default ''                comment '操作系统',
  status         char(1)        default '0'               comment '登录状态（0成功 1失败）',
  msg            varchar(255)   default ''                comment '提示消息',
  login_time     datetime                                 comment '访问时间',
  primary key (info_id),
  key idx_sys_logininfor_s  (status),
  key idx_sys_logininfor_lt (login_time)
) engine=innodb auto_increment=100 comment = '系统访问记录';


-- ============================================================
-- 二、访客业务表
-- ============================================================

-- ----------------------------
-- 15、访客预约表
-- ----------------------------
DROP TABLE IF EXISTS `visitor_appointment`;
CREATE TABLE `visitor_appointment` (
  `appointment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `dept_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `visitor_name` varchar(50) NOT NULL COMMENT '访客姓名',
  `visitor_phone` varchar(20) NOT NULL COMMENT '访客电话',
  `visitor_id_card` varchar(20) DEFAULT '' COMMENT '访客身份证号(预留人脸识别)',
  `visitor_company` varchar(100) DEFAULT '' COMMENT '访客单位',
  `visit_reason` varchar(200) DEFAULT '' COMMENT '访问事由',
  `host_name` varchar(50) DEFAULT '' COMMENT '被访人姓名',
  `host_dept` varchar(100) DEFAULT '' COMMENT '被访人部门',
  `host_phone` varchar(20) DEFAULT '' COMMENT '被访人电话',
  `visit_time` datetime DEFAULT NULL COMMENT '计划来访时间',
  `leave_time` datetime DEFAULT NULL COMMENT '实际离开时间',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING:待审批,APPROVED:已通过,REJECTED:已拒绝,CANCELLED:已取消,VISITING:来访中,COMPLETED:已完成)',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_remark` varchar(500) DEFAULT '' COMMENT '审批备注',
  `sms_sent` char(1) DEFAULT 'N' COMMENT '是否发送短信(Y/N)',
  `sms_content` varchar(500) DEFAULT '' COMMENT '短信内容',
  `pass_code` varchar(20) DEFAULT '' COMMENT '通行码',
  `has_car` char(1) DEFAULT '0' COMMENT '是否开车(0=否 1=是)',
  `car_plate` varchar(20) DEFAULT '' COMMENT '车牌号',
  `has_goods` char(1) DEFAULT '0' COMMENT '是否携带物资(0=否 1=是)',
  `goods_desc` varchar(200) DEFAULT '' COMMENT '携带物资说明',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`appointment_id`),
  KEY `idx_visitor_phone` (`visitor_phone`),
  KEY `idx_status` (`status`),
  KEY `idx_visit_time` (`visit_time`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_visitor_appointment_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访客预约表';

-- ----------------------------
-- 16、来访记录表
-- ----------------------------
DROP TABLE IF EXISTS `visitor_log`;
CREATE TABLE `visitor_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `dept_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `appointment_id` bigint DEFAULT NULL COMMENT '关联预约ID(现场登记为空)',
  `visitor_name` varchar(50) NOT NULL COMMENT '访客姓名',
  `visitor_phone` varchar(20) DEFAULT '' COMMENT '访客电话',
  `visitor_id_card` varchar(20) DEFAULT '' COMMENT '访客身份证号',
  `visitor_company` varchar(100) DEFAULT '' COMMENT '访客单位',
  `visit_reason` varchar(200) DEFAULT '' COMMENT '访问事由',
  `host_name` varchar(50) DEFAULT '' COMMENT '被访人姓名',
  `host_dept` varchar(100) DEFAULT '' COMMENT '被访人部门',
  `entry_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '进入时间',
  `exit_time` datetime DEFAULT NULL COMMENT '离开时间',
  `register_type` varchar(20) DEFAULT 'WALKIN' COMMENT '登记类型(APPOINTMENT:预约,WALKIN:现场登记)',
  `pass_code` varchar(20) DEFAULT '' COMMENT '通行码',
  `has_car` char(1) DEFAULT '0' COMMENT '是否开车(0=否 1=是)',
  `car_plate` varchar(20) DEFAULT '' COMMENT '车牌号',
  `has_goods` char(1) DEFAULT '0' COMMENT '是否携带物资(0=否 1=是)',
  `goods_desc` varchar(200) DEFAULT '' COMMENT '携带物资说明',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`log_id`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_visitor_name` (`visitor_name`),
  KEY `idx_entry_time` (`entry_time`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_visitor_log_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='来访记录表';

-- ----------------------------
-- 17、部门访客登记二维码配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS visitor_dept_qrcode (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  dept_id       BIGINT       NOT NULL COMMENT '部门ID',
  register_code VARCHAR(64)  NOT NULL COMMENT '登记码(URL标识,唯一)',
  enabled       CHAR(1)      NOT NULL DEFAULT '1' COMMENT '是否启用(0停用 1启用)',
  create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
  update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
  del_flag      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dept_id (dept_id),
  UNIQUE KEY uk_register_code (register_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门访客登记二维码配置表';


-- ============================================================
-- 三、短信表（访客审批/待审批通知依赖）
-- ============================================================

-- ----------------------------
-- 18、短信渠道表
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms_channel`;
CREATE TABLE `sys_sms_channel` (
  `channel_id`        bigint          NOT NULL AUTO_INCREMENT COMMENT '渠道主键ID',
  `channel_name`      varchar(100)    NOT NULL                COMMENT '渠道名称',
  `channel_type`      varchar(30)     NOT NULL                COMMENT '渠道类型: ALIYUN/TENCENT/API',
  `access_key_id`     varchar(200)    DEFAULT ''              COMMENT '密钥1(AccessKey/AppId)',
  `access_key_secret` varchar(500)    DEFAULT ''              COMMENT '密钥2(加密存储)',
  `endpoint`          varchar(300)    DEFAULT ''              COMMENT '网关/接口地址',
  `api_method`        varchar(10)     DEFAULT 'POST'          COMMENT 'API渠道专用: GET/POST',
  `api_headers`       varchar(2000)   DEFAULT ''              COMMENT 'API渠道专用: 自定义请求头JSON',
  `api_body_template` varchar(2000)   DEFAULT ''              COMMENT 'API渠道专用: 请求体模板',
  `api_success_rule`  varchar(500)    DEFAULT ''              COMMENT 'API渠道专用: 成功判定规则',
  `timeout`           int             DEFAULT 5000            COMMENT '请求超时时间(毫秒)',
  `retry_count`       int             DEFAULT 0               COMMENT '渠道失败重试次数',
  `status`            char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `remark`            varchar(500)    DEFAULT ''              COMMENT '备注',
  `create_by`         varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`       datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`       datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`          char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`channel_id`),
  KEY `idx_channel_type` (`channel_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信渠道配置表';

-- ----------------------------
-- 19、短信签名模板表
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms_sign_template`;
CREATE TABLE `sys_sms_sign_template` (
  `st_id`                   bigint          NOT NULL AUTO_INCREMENT COMMENT '签名模板主键ID',
  `channel_id`              bigint          NOT NULL                COMMENT '关联渠道ID',
  `sign_name`               varchar(100)    NOT NULL                COMMENT '短信签名',
  `template_code`           varchar(100)    NOT NULL                COMMENT '模板CODE/模板ID',
  `template_param_mapping`  varchar(1000)   DEFAULT ''              COMMENT '第三方API参数映射规则JSON',
  `template_desc`           varchar(500)    DEFAULT ''              COMMENT '模板用途描述',
  `status`                  char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `create_by`               varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`             datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`               varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`             datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`                char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`st_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_st_channel` FOREIGN KEY (`channel_id`) REFERENCES `sys_sms_channel` (`channel_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信签名模板表';

-- ----------------------------
-- 20、业务短信绑定表
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms_biz`;
CREATE TABLE `sys_sms_biz` (
  `biz_id`            bigint          NOT NULL AUTO_INCREMENT COMMENT '业务绑定主键ID',
  `biz_code`          varchar(50)     NOT NULL                COMMENT '全局唯一业务编码(调用唯一标识)',
  `biz_name`          varchar(100)    NOT NULL                COMMENT '业务名称',
  `st_id`             bigint          NOT NULL                COMMENT '关联签名模板ID',
  `backup_channel_id` bigint          DEFAULT NULL            COMMENT '备用渠道ID(主渠道故障自动切换)',
  `minute_limit`      int             DEFAULT 5               COMMENT '每分钟发送上限',
  `day_limit`         int             DEFAULT 100             COMMENT '每日发送上限',
  `status`            char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `remark`            varchar(500)    DEFAULT ''              COMMENT '备注',
  `create_by`         varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`       datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`       datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`          char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`biz_id`),
  UNIQUE KEY `uk_biz_code` (`biz_code`),
  KEY `idx_st_id` (`st_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_biz_st` FOREIGN KEY (`st_id`) REFERENCES `sys_sms_sign_template` (`st_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务短信绑定表';

-- ----------------------------
-- 21、短信黑名单表
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms_blacklist`;
CREATE TABLE `sys_sms_blacklist` (
  `phone`       varchar(20)     NOT NULL                COMMENT '黑名单手机号',
  `reason`      varchar(500)    DEFAULT ''              COMMENT '拉黑原因',
  `status`      char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `create_by`   varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time` datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time` datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信黑名单表';

-- ----------------------------
-- 22、短信发送日志表
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms_log`;
CREATE TABLE `sys_sms_log` (
  `log_id`        bigint          NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
  `biz_code`      varchar(50)     NOT NULL                COMMENT '业务编码',
  `channel_type`  varchar(30)     NOT NULL                COMMENT '渠道类型(ALIYUN/TENCENT/API)',
  `phone`         varchar(20)     NOT NULL                COMMENT '手机号码',
  `template_code` varchar(100)    DEFAULT ''              COMMENT '模板ID',
  `sign_name`     varchar(100)    DEFAULT ''              COMMENT '签名',
  `param`         varchar(2000)   DEFAULT ''              COMMENT '模板参数JSON',
  `send_mode`     tinyint         DEFAULT 1               COMMENT '发送模式(1即时 2定时)',
  `send_time`     datetime        DEFAULT NULL            COMMENT '定时发送时间(即时为null)',
  `task_status`   varchar(20)     DEFAULT 'WAIT'          COMMENT '任务状态: WAIT待执行/SUCCESS发送成功/FAIL发送失败/CANCEL已撤销',
  `retry_count`   int             DEFAULT 0               COMMENT '已重试次数',
  `result_msg`    varchar(1000)   DEFAULT ''              COMMENT '返回结果/错误信息',
  `create_by`     varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`        varchar(500)    DEFAULT ''              COMMENT '备注',
  PRIMARY KEY (`log_id`),
  KEY `idx_biz_code` (`biz_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_send_mode` (`send_mode`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信发送日志表(合规审计)';

-- ----------------------------
-- 初始化-短信渠道/模板/业务编码（仅访客相关）
-- ----------------------------
INSERT INTO `sys_sms_channel` (`channel_id`, `channel_name`, `channel_type`, `access_key_id`, `access_key_secret`, `endpoint`, `timeout`, `retry_count`, `status`, `remark`, `create_by`, `create_time`)
VALUES (1, '阿里云短信', 'ALIYUN', 'YOUR_ACCESS_KEY_ID', 'YOUR_ACCESS_KEY_SECRET', 'dysmsapi.aliyuncs.com', 5000, 0, '0', '阿里云短信渠道(占位，请替换真实密钥)', 'admin', NOW());

INSERT INTO `sys_sms_sign_template` (`st_id`, `channel_id`, `sign_name`, `template_code`, `template_desc`, `status`, `create_by`, `create_time`)
VALUES (1, 1, '智慧园区', 'SMS_123456789', '默认短信签名模板', '0', 'admin', NOW());

INSERT INTO `sys_sms_biz` (`biz_code`, `biz_name`, `st_id`, `backup_channel_id`, `minute_limit`, `day_limit`, `status`, `remark`, `create_by`, `create_time`) VALUES
('visitor_approval',      '访客审批通知',    1, NULL, 10, 200, '0', '访客预约审批结果通知', 'admin', NOW()),
('visitor_pending_notify','访客待审批提醒',  1, NULL, 10, 200, '0', '审批人待审批事项提醒', 'admin', NOW());

-- ============================================================
-- 初始化完成
-- ============================================================
