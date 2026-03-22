<template>
  <div class="song-manage">
    <div class="header">
      <h2>歌曲管理（管理员）</h2>
      <div class="tools">
        <el-input v-model="keyword" placeholder="歌名或歌手名" clearable class="tool-input" @keyup.enter="onSearch" />
        <el-select v-model="statusFilter" clearable placeholder="上架状态" class="tool-select" @change="onSearch">
          <el-option label="全部" :value="''" />
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="onSearch">查询</el-button>
        <el-button type="success" @click="openCreate">上传歌曲</el-button>
      </div>
    </div>

    <el-table :data="songList" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="name" label="歌曲名" min-width="140" show-overflow-tooltip />
      <el-table-column label="歌手" width="120">
        <template #default="scope">
          {{ getSingerNameById(scope.row.singerId) }}
        </template>
      </el-table-column>
      <el-table-column prop="introduction" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="88">
        <template #default="scope">
          <el-tag :type="isOnline(scope.row.status) ? 'success' : 'info'">
            {{ isOnline(scope.row.status) ? "上架" : "下架" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-popconfirm
            :title="isOnline(scope.row.status) ? '确认下架该歌曲？前台将不可播放' : '确认重新上架？'"
            @confirm="toggleShelf(scope.row)"
          >
            <template #reference>
              <el-button size="small" :type="isOnline(scope.row.status) ? 'warning' : 'success'">
                {{ isOnline(scope.row.status) ? "下架" : "上架" }}
              </el-button>
            </template>
          </el-popconfirm>
          <el-popconfirm title="确认删除该歌曲？" @confirm="deleteSong(scope.row.id)">
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
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

    <el-dialog v-model="createVisible" title="上传歌曲" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="歌曲名">
          <el-input v-model="createForm.name" placeholder="请输入歌曲名" />
        </el-form-item>
        <el-form-item label="歌手">
          <el-select v-model="createForm.singerId" placeholder="请选择歌手" style="width: 100%">
            <el-option v-for="s in singerList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="createForm.introduction" placeholder="可选" />
        </el-form-item>
        <el-form-item label="歌词内容">
          <el-input
            v-model="createForm.lyric"
            type="textarea"
            :rows="4"
            placeholder="可选；若无歌词可填 [00:00:00]暂无歌词"
          />
        </el-form-item>
        <el-form-item label="音频文件">
          <input type="file" accept=".mp3,.m4a,.wav" @change="onSongFileChange" />
        </el-form-item>
        <el-form-item label="歌词文件">
          <input type="file" accept=".lrc,.txt" @change="onLrcFileChange" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确认上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑歌曲信息" width="560px" @closed="resetEdit">
      <el-form v-loading="editLoading" :model="editForm" label-width="90px">
        <el-form-item label="歌曲名">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.introduction" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="歌词">
          <el-input v-model="editForm.lyric" type="textarea" :rows="8" placeholder="支持直接粘贴 LRC 文本" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { getCurrentInstance, onMounted, reactive, ref } from "vue";
import axios from "axios";
import { HttpManager } from "@/api";
import { getBaseURL } from "@/api/request";

const { proxy } = getCurrentInstance() as any;

const songList = ref<any[]>([]);
const singerList = ref<any[]>([]);
const loading = ref(false);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const keyword = ref("");
const statusFilter = ref<any>("");

const createVisible = ref(false);
const createForm = reactive({
  name: "",
  singerId: undefined as number | undefined,
  introduction: "",
  lyric: "[00:00:00]暂无歌词",
  songFile: null as File | null,
  lrcFile: null as File | null,
});

const editVisible = ref(false);
const editLoading = ref(false);
const editSaving = ref(false);
const editForm = reactive({
  id: 0,
  name: "",
  introduction: "",
  lyric: "",
});

function getErrMsg(err: any) {
  return err?.response?.data?.message || err?.message || "请求失败";
}

function isOnline(status: any) {
  return Number(status) !== 0;
}

function getSingerNameById(id: number) {
  const s = singerList.value.find((x) => Number(x.id) === Number(id));
  return s?.name || `ID:${id}`;
}

function onSongFileChange(e: Event) {
  const input = e.target as HTMLInputElement;
  createForm.songFile = input?.files?.[0] || null;
}

function onLrcFileChange(e: Event) {
  const input = e.target as HTMLInputElement;
  createForm.lrcFile = input?.files?.[0] || null;
}

function openCreate() {
  createVisible.value = true;
}

async function loadSingers() {
  try {
    const singers = await HttpManager.getAllSinger();
    singerList.value = (((singers as any)?.data || []) as any[]).sort((a, b) => a.id - b.id);
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  }
}

async function loadSongs() {
  loading.value = true;
  try {
    const res = (await HttpManager.adminSongPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      status: statusFilter.value === "" ? undefined : statusFilter.value,
    })) as any;
    songList.value = res?.data?.items || [];
    total.value = res?.data?.total || 0;
  } catch (err: any) {
    songList.value = [];
    total.value = 0;
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  pageNum.value = 1;
  loadSongs();
}

function onPageChange(p: number) {
  pageNum.value = p;
  loadSongs();
}

function onSizeChange(ps: number) {
  pageSize.value = ps;
  pageNum.value = 1;
  loadSongs();
}

async function submitCreate() {
  if (!createForm.name || !createForm.singerId || !createForm.songFile || !createForm.lrcFile) {
    proxy?.$message?.({ message: "请填写必填项并选择音频/歌词文件", type: "warning" });
    return;
  }
  try {
    const formData = new FormData();
    formData.append("name", createForm.name);
    formData.append("singerId", String(createForm.singerId));
    formData.append("introduction", createForm.introduction || "");
    formData.append("lyric", createForm.lyric || "[00:00:00]暂无歌词");
    formData.append("file", createForm.songFile);
    formData.append("lrcfile", createForm.lrcFile);

    const res = await axios.post(`${getBaseURL()}/song/add`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
      withCredentials: true,
    });
    const body = res?.data;
    proxy?.$message?.({ message: body?.message || "上传成功", type: body?.type || "success" });
    if (body?.success) {
      createVisible.value = false;
      createForm.name = "";
      createForm.singerId = undefined;
      createForm.introduction = "";
      createForm.lyric = "[00:00:00]暂无歌词";
      createForm.songFile = null;
      createForm.lrcFile = null;
      await loadSongs();
    }
  } catch (e: any) {
    proxy?.$message?.({ message: e?.response?.data?.message || "上传失败", type: "error" });
  }
}

async function deleteSong(id: number) {
  try {
    const res = (await axios.delete(`${getBaseURL()}/song/delete?id=${id}`, { withCredentials: true }))?.data;
    proxy?.$message?.({ message: res?.message || "删除成功", type: res?.type || "success" });
    await loadSongs();
  } catch (e: any) {
    proxy?.$message?.({ message: e?.response?.data?.message || "删除失败", type: "error" });
  }
}

async function openEdit(row: any) {
  editVisible.value = true;
  editLoading.value = true;
  try {
    const res = (await HttpManager.adminSongDetail(row.id)) as any;
    if (res?.success && res?.data) {
      const d = res.data;
      editForm.id = d.id;
      editForm.name = d.name || "";
      editForm.introduction = d.introduction || "";
      editForm.lyric = d.lyric || "";
    } else {
      proxy?.$message?.({ message: res?.message || "加载失败", type: "error" });
    }
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    editLoading.value = false;
  }
}

function resetEdit() {
  editForm.id = 0;
  editForm.name = "";
  editForm.introduction = "";
  editForm.lyric = "";
}

async function submitEdit() {
  if (!editForm.id) return;
  editSaving.value = true;
  try {
    const res = (await HttpManager.adminSongUpdate({
      id: editForm.id,
      name: editForm.name,
      introduction: editForm.introduction,
      lyric: editForm.lyric,
    })) as any;
    proxy?.$message?.({ message: res?.message || "保存成功", type: res?.type || "success" });
    if (res?.success) {
      editVisible.value = false;
      await loadSongs();
    }
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  } finally {
    editSaving.value = false;
  }
}

async function toggleShelf(row: any) {
  const next = isOnline(row.status) ? 0 : 1;
  try {
    const res = (await HttpManager.adminSongStatus({ songId: row.id, status: next })) as any;
    proxy?.$message?.({ message: res?.message || "操作完成", type: res?.type || "success" });
    if (res?.success) await loadSongs();
  } catch (err: any) {
    proxy?.$message?.({ message: getErrMsg(err), type: "error" });
  }
}

onMounted(async () => {
  await loadSingers();
  await loadSongs();
});
</script>

<style scoped lang="scss">
.song-manage {
  padding: 0;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
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
