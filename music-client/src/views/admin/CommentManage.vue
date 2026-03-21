<template>
  <div class="admin-page">
    <div class="header">
      <h2>评论管理</h2>
      <div class="tools">
        <el-input v-model="keyword" placeholder="搜索用户名或评论内容" clearable class="tool-input" @keyup.enter="onSearch" />
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <el-table :data="items" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户" width="140" />
      <el-table-column label="来源" width="130">
        <template #default="scope">{{ sourceText(scope.row) }}</template>
      </el-table-column>
      <el-table-column prop="content" label="评论内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="up" label="点赞" width="90" />
      <el-table-column label="时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="scope">
          <el-popconfirm title="确认删除该评论？" @confirm="removeComment(scope.row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { getCurrentInstance, onMounted, ref } from "vue";
import { HttpManager } from "@/api";

const { proxy } = getCurrentInstance() as any;
const loading = ref(false);
const items = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref("");

function formatTime(t: any) {
  if (!t) return "-";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function sourceText(row: any) {
  if (row?.songId) return `歌曲:${row.songId}`;
  if (row?.songListId) return `歌单:${row.songListId}`;
  return "-";
}

function getErrMsg(err: any) {
  return err?.response?.data?.message || err?.message || "请求失败，请稍后重试";
}

async function loadData() {
  loading.value = true;
  try {
    const res = (await HttpManager.adminCommentPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
    })) as any;
    items.value = res?.data?.items || [];
    total.value = res?.data?.total || 0;
  } catch (err: any) {
    items.value = [];
    total.value = 0;
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  pageNum.value = 1;
  loadData();
}

function onPageChange(p: number) {
  pageNum.value = p;
  loadData();
}

function onSizeChange(ps: number) {
  pageSize.value = ps;
  pageNum.value = 1;
  loadData();
}

async function removeComment(id: number) {
  try {
    const res = (await HttpManager.adminDeleteComment(id)) as any;
    proxy?.$message?.({ message: res?.message || "删除成功", type: res?.type || "success" });
    if (res?.success) {
      loadData();
    }
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  }
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.admin-page {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.header {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.tools {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tool-input {
  width: 260px;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
</style>
