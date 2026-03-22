<template>
  <div class="admin-page">
    <div class="header">
      <h2>歌单管理</h2>
      <div class="tools">
        <el-input v-model="keyword" placeholder="标题或风格" clearable class="tool-input" @keyup.enter="onSearch" />
        <el-select v-model="statusFilter" clearable placeholder="状态" class="tool-select" @change="onSearch">
          <el-option label="全部" :value="''" />
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button type="success" @click="openCreate">新建歌单</el-button>
      </div>
    </div>

    <el-table :data="items" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="style" label="风格" width="100" show-overflow-tooltip />
      <el-table-column label="状态" width="88">
        <template #default="scope">
          <el-tag :type="isOnline(scope.row.status) ? 'success' : 'info'">
            {{ isOnline(scope.row.status) ? "上架" : "下架" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="封面" width="72">
        <template #default="scope">
          <el-image
            v-if="scope.row.pic"
            class="thumb"
            fit="cover"
            :src="HttpManager.attachImageUrl(scope.row.pic)"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" @click="openTracks(scope.row)">曲目</el-button>
          <el-popconfirm
            :title="isOnline(scope.row.status) ? '确认下架该歌单？' : '确认上架？'"
            @confirm="toggleShelf(scope.row)"
          >
            <template #reference>
              <el-button size="small" :type="isOnline(scope.row.status) ? 'warning' : 'success'">
                {{ isOnline(scope.row.status) ? "下架" : "上架" }}
              </el-button>
            </template>
          </el-popconfirm>
          <el-popconfirm title="确认删除歌单及其曲目关联？" @confirm="removeList(scope.row.id)">
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

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="formVisible" :title="isCreate ? '新建歌单' : '编辑歌单'" width="520px" @closed="resetForm">
      <el-form v-loading="formLoading" :model="form" label-width="88px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="歌单标题" />
        </el-form-item>
        <el-form-item label="风格">
          <el-input v-model="form.style" placeholder="如 流行、轻音乐" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.introduction" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSaving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 曲目 -->
    <el-drawer v-model="tracksVisible" :title="`曲目 — ${currentListTitle}`" size="420px">
      <div class="track-tools">
        <el-select
          v-model="pickSongId"
          filterable
          clearable
          placeholder="选择要加入的歌曲"
          style="width: 100%; margin-bottom: 10px"
        >
          <el-option v-for="s in allSongs" :key="s.id" :label="`${s.name} (ID:${s.id})`" :value="s.id" />
        </el-select>
        <el-button type="primary" :disabled="!pickSongId || !currentListId" @click="addTrack">加入歌单</el-button>
      </div>
      <el-table :data="trackRows" v-loading="tracksLoading" size="small" class="track-table">
        <el-table-column prop="songId" label="歌曲ID" width="80" />
        <el-table-column label="歌曲名" min-width="120">
          <template #default="scope">{{ songNameMap.get(scope.row.songId) || "-" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="88" align="center">
          <template #default="scope">
            <el-button size="small" type="danger" link @click="removeTrack(scope.row.songId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, onMounted, reactive, ref } from "vue";
import { HttpManager } from "@/api";

const { proxy } = getCurrentInstance() as any;

const loading = ref(false);
const items = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref("");
const statusFilter = ref<any>("");

const formVisible = ref(false);
const formLoading = ref(false);
const formSaving = ref(false);
const isCreate = ref(true);
const form = reactive({
  id: 0,
  title: "",
  style: "",
  introduction: "",
});

const tracksVisible = ref(false);
const tracksLoading = ref(false);
const currentListId = ref(0);
const currentListTitle = ref("");
const trackRows = ref<any[]>([]);
const pickSongId = ref<number | undefined>(undefined);
const allSongs = ref<any[]>([]);

const songNameMap = computed(() => {
  const m = new Map<number, string>();
  for (const s of allSongs.value) {
    m.set(Number(s.id), s.name);
  }
  return m;
});

function getErrMsg(err: any) {
  return err?.response?.data?.message || err?.message || "请求失败";
}

function isOnline(status: any) {
  return Number(status) !== 0;
}

async function loadList() {
  loading.value = true;
  try {
    const res = (await HttpManager.adminSongListPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      status: statusFilter.value === "" ? undefined : statusFilter.value,
    })) as any;
    items.value = res?.data?.items || [];
    total.value = res?.data?.total || 0;
  } catch (e: any) {
    items.value = [];
    total.value = 0;
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  pageNum.value = 1;
  loadList();
}

function onPageChange(p: number) {
  pageNum.value = p;
  loadList();
}

function onSizeChange(ps: number) {
  pageSize.value = ps;
  pageNum.value = 1;
  loadList();
}

function openCreate() {
  isCreate.value = true;
  resetForm();
  formVisible.value = true;
}

async function openEdit(row: any) {
  isCreate.value = false;
  formVisible.value = true;
  formLoading.value = true;
  try {
    const res = (await HttpManager.adminSongListDetail(row.id)) as any;
    if (res?.success && res?.data) {
      const d = res.data;
      form.id = d.id;
      form.title = d.title || "";
      form.style = d.style || "";
      form.introduction = d.introduction || "";
    } else {
      proxy?.$message?.({ message: res?.message || "加载失败", type: "error" });
    }
  } catch (e: any) {
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  } finally {
    formLoading.value = false;
  }
}

function resetForm() {
  form.id = 0;
  form.title = "";
  form.style = "";
  form.introduction = "";
}

async function submitForm() {
  if (!form.title?.trim()) {
    proxy?.$message?.({ message: "请填写标题", type: "warning" });
    return;
  }
  formSaving.value = true;
  try {
    const payload = {
      id: isCreate.value ? undefined : form.id,
      title: form.title.trim(),
      style: form.style || "",
      introduction: form.introduction || "",
    };
    const res = (await (isCreate.value ? HttpManager.adminSongListAdd(payload) : HttpManager.adminSongListUpdate(payload))) as any;
    proxy?.$message?.({ message: res?.message || "保存成功", type: res?.type || "success" });
    if (res?.success) {
      formVisible.value = false;
      await loadList();
    }
  } catch (e: any) {
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  } finally {
    formSaving.value = false;
  }
}

async function toggleShelf(row: any) {
  const next = isOnline(row.status) ? 0 : 1;
  try {
    const res = (await HttpManager.adminSongListStatus({ songListId: row.id, status: next })) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
    if (res?.success) await loadList();
  } catch (e: any) {
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  }
}

async function removeList(id: number) {
  try {
    const res = (await HttpManager.adminSongListDelete(id)) as any;
    proxy?.$message?.({ message: res?.message || "已删除", type: res?.type || "success" });
    if (res?.success) await loadList();
  } catch (e: any) {
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  }
}

async function loadAllSongs() {
  try {
    const res = (await HttpManager.getAllSong()) as any;
    allSongs.value = (res?.data || []) as any[];
  } catch {
    allSongs.value = [];
  }
}

async function openTracks(row: any) {
  currentListId.value = row.id;
  currentListTitle.value = row.title || `ID ${row.id}`;
  pickSongId.value = undefined;
  tracksVisible.value = true;
  await loadAllSongs();
  await loadTracks();
}

async function loadTracks() {
  if (!currentListId.value) return;
  tracksLoading.value = true;
  try {
    const res = (await HttpManager.adminSongListSongs(currentListId.value)) as any;
    trackRows.value = (res?.data || []) as any[];
  } catch (e: any) {
    trackRows.value = [];
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  } finally {
    tracksLoading.value = false;
  }
}

async function addTrack() {
  if (!currentListId.value || !pickSongId.value) return;
  try {
    const res = (await HttpManager.adminSongListSongAdd({
      songListId: currentListId.value,
      songId: pickSongId.value,
    })) as any;
    proxy?.$message?.({ message: res?.message || "已添加", type: res?.type || "success" });
    if (res?.success) {
      pickSongId.value = undefined;
      await loadTracks();
    }
  } catch (e: any) {
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  }
}

async function removeTrack(songId: number) {
  if (!currentListId.value) return;
  try {
    const res = (await HttpManager.adminSongListSongRemove({
      songListId: currentListId.value,
      songId,
    })) as any;
    proxy?.$message?.({ message: res?.message || "已移除", type: res?.type || "success" });
    if (res?.success) await loadTracks();
  } catch (e: any) {
    proxy?.$message?.({ message: getErrMsg(e), type: "error" });
  }
}

onMounted(() => {
  loadList();
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
  flex-wrap: wrap;
  gap: 10px;
}
.tools {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tool-input {
  width: 180px;
}
.tool-select {
  width: 110px;
}
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: center;
}
.thumb {
  width: 44px;
  height: 44px;
  border-radius: 4px;
}
.track-tools {
  margin-bottom: 12px;
}
.track-table {
  margin-top: 8px;
}
</style>
