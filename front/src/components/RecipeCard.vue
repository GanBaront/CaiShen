<script setup lang="ts">
withDefaults(
  defineProps<{
    name: string
    time?: string
    difficulty?: string
    taste?: string
    match?: string | number
    matchText?: string
    status?: string
    statusType?: 'full' | 'lack'
    showHeart?: boolean
    favorited?: boolean
    showOrder?: boolean
    ordering?: boolean
    variant?: 'home' | 'wall'
    /** AI 推荐理由（大模型结合用户食材生成，仅 AI 排序时出现） */
    reason?: string
    /** 是否为 AI 现场创作的菜谱（站内库无匹配时生成，待人工审核） */
    aiGenerated?: boolean
  }>(),
  {
    variant: 'home',
    statusType: 'full',
    showHeart: false,
    favorited: false,
    showOrder: false,
    ordering: false,
    reason: '',
    aiGenerated: false
  }
)
const emit = defineEmits<{ (e: 'open'): void; (e: 'favorite'): void; (e: 'order'): void }>()
</script>

<template>
  <div class="rec" :class="variant" @click="emit('open')">
    <div class="thumb img-ph">
      <template v-if="variant === 'wall'">
        <span v-if="match !== undefined && match !== ''" class="match-solid">匹配 {{ match }}</span>
        <span v-if="match !== undefined && match !== ''" class="match-soft">{{
          matchText || `匹配度 ${match}`
        }}</span>
      </template>
      <span v-else-if="match !== undefined && match !== ''" class="match">匹配 {{ match }}</span>
      菜品图
      <button v-if="showHeart" class="heart" :class="{ on: favorited }" @click.stop="emit('favorite')">
        {{ favorited ? '♥' : '♡' }}
      </button>
    </div>
    <div class="body">
      <h3>
        {{ name }}
        <span v-if="aiGenerated" class="ai-badge">AI 现场创作</span>
      </h3>
      <div class="meta">
        <template v-if="time">{{ time }}</template>
        <template v-if="difficulty"> · {{ difficulty }}</template>
        <template v-if="taste"> · {{ taste }}</template>
      </div>
      <div v-if="reason" class="reason"><span class="ico">✨</span>{{ reason }}</div>
      <div v-if="status" class="st">
        <span :class="statusType === 'full' ? 'tag-green' : 'tag-amber'">{{ status }}</span>
      </div>
      <button
        v-if="showOrder"
        class="order-btn"
        :disabled="ordering"
        @click.stop="emit('order')"
      >
        {{ ordering ? '下单中…' : '一键下单' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.rec {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--shadow);
  cursor: pointer;
}
.thumb {
  height: 172px;
  border-radius: 12px;
  margin: 8px;
  position: relative;
}
.rec.wall {
  border: 1px solid #eef1ec;
}
.rec.wall .thumb {
  height: 150px;
  border-radius: 15px 15px 0 0;
  margin: 0;
}
.rec.wall .body {
  border-top: 1px solid #f0f3ee;
}
.body {
  padding: 12px 16px 16px;
}
.rec:not(.wall) .body {
  padding: 6px 14px 16px;
}
h3 {
  font-size: 15px;
  margin: 0 0 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.ai-badge {
  font-size: 10px;
  font-weight: 600;
  color: var(--amber);
  background: var(--amber-bg);
  border-radius: 4px;
  padding: 1px 6px;
  flex: none;
}
.meta {
  color: var(--text3);
  font-size: 12px;
}
.reason {
  margin-top: 8px;
  background: var(--chip-bg);
  color: var(--chip-text);
  border-radius: 8px;
  padding: 6px 10px;
  font-size: 12px;
  line-height: 1.5;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}
.reason .ico {
  flex: none;
}
.st {
  margin-top: 10px;
}
.order-btn {
  margin-top: 10px;
  width: 100%;
  height: 30px;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.order-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.match {
  position: absolute;
  left: 10px;
  top: 10px;
  background: var(--chip-bg);
  color: var(--chip-text);
  font-size: 11px;
  border-radius: 6px;
  padding: 3px 8px;
  font-weight: 600;
}
.rec.wall .match-solid,
.rec.wall .match-soft {
  position: absolute;
  top: 10px;
  font-size: 11px;
  border-radius: 999px;
  padding: 2px 9px;
}
.rec.wall .match-solid {
  left: 10px;
  background: var(--chip-bg);
  color: var(--chip-text);
  font-weight: 600;
}
.rec.wall .match-soft {
  left: 82px;
  background: var(--chip-bg);
  color: var(--chip-text);
}
.heart {
  position: absolute;
  right: 10px;
  top: 10px;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  cursor: pointer;
  color: var(--text2);
  font-size: 15px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.heart.on {
  color: var(--danger);
}
</style>
