# 数据库升级脚本说明

管理后台、数据看板、歌曲/歌单上下架等接口依赖下列表结构。**若接口返回 500**，请对照本文在 MySQL 中执行缺失的脚本（建议在测试库先备份）。

## 建议执行顺序

| 顺序 | 文件 | 说明 |
|------|------|------|
| 1 | `001_add_play_record.sql` | 播放记录表；**看板** `playRecordTotal`、推荐等依赖 |
| 2 | `002_add_note_community.sql` | 社区帖子、评论、点赞表 |
| 3 | `003_add_follow.sql` | 关注关系表 |
| 4 | `004_add_song_status.sql` | 歌曲 `status` 字段；**管理端歌曲**、看板歌曲统计依赖 |
| 5 | `005_add_songlist_status.sql` | 歌单 `status` 字段；**管理端歌单**、看板歌单统计依赖 |
| 6 | `006_add_consumer_admin_fields.sql` | 用户 `status` / `create_time` / `update_time`；**管理端用户列表**、看板用户统计依赖 |
| 7 | `007_add_comment_create_time.sql` | 评论 `create_time`；**管理端评论列表**依赖 |
| 8 | `008_add_admin_pic.sql` | （可选）管理员表增加 `pic` 头像路径，需配合后端实体与上传接口 |

> `006`、`007` 使用 `information_schema` 判断列是否存在，**可重复执行**。  
> `004`、`005` 为 `ADD COLUMN`，若已加过列再执行会报错，可忽略或手动跳过。

## 如何确认是否缺列

在 MySQL 中执行：

```sql
SHOW COLUMNS FROM consumer LIKE 'status';
SHOW COLUMNS FROM consumer LIKE 'create_time';
SHOW COLUMNS FROM song LIKE 'status';
SHOW COLUMNS FROM song_list LIKE 'status';
SHOW COLUMNS FROM comment LIKE 'create_time';
SHOW TABLES LIKE 'play_record';
```

若某条无结果或表不存在，请执行对应脚本。

## 仍 500 时

查看 **Spring Boot 控制台** 或日志中的 SQL 异常（如 `Unknown column 'xxx'`），按提示补列或执行脚本。
