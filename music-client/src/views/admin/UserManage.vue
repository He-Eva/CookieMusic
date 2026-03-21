<template>
  <div class="admin-page">
    <div class="header">
      <h2>用户管理</h2>
      <div class="tools">
        <el-input v-model="keyword" placeholder="按用户名搜索" clearable class="tool-input" @keyup.enter="onSearch" />
        <el-select v-model="statusFilter" clearable placeholder="状态筛选" class="tool-select" @change="onSearch">
          <el-option label="全部" :value="''" />
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
      </div>
    </div>

    <el-table :data="items" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column label="性别" width="90">
        <template #default="scope">{{ sexText(scope.row.sex) }}</template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="phoneNum" label="手机号" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="isEnabled(scope.row.status) ? 'success' : 'danger'">
            {{ isEnabled(scope.row.status) ? "正常" : "禁用" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="scope">
          <el-popconfirm
            :title="isEnabled(scope.row.status) ? '确认禁用该用户？' : '确认解禁该用户？'"
            @confirm="toggleStatus(scope.row)"
          >
            <template #reference>
              <el-button size="small" :type="isEnabled(scope.row.status) ? 'danger' : 'success'">
                {{ isEnabled(scope.row.status) ? "禁用" : "解禁" }}
              </el-button>
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
const statusFilter = ref<any>("");

function sexText(v: any) {
  const s = Number(v);
  if (s === 0) return "女";
  if (s === 1) return "男";
  return "-";
}
function isEnabled(v: any) {
  return Number(v) !== 0;
}

function formatTime(t: any) {
  if (!t) return "-";
  const d = new Date(t);
  if (Number.isNaN(d.getTime())) return String(t);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function getErrMsg(err: any) {
  return err?.response?.data?.message || err?.message || "请求失败，请稍后重试";
}

async function loadData() {
  loading.value = true;
  try {
    const res = (await HttpManager.adminUserPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      status: statusFilter.value === "" ? undefined : statusFilter.value,
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

async function toggleStatus(row: any) {
  const nextStatus = isEnabled(row?.status) ? 0 : 1;
  try {
    const res = (await HttpManager.adminUpdateUserStatus({
      userId: row.id,
      status: nextStatus,
    })) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
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
  width: 200px;
}
.tool-select {
  width: 120px;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
</style>
