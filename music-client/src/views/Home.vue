<template>
  <!--轮播图-->
  <el-carousel v-if="swiperList.length" class="swiper-container" type="card" height="20vw" :interval="4000">
    <el-carousel-item v-for="(item, index) in swiperList" :key="index">
      <img :src="HttpManager.attachImageUrl(item.pic)" />
    </el-carousel-item>
  </el-carousel>
  <!--猜你喜欢（协同过滤推荐）-->
  <div class="recommend" v-if="recommendSongs.length">
    <div class="recommend-title">猜你喜欢</div>
    <song-list :songList="recommendSongs"></song-list>
  </div>
  <!--热门歌单-->
  <play-list class="play-list-container" title="歌单" path="song-sheet-detail" :playList="songList"></play-list>
  <!--热门歌手-->
  <play-list class="play-list-container" title="歌手" path="singer-detail" :playList="singerList"></play-list>
</template>

<script lang="ts" setup>
import { computed, getCurrentInstance, onMounted, ref } from "vue";

import PlayList from "@/components/PlayList.vue";
import SongList from "@/components/SongList.vue";
import {  NavName } from "@/enums";
import { HttpManager } from "@/api";
import mixin from "@/mixins/mixin";
import { useStore } from "vuex";

const songList = ref([]); // 歌单列表
const singerList = ref([]); // 歌手列表
const swiperList = ref([]);// 轮播图 每次都在进行查询
const recommendSongs = ref([]); // 推荐歌曲
const { changeIndex } = mixin();
const store = useStore();
const token = computed(() => store.getters.token);
const userId = computed(() => store.getters.userId);
const { proxy } = getCurrentInstance() as any;
try {

  HttpManager.getBannerList().then((res) => {
    swiperList.value = (res as ResponseBody).data.sort();
  });

  HttpManager.getSongList().then((res) => {
    songList.value =  (res as ResponseBody).data.sort().slice(0, 10);
    // console.log(songList.value)
  });

  HttpManager.getAllSinger().then((res) => {
    singerList.value = (res as ResponseBody).data.sort().slice(0, 10);
  });

  // 登录后拉取推荐
  if (token.value && userId.value) {
    HttpManager.getRecommendSongs({ consumerId: Number(userId.value), limit: 10 }).then((res) => {
      const body = res as any;
      if (body?.success) {
        // 兼容两种返回：
        // - 推荐命中：RecommendSongVO[] => [{ song, score }, ...]
        // - 冷启动兜底：Song[]
        const list = Array.isArray(body.data) ? body.data : [];
        if (list.length && list[0] && typeof list[0] === "object" && "song" in list[0]) {
          recommendSongs.value = list.map((x: any) => x?.song).filter(Boolean);
        } else {
          recommendSongs.value = list.filter(Boolean);
        }
      }
    }).catch((e) => {
      // ignore
    });
  }

  onMounted(() => {
    changeIndex(NavName.Home);
  });
} catch (error) {
  console.error(error);
}
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";

/*轮播图*/
.swiper-container {
  width: 90%;
  margin: auto;
  padding-top: 20px;
  img {
    width: 100%;
  }
}

.recommend {
  width: 90%;
  margin: 18px auto 0;
}
.recommend-title {
  height: 60px;
  line-height: 60px;
  font-size: 28px;
  font-weight: 500;
  text-align: center;
  color: $color-black;
}

.swiper-container:deep(.el-carousel__indicators.el-carousel__indicators--outside) {
  display: inline-block;
  transform: translateX(30vw);
}

.el-slider__runway {
  background-color: $color-blue;
}
</style>
