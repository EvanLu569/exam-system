<template>
  <div class="page">
    <el-row :gutter="16">
      <!-- 成绩趋势 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="chart-card">
          <div class="page-header">
            <span class="page-title">成绩趋势</span>
          </div>
          <el-empty v-if="!scores.length" description="暂无成绩数据，快去参加考试吧" :image-size="80" />
          <svg v-else :viewBox="`0 0 ${lineW} ${lineH}`" class="chart-svg">
            <!-- 网格与 Y 轴刻度 -->
            <g v-for="tick in lineYTicks" :key="tick.v">
              <line :x1="padL" :x2="lineW - padR" :y1="tick.y" :y2="tick.y" stroke="#ebeef5" />
              <text :x="padL - 8" :y="tick.y + 4" text-anchor="end" class="axis-text">{{ tick.v }}</text>
            </g>
            <!-- 折线 -->
            <polyline :points="linePoints" fill="none" stroke="#409eff" stroke-width="2" />
            <!-- 数据点与标签 -->
            <g v-for="(p, i) in lineDots" :key="i">
              <circle :cx="p.x" :cy="p.y" r="4" fill="#409eff" />
              <text :x="p.x" :y="p.y - 8" text-anchor="middle" class="val-text">{{ p.value }}</text>
              <text :x="p.x" :y="lineH - 10" text-anchor="middle" class="axis-text">{{ p.label }}</text>
            </g>
          </svg>
        </el-card>
      </el-col>

      <!-- 知识点掌握度 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="chart-card">
          <div class="page-header">
            <span class="page-title">知识点掌握度</span>
          </div>
          <el-empty v-if="!mastery.length" description="暂无答题数据" :image-size="80" />
          <svg v-else :viewBox="`0 0 ${radarW} ${radarH}`" class="chart-svg">
            <!-- 网格 -->
            <polygon
              v-for="level in [0.25, 0.5, 0.75, 1]"
              :key="level"
              :points="radarGrid(level)"
              fill="none"
              stroke="#ebeef5"
            />
            <!-- 轴线 -->
            <line
              v-for="(m, i) in mastery"
              :key="'axis-' + i"
              :x1="radarCx"
              :y1="radarCy"
              :x2="radarPoint(i, 1).x"
              :y2="radarPoint(i, 1).y"
              stroke="#ebeef5"
            />
            <!-- 数据多边形 -->
            <polygon :points="radarDataPoints" fill="rgba(64,158,255,0.25)" stroke="#409eff" stroke-width="2" />
            <circle v-for="(m, i) in mastery" :key="'dot-' + i" :cx="radarPoint(i, m.masteryRate).x" :cy="radarPoint(i, m.masteryRate).y" r="3" fill="#409eff" />
            <!-- 标签 -->
            <text
              v-for="(m, i) in mastery"
              :key="'label-' + i"
              :x="radarPoint(i, 1.22).x"
              :y="radarPoint(i, 1.22).y"
              text-anchor="middle"
              dominant-baseline="middle"
              class="axis-text"
            >
              {{ m.knowledgePoint }} {{ Math.round(m.masteryRate * 100) }}%
            </text>
          </svg>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyScores, getKnowledgeMastery } from '@/api/stats'

const scores = ref([])
const mastery = ref([])

onMounted(async () => {
  const [s, m] = await Promise.all([getMyScores(), getKnowledgeMastery()])
  scores.value = s || []
  mastery.value = m || []
})

// ---------- 折线图 ----------
const lineW = 640
const lineH = 280
const padL = 44
const padR = 20
const padT = 20
const padB = 36

const lineYTicks = computed(() => {
  const maxV = Math.max(100, ...scores.value.map((s) => s.score))
  const step = Math.ceil(maxV / 4 / 10) * 10 || 10
  const ticks = []
  for (let v = 0; v <= maxV; v += step) {
    ticks.push({ v, y: padT + (lineH - padT - padB) * (1 - v / maxV) })
  }
  return ticks
})

const lineDots = computed(() => {
  const maxV = Math.max(100, ...scores.value.map((s) => s.score))
  const plotW = lineW - padL - padR
  const plotH = lineH - padT - padB
  return scores.value.map((s, i) => {
    const x = scores.value.length === 1 ? padL + plotW / 2 : padL + (i * plotW) / (scores.value.length - 1)
    const y = padT + plotH - (s.score / maxV) * plotH
    return { x, y, value: s.score, label: (s.date || '').slice(5) }
  })
})

const linePoints = computed(() => lineDots.value.map((p) => `${p.x},${p.y}`).join(' '))

// ---------- 雷达图 ----------
const radarW = 360
const radarH = 320
const radarCx = radarW / 2
const radarCy = radarH / 2
const radarR = 105

function radarAngle(i) {
  return -Math.PI / 2 + (i * 2 * Math.PI) / Math.max(mastery.value.length, 1)
}

function radarPoint(i, ratio) {
  const a = radarAngle(i)
  return {
    x: radarCx + Math.cos(a) * radarR * ratio,
    y: radarCy + Math.sin(a) * radarR * ratio
  }
}

function radarGrid(level) {
  return mastery.value
    .map((_, i) => {
      const p = radarPoint(i, level)
      return `${p.x},${p.y}`
    })
    .join(' ')
}

const radarDataPoints = computed(() =>
  mastery.value.map((m, i) => {
    const p = radarPoint(i, Math.min(1, Math.max(0, m.masteryRate)))
    return `${p.x},${p.y}`
  }).join(' ')
)
</script>

<style scoped>
.chart-card {
  margin-bottom: 16px;
}

.chart-svg {
  width: 100%;
  height: auto;
}

.axis-text {
  font-size: 12px;
  fill: #909399;
}

.val-text {
  font-size: 12px;
  fill: #409eff;
  font-weight: 600;
}
</style>
