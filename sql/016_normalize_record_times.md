# 016 业务记录时间规范化

## 目标

- 所有业务记录的创建/更新时间落在 **2025-12-01 ~ 2026-05-19**（不晚于答辩日）
- 同一表内按 **主键 id 升序**，时间单调递增（id 小的记录时间更早）

## 涉及表

| 表 | 字段 |
|----|------|
| consumer | create_time, update_time |
| song | create_time, update_time |
| collect | create_time |
| comment | create_time |
| follow | create_time |
| post | create_time, update_time |
| post_comment | create_time |
| post_like | create_time |
| play_record | play_time |

## 不修改

- `singer.birth`：歌手出生日期，非系统录入时间
- `singer_merge_backup.backup_time`：运维备份表

## 执行

```bash
python tools/normalize_record_times.py
```

执行前会自动写入备份表 `backup_record_times_snapshot`，可按 `(tbl, row_id, col_name)` 回滚。

## 修复前问题概览（约）

| 表 | 超范围 create | id 与时间乱序 |
|----|---------------|---------------|
| consumer | 19 | 有 |
| song | 105 | 23 |
| collect | 21 | 31 |
| comment | 56 | 252 |
| follow | 0 | 22 |
| post | 0 | 15 |
| post_comment | 0 | 14 |
| post_like | 0 | 35 |
| play_record | 0 | 32 |

修复后上述问题均为 **0**。
