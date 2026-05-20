# -*- coding: utf-8 -*-
"""
将业务表的创建/更新时间统一到 2025-12-01 ~ 2026-05-19（答辩日前），
并按主键 id 升序单调递增（先创建的 id 更早的时间）。

不修改 singer.birth（歌手出生日期，非记录创建时间）。
"""
from __future__ import print_function
from datetime import datetime, timedelta

import pymysql

START = datetime(2025, 12, 1, 0, 0, 0)
END = datetime(2026, 5, 19, 23, 59, 59)

# (table, id_col, time_cols)  time_cols: create only or (create, update)
TABLES = [
    ("consumer", "id", ("create_time", "update_time")),
    ("song", "id", ("create_time", "update_time")),
    ("collect", "id", ("create_time",)),
    ("comment", "id", ("create_time",)),
    ("follow", "id", ("create_time",)),
    ("post", "id", ("create_time", "update_time")),
    ("post_comment", "id", ("create_time",)),
    ("post_like", "id", ("create_time",)),
    ("play_record", "id", ("play_time",)),
]


def spread_times(count):
    if count <= 0:
        return []
    if count == 1:
        mid = START + (END - START) / 2
        return [mid]
    span = (END - START).total_seconds()
    step = span / (count - 1)
    return [START + timedelta(seconds=step * i) for i in range(count)]


def bump_update_times(create_times, gap_hours=6):
    """update_time 晚于 create_time，且随 id 单调不减。"""
    updates = []
    prev = START
    for ct in create_times:
        ut = ct + timedelta(hours=gap_hours)
        if ut > END:
            ut = END
        if ut < prev:
            ut = min(prev + timedelta(minutes=1), END)
        if ut < ct:
            ut = min(ct + timedelta(minutes=1), END)
        updates.append(ut)
        prev = ut
    return updates


def audit(conn):
    start_s = START.strftime("%Y-%m-%d %H:%M:%S")
    end_s = END.strftime("%Y-%m-%d %H:%M:%S")
    print("=== Audit (range %s ~ %s) ===" % (start_s, end_s))
    with conn.cursor() as cur:
        for table, id_col, cols in TABLES:
            col = cols[0]
            cur.execute(
                "SELECT COUNT(*) FROM `%s` WHERE `%s` < %%s OR `%s` > %%s"
                % (table, col, col),
                (START, END),
            )
            out_range = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM `%s`" % table)
            total = cur.fetchone()[0]
            cur.execute(
                "SELECT COUNT(*) FROM `%s` t WHERE EXISTS ("
                "SELECT 1 FROM `%s` t2 WHERE t2.`%s` < t.`%s` AND t2.`%s` > t.`%s`)"
                % (table, table, id_col, id_col, col, col)
            )
            order_bad = cur.fetchone()[0]
            extra = ""
            if len(cols) > 1:
                uc = cols[1]
                cur.execute(
                    "SELECT COUNT(*) FROM `%s` WHERE `%s` < %%s OR `%s` > %%s"
                    % (table, uc, uc),
                    (START, END),
                )
                out_upd = cur.fetchone()[0]
                extra = ", bad_update=%d" % out_upd
            print("  %s: total=%d, out_of_range(%s)=%d, order_violations=%d%s"
                  % (table, total, col, out_range, order_bad, extra))


def backup(conn):
    with conn.cursor() as cur:
        cur.execute("DROP TABLE IF EXISTS backup_record_times_snapshot")
        cur.execute(
            "CREATE TABLE backup_record_times_snapshot ("
            "  tbl VARCHAR(32) NOT NULL,"
            "  row_id BIGINT NOT NULL,"
            "  col_name VARCHAR(32) NOT NULL,"
            "  old_value DATETIME NULL,"
            "  backup_at DATETIME NOT NULL,"
            "  PRIMARY KEY (tbl, row_id, col_name)"
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
        )
        now = datetime.now()
        for table, id_col, cols in TABLES:
            for col in cols:
                sql = (
                    "INSERT INTO backup_record_times_snapshot "
                    "(tbl, row_id, col_name, old_value, backup_at) "
                    "SELECT %s, `{id_col}`, %s, `{col}`, %s FROM `{table}`"
                ).format(id_col=id_col, col=col, table=table)
                cur.execute(sql, (table, col, now))
    conn.commit()
    print("Backup -> backup_record_times_snapshot")


def normalize(conn, dry_run=False):
    with conn.cursor() as cur:
        for table, id_col, cols in TABLES:
            cur.execute("SELECT `%s` FROM `%s` ORDER BY `%s` ASC" % (id_col, table, id_col))
            ids = [row[0] for row in cur.fetchall()]
            if not ids:
                continue
            create_times = spread_times(len(ids))
            update_times = bump_update_times(create_times) if len(cols) > 1 else None
            for i, row_id in enumerate(ids):
                ct = create_times[i]
                if len(cols) == 1:
                    sql = "UPDATE `%s` SET `%s`=%%s WHERE `%s`=%%s" % (table, cols[0], id_col)
                    params = (ct, row_id)
                else:
                    ut = update_times[i]
                    sql = "UPDATE `%s` SET `%s`=%%s, `%s`=%%s WHERE `%s`=%%s" % (
                        table, cols[0], cols[1], id_col)
                    params = (ct, ut, row_id)
                if dry_run:
                    continue
                cur.execute(sql, params)
            print("  normalized %s: %d rows" % (table, len(ids)))
    if not dry_run:
        conn.commit()


def main():
    import sys
    dry = "--dry-run" in sys.argv
    conn = pymysql.connect(
        host="localhost", port=3306, user="root", password="123456",
        database="tp_music", charset="utf8mb4",
    )
    try:
        audit(conn)
        if dry:
            print("\n(dry-run, no changes)")
            return
        backup(conn)
        print("\n=== Normalizing ===")
        normalize(conn)
        print("\n=== After ===")
        audit(conn)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
