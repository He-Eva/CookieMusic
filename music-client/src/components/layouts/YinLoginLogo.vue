<template>
  <div class="login-logo">
    <div class="login-cover-base" aria-hidden="true" />
    <img
      v-if="!imgGiveUp"
      class="login-cover-img"
      :key="coverSrc"
      :src="coverSrc"
      alt=""
      @error="onImgError"
    />
    <div class="login-cover-mask" aria-hidden="true" />
    <div class="login-brand">
      <yin-icon class="brand-icon" :icon="icon" />
      <div class="brand-texts">
        <span class="brand-name">{{ brandName }}</span>
        <span class="brand-sub">在线音乐社区</span>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from "vue";
import YinIcon from "./YinIcon.vue";
import { Icon } from "@/enums";
import { MUSICNAME } from "@/enums/music-name";
import { HttpManager } from "@/api";

const brandName = MUSICNAME;
const icon = Icon.ERJI;

/** 候选地址顺序：先本地 public（你准备的图），再 MinIO 覆盖图 */
const extTries = ["jpg", "png", "webp", "gif"];
const urlIndex = ref(0);
const imgGiveUp = ref(false);

const candidateUrls = computed(() => {
  const base = process.env.BASE_URL || "/";
  const prefix = base.endsWith("/") ? base : `${base}/`;
  const local = [`${prefix}login-cover.png`, `${prefix}login-cover.jpg`];
  const remote = extTries.map((ext) =>
    HttpManager.attachImageUrl(`user01/site/img/login-cover.${ext}`)
  );
  return [...local, ...remote];
});

const coverSrc = computed(() => {
  const list = candidateUrls.value;
  const i = Math.min(urlIndex.value, Math.max(0, list.length - 1));
  return list[i] || list[0];
});

function onImgError() {
  const list = candidateUrls.value;
  if (urlIndex.value < list.length - 1) {
    urlIndex.value += 1;
  } else {
    imgGiveUp.value = true;
  }
}
</script>

<style lang="scss" scoped>
@import "@/assets/css/var.scss";
@import "@/assets/css/global.scss";

.login-logo {
  position: relative;
  height: calc(100vh - $header-height - $footer-height);
  min-width: 50vw;
  overflow: hidden;
  @include layout(center, center);
}

.login-cover-base {
  position: absolute;
  inset: 0;
  background: linear-gradient(145deg, $color-blue-light 0%, $color-blue-dark 55%, #1a6cad 100%);
}

.login-cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  z-index: 1;
}

.login-cover-mask {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background: linear-gradient(
    115deg,
    rgba(15, 23, 42, 0.55) 0%,
    rgba(15, 23, 42, 0.22) 42%,
    rgba(255, 255, 255, 0.06) 100%
  );
}

.login-brand {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2rem 1.5rem;
  text-align: center;
  color: #fff;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
}

.brand-icon {
  @include icon(6rem, #fff);
  opacity: 0.95;
  filter: drop-shadow(0 4px 16px rgba(0, 0, 0, 0.2));
}

.brand-texts {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.brand-name {
  font-size: 1.85rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.brand-sub {
  font-size: 1rem;
  opacity: 0.92;
  letter-spacing: 0.12em;
}

@media screen and (min-width: $sm) {
  .login-logo {
    width: 50vw;
  }
}

@media screen and (max-width: $sm) {
  .login-logo {
    width: 100vw;
  }
}
</style>
