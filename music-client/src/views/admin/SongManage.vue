<template>
  <div class="song-manage">
    <div class="header">
      <h2>歌曲管理（管理员）</h2>
      <el-button type="primary" @click="openCreate">上传歌曲</el-button>
    </div>

    <el-table :data="songList" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="歌曲名" />
      <el-table-column label="歌手" width="180">
        <template #default="scope">
          {{ getSingerNameById(scope.row.singerId) }}
        </template>
      </el-table-column>
      <el-table-column prop="introduction" label="简介" />
      <el-table-column label="操作" width="120" align="center">
        <template #default="scope">
          <el-popconfirm title="确认删除该歌曲？" @confirm="deleteSong(scope.row.id)">
            <template #reference>
              <el-button type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

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
const createVisible = ref(false);
const createForm = reactive({
  name: "",
  singerId: undefined as number | undefined,
  introduction: "",
  lyric: "[00:00:00]暂无歌词",
  songFile: null as File | null,
  lrcFile: null as File | null,
});

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

async function loadData() {
  const [songs, singers] = await Promise.all([
    HttpManager.getAllSong(),
    HttpManager.getAllSinger(),
  ]);
  songList.value = ((songs as any)?.data || []) as any[];
  singerList.value = (((singers as any)?.data || []) as any[]).sort((a, b) => a.id - b.id);
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
      await loadData();
    }
  } catch (e: any) {
    proxy?.$message?.({ message: e?.data?.message || "上传失败", type: "error" });
  }
}

async function deleteSong(id: number) {
  try {
    const res = (await axios.delete(`${getBaseURL()}/song/delete?id=${id}`, { withCredentials: true }))?.data;
    proxy?.$message?.({ message: res?.message || "删除成功", type: res?.type || "success" });
    await loadData();
  } catch (e: any) {
    proxy?.$message?.({ message: e?.data?.message || "删除失败", type: "error" });
  }
}

onMounted(async () => {
  await loadData();
});
</script>

<style scoped lang="scss">
.song-manage {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>

