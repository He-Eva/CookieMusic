/**
 * 全局混入方法（各页面 setup / methods 中调用 mixin()）
 * 核心：playMusic 切歌、playSongById 按 ID 播放、routerManager 跳转、checkStatus 登录校验
 */
import { getCurrentInstance, computed } from "vue";
import { useStore } from "vuex";
import { LocationQueryRaw } from "vue-router";
import { RouterName } from "@/enums";
import { HttpManager } from "@/api";
import axios from 'axios'
interface routerOptions {
  path?: string;
  query?: LocationQueryRaw;
}

export default function () {
  const { proxy } = getCurrentInstance();

  const store = useStore();
  const token = computed(() => store.getters.token);

  function getUserSex(sex) {
    if (sex === 0) {
      return "女";
    } else if (sex === 1) {
      return "男";
    }
  }

  // 获取歌曲名
  function getSongTitle(str) {
    if (!str) return "";
    const parts = String(str)
      .split("-")
      .map((x) => x.trim())
      .filter(Boolean);
    // 兼容旧数据：`歌手-歌名`
    if (parts.length >= 2) {
      return parts.slice(1).join("-").trim();
    }
    // 兼容新数据：仅存歌名
    return String(str).trim();
  }

  // 获取歌手名
  function getSingerName(str) {
    if (!str) return "未知歌手";
    const parts = String(str)
      .split("-")
      .map((x) => x.trim())
      .filter(Boolean);
    // 兼容旧数据：`歌手-歌名`
    if (parts.length >= 2) {
      return parts[0];
    }
    // 兼容新数据：仅存歌名（无歌手信息）
    return "未知歌手";
  }

  // 判断登录状态
  function checkStatus(status?: boolean) {
    // 用户行为（点赞/评论/关注等）必须有有效 userId，避免 consumerId=0 导致脏数据
    const uid = store.getters.userId;
    const hasUser = uid !== null && uid !== undefined && String(uid) !== "" && Number(uid) > 0;
    if (!hasUser) {
      if (status !== false)
        (proxy as any).$message({
          message: "请先登录",
          type: "warning",
        });
      return false;
    }
    return true;
  }

  // 播放
  function playMusic({ id, url, pic, index, name, singerName, lyric, currentSongList }) {
    const songTitle = getSongTitle(name);
    const resolvedSingerName = singerName || getSingerName(name);
    proxy.$store.dispatch("playMusic", {
      id,
      url,
      pic,
      index,
      songTitle,
      singerName: resolvedSingerName,
      lyric,
      currentSongList,
    });
  }

  /** 按歌曲 ID 拉取详情、写入播放器并可选跳转歌词页（社区关联歌曲等） */
  async function playSongById(songId: number | string, goLyric = true) {
    const sid = Number(songId);
    if (!Number.isFinite(sid) || sid <= 0) return false;
    try {
      const res = (await HttpManager.getSongOfId(sid)) as ResponseBody;
      if (!res?.success || !Array.isArray(res.data) || !res.data[0]) {
        (proxy as any)?.$message?.({
          message: res?.message || "歌曲不存在或已下架",
          type: "warning",
        });
        return false;
      }
      const song = res.data[0];
      if (!song.url) {
        (proxy as any)?.$message?.({
          message: "该歌曲暂无音频资源",
          type: "warning",
        });
        return false;
      }
      const currentSongList = [{ ...song, index: 0 }];
      playMusic({
        id: song.id,
        url: song.url,
        pic: song.pic,
        index: 0,
        name: song.name,
        singerName: song.singerName,
        lyric: song.lyric,
        currentSongList,
      });
      store.commit("setIsPlay", true);
      if (goLyric) {
        routerManager(RouterName.Lyric, { path: `${RouterName.Lyric}/${song.id}` });
      }
      return true;
    } catch (e: any) {
      (proxy as any)?.$message?.({
        message: e?.data?.message || "加载歌曲失败",
        type: "error",
      });
      return false;
    }
  }

  function getFileName(path) {
    const parts = path.split('/');
    return parts[parts.length - 1];
  }

  // 下载
  async function downloadMusic({ songUrl, songName }) {
    if (!songUrl) {
      (proxy as any).$message({
        message: "下载链接为空！",
        type: "error",
      });
      console.error("下载链接为空！");
      return;
    }
    console.log("songurl: " + songUrl)
    const fileName = getFileName(songUrl);
    console.log("filename: " + fileName)
    const downUrl="/download/"+fileName
   // const result = (await HttpManager.downloadMusic(downUrl)) as ResponseBody;
   // console.log(result.data);

    // const eleLink = document.createElement("a");
    // eleLink.download = `${fileName}`;
    // eleLink.style.display = "none";
    // // 字符内容转变成 blob 地址
    // const blob = new Blob([result.data]);
    // console.log(blob)
    // eleLink.href = URL.createObjectURL(blob);
    // document.body.appendChild(eleLink); // 触发点击
    // eleLink.click();
    // document.body.removeChild(eleLink); // 移除

      const response = await axios.get(downUrl, {
        responseType: 'blob', // 指定响应类型为二进制数据
      });
      
      // 创建一个Blob URL来下载文件
      const blob = new Blob([response.data], { type: 'application/octet-stream' });
      const url = window.URL.createObjectURL(blob);

      // 创建一个隐藏的<a>标签来下载文件
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();

      // 释放URL对象
      window.URL.revokeObjectURL(url);
      document.body.removeChild(link);

  }

  // 导航索引
  function changeIndex(value) {
    proxy.$store.commit("setActiveNavName", value);
  }
  // 路由管理
  function routerManager(routerName: string | number, options: routerOptions) {
    switch (routerName) {
      case RouterName.Search:
        proxy.$router.push({ path: options.path, query: options.query });
        break;
      case RouterName.Home:
      case RouterName.SongSheet:
      case RouterName.SongSheetDetail:
      case RouterName.Singer:
      case RouterName.SingerDetail:
      case RouterName.Personal:
      case RouterName.PersonalData:
      case RouterName.Setting:
      case RouterName.SignIn:
      case RouterName.SignUp:
      case RouterName.SignOut:
      case RouterName.Lyric:
      case RouterName.Error:
      default:
        proxy.$router.push({ path: options.path });
        break;
    }
  }

  function goBack(step = -1) {
    proxy.$router.go(step);
  }

  return {
    getUserSex,
    getSongTitle,
    getSingerName,
    changeIndex,
    checkStatus,
    playMusic,
    playSongById,
    routerManager,
    goBack,
    downloadMusic,
  };
}
