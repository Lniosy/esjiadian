<template>
  <div class="page-stack">
    <el-card class="page-card">
      <template #header>
        <div class="page-head">
          <h3>我的店铺配置</h3>
          <el-button type="primary" @click="save">保存信息</el-button>
        </div>
      </template>

      <el-form label-width="100px" class="shop-form">
        <el-form-item label="店铺名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="店铺图标">
          <ImageUploader v-model="logoImages" :max="1" :allow-external-url="true" add-label="上传店铺图标" />
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="类目"><el-input v-model="form.categories" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="地区"><el-input v-model="form.region" /></el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="overview" class="page-card">
      <template #header><div class="page-head"><h3>店铺经营概览</h3></div></template>
      <div class="kpi-grid">
        <article class="kpi-item">
          <div class="kpi-label">店铺评分</div>
          <div class="kpi-value">{{ Number(overview.rating || 0).toFixed(2) }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">评价总数</div>
          <div class="kpi-value">{{ overview.evaluationCount }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">在售商品</div>
          <div class="kpi-value">{{ overview.onShelfProductCount }}</div>
        </article>
        <article class="kpi-item">
          <div class="kpi-label">店铺地区</div>
          <div class="kpi-value">{{ overview.shop?.region || '-' }}</div>
        </article>
      </div>
    </el-card>

    <el-card v-if="overview" class="page-card">
      <template #header>
        <div class="page-head">
          <h3>在售商品 <span class="count-tip">（{{ (overview.products || []).length }} 件）</span></h3>
          <el-button type="primary" @click="openProductDialog(null)">＋ 新增商品</el-button>
        </div>
      </template>
      <el-table empty-text="暂无数据" :data="overview.products || []">
        <el-table-column prop="id" label="编号" width="70" />
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" width="90">
          <template #default="scope">¥{{ scope.row.price }}</template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌/型号" width="140">
          <template #default="scope">{{ scope.row.brand }} {{ scope.row.model }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ON_SHELF' ? 'success' : scope.row.status === 'SOLD' ? 'info' : 'warning'" size="small">
              {{ productStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="openProductDialog(scope.row)">编辑</el-button>
            <el-button
              size="small"
              :type="scope.row.status === 'ON_SHELF' ? 'warning' : 'success'"
              :disabled="scope.row.status === 'SOLD'"
              @click="toggleShelf(scope.row)"
            >
              {{ scope.row.status === 'ON_SHELF' ? '下架' : '上架' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

  <!-- 新增/编辑商品弹窗 -->
  <el-dialog
    v-model="productDialog.visible"
    :title="productDialog.editId ? '编辑商品' : '新增商品'"
    width="620px"
    :close-on-click-modal="false"
    @closed="resetProductDialog"
  >
    <el-form :model="productForm" label-width="90px" class="product-form">
      <el-form-item label="商品标题" required>
        <el-input v-model="productForm.title" placeholder="如：海尔双门冰箱 BCD-216STPT" maxlength="60" show-word-limit />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="品牌" required>
            <el-input v-model="productForm.brand" placeholder="如：海尔、美的" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="型号" required>
            <el-input v-model="productForm.model" placeholder="如：BCD-216STPT" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="售价（元）" required>
            <el-input-number v-model="productForm.price" :min="1" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="原价（元）">
            <el-input-number v-model="productForm.originalPrice" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="成色" required>
            <el-select v-model="productForm.conditionLevel" style="width:100%">
              <el-option label="全新" value="NEW" />
              <el-option label="九成新" value="LIKE_NEW" />
              <el-option label="七成新" value="GOOD" />
              <el-option label="一般" value="FAIR" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="功能状态" required>
            <el-select v-model="productForm.functionStatus" style="width:100%">
              <el-option label="功能完好" value="FULLY_FUNCTIONAL" />
              <el-option label="轻微问题" value="MINOR_ISSUE" />
              <el-option label="需要维修" value="NEEDS_REPAIR" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="所在地区" required>
            <el-input v-model="productForm.region" placeholder="如：上海市浦东新区" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="交易方式" required>
            <el-select v-model="productForm.tradeMethods" style="width:100%">
              <el-option label="同城自提" value="同城自提" />
              <el-option label="快递邮寄" value="快递邮寄" />
              <el-option label="均可" value="均可" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="类目" required>
        <el-select v-model="productForm.categoryId" style="width:100%">
          <el-option label="电视" :value="1" />
          <el-option label="冰箱" :value="2" />
          <el-option label="洗衣机" :value="3" />
          <el-option label="空调" :value="4" />
          <el-option label="电脑" :value="5" />
          <el-option label="手机" :value="6" />
          <el-option label="音响" :value="7" />
          <el-option label="其他家电" :value="8" />
        </el-select>
      </el-form-item>
      <el-form-item label="购买日期">
        <el-date-picker v-model="productForm.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择购买日期" style="width:100%" />
      </el-form-item>
      <el-form-item label="维修历史">
        <el-input v-model="productForm.repairHistory" placeholder="如：2023年更换压缩机，无其他维修记录" />
      </el-form-item>
      <el-form-item label="商品描述" required>
        <el-input v-model="productForm.description" type="textarea" :rows="3" placeholder="详细描述商品使用情况、外观状态等…" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="商品图片">
        <ImageUploader v-model="productForm.images" :max="9" :allow-external-url="true" add-label="上传商品图" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="productDialog.visible = false">取消</el-button>
      <el-button type="primary" :loading="productDialog.submitting" @click="submitProduct">
        {{ productDialog.editId ? '保存修改' : '创建并上架' }}
      </el-button>
    </template>
  </el-dialog>

    <el-card v-if="overview" class="page-card">
      <template #header><div class="page-head"><h3>历史评价</h3></div></template>
      <el-table empty-text="暂无数据" :data="overview.evaluations || []">
        <el-table-column prop="orderId" label="订单编号" width="90" />
        <el-table-column prop="productId" label="商品编号" width="90" />
        <el-table-column prop="score" label="评分" width="80" />
        <el-table-column prop="content" label="内容" />
        <el-table-column label="图片" width="160">
          <template #default="scope">
            <span v-if="!(scope.row.images || []).length">-</span>
            <el-tag v-for="(img, idx) in (scope.row.images || [])" :key="idx" class="img-tag">
              <a :href="img" target="_blank">图{{ idx + 1 }}</a>
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="ratingDetail" class="page-card">
      <template #header><div class="page-head"><h3>评分分布与标签</h3></div></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="总评价数">{{ ratingDetail.total }}</el-descriptions-item>
        <el-descriptions-item label="平均分">{{ Number(ratingDetail.rating || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="5星">{{ ratingDetail.distribution?.[5] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="4星">{{ ratingDetail.distribution?.[4] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="3星">{{ ratingDetail.distribution?.[3] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="2星">{{ ratingDetail.distribution?.[2] || 0 }}</el-descriptions-item>
        <el-descriptions-item label="1星">{{ ratingDetail.distribution?.[1] || 0 }}</el-descriptions-item>
      </el-descriptions>
      <div class="rating-bars">
        <div class="bar-row" v-for="item in ratingBars" :key="item.star">
          <span class="star-label">{{ item.star }}星</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: `${item.percent}%` }"></div>
          </div>
          <span class="bar-value">{{ item.count }}</span>
        </div>
      </div>
      <div class="tag-wrap">
        <el-tag v-for="t in (ratingDetail.tagCloud || [])" :key="t.tag" type="info">{{ t.tag }} · {{ t.count }}</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluationApi, productApi, shopApi } from '../../api/modules'
import ImageUploader from '../../components/common/ImageUploader.vue'
import { productStatusText } from '../../utils/display'

const form = reactive({ id: null, name: '', logoUrl: '', intro: '', categories: '', region: '' })
const overview = ref(null)
const ratingDetail = ref(null)
const logoImages = computed({
  get: () => (form.logoUrl ? [form.logoUrl] : []),
  set: (urls) => { form.logoUrl = Array.isArray(urls) && urls[0] ? urls[0] : '' }
})

// ---- 商品新增/编辑对话框 ----
const productDialog = reactive({ visible: false, editId: null, submitting: false })
const defaultProductForm = () => ({
  title: '', categoryId: 1, brand: '', model: '',
  purchaseDate: '', conditionLevel: 'LIKE_NEW', functionStatus: 'FULLY_FUNCTIONAL',
  repairHistory: '', description: '', videoUrl: '',
  price: null, originalPrice: null, region: form.region || '',
  tradeMethods: '均可', images: []
})
const productForm = reactive(defaultProductForm())

const resetProductDialog = () => {
  productDialog.editId = null
  Object.assign(productForm, defaultProductForm())
}

const openProductDialog = (row) => {
  resetProductDialog()
  if (row) {
    productDialog.editId = row.id
    Object.assign(productForm, {
      title: row.title || '',
      categoryId: row.categoryId || 1,
      brand: row.brand || '',
      model: row.model || '',
      purchaseDate: row.purchaseDate || '',
      conditionLevel: row.conditionLevel || 'LIKE_NEW',
      functionStatus: row.functionStatus || 'FULLY_FUNCTIONAL',
      repairHistory: row.repairHistory || '',
      description: row.description || '',
      videoUrl: row.videoUrl || '',
      price: row.price ? Number(row.price) : null,
      originalPrice: row.originalPrice ? Number(row.originalPrice) : null,
      region: row.region || '',
      tradeMethods: row.tradeMethods || '均可',
      images: Array.isArray(row.images) ? [...row.images] : (row.image ? [row.image] : [])
    })
  } else {
    productForm.region = form.region || ''
  }
  productDialog.visible = true
}

const submitProduct = async () => {
  if (!productForm.title || !productForm.brand || !productForm.model || !productForm.price || !productForm.description || !productForm.region) {
    ElMessage.warning('请填写标题、品牌、型号、价格、描述、地区')
    return
  }
  const payload = {
    title: productForm.title,
    categoryId: productForm.categoryId,
    brand: productForm.brand,
    model: productForm.model,
    purchaseDate: productForm.purchaseDate || null,
    conditionLevel: productForm.conditionLevel,
    functionStatus: productForm.functionStatus,
    repairHistory: productForm.repairHistory || null,
    description: productForm.description,
    videoUrl: productForm.videoUrl || null,
    price: productForm.price,
    originalPrice: productForm.originalPrice || null,
    region: productForm.region,
    tradeMethods: productForm.tradeMethods,
    images: productForm.images.filter(Boolean)
  }
  productDialog.submitting = true
  try {
    if (productDialog.editId) {
      await productApi.update(productDialog.editId, payload)
      ElMessage.success('商品已更新')
    } else {
      const newProduct = await productApi.create(payload)
      await productApi.onShelf(newProduct.id).catch(() => {})
      ElMessage.success('商品已创建并上架')
    }
    productDialog.visible = false
    await loadOverview()
  } finally {
    productDialog.submitting = false
  }
}

const toggleShelf = async (row) => {
  try {
    if (row.status === 'ON_SHELF') {
      await productApi.offShelf(row.id)
      ElMessage.success('已下架')
    } else {
      await productApi.onShelf(row.id)
      ElMessage.success('已上架')
    }
    await loadOverview()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

const ratingBars = computed(() => {
  const total = Number(ratingDetail.value?.total || 0)
  return [5, 4, 3, 2, 1].map((star) => {
    const count = Number(ratingDetail.value?.distribution?.[star] || 0)
    return {
      star,
      count,
      percent: total > 0 && count > 0 ? Math.max(4, Math.round((count / total) * 100)) : 0
    }
  })
})

const loadOverview = async () => {
  if (!form.id) return
  overview.value = await shopApi.overview(form.id)
  ratingDetail.value = await evaluationApi.shopRatingDetail(form.id).catch(() => null)
}

const load = async () => {
  const data = await shopApi.myShop()
  if (data) {
    Object.assign(form, data)
    await loadOverview()
  }
}

const save = async () => {
  const data = await shopApi.upsertMyShop({
    name: form.name,
    logoUrl: form.logoUrl,
    intro: form.intro,
    categories: form.categories,
    region: form.region
  })
  Object.assign(form, data)
  ElMessage.success('店铺信息已保存')
  await loadOverview()
}

onMounted(() => {
  load().catch(() => {})
})
</script>

<style scoped>
.shop-form {
  max-width: 920px;
}

.count-tip {
  font-size: 13px;
  font-weight: 400;
  color: #999;
}

.img-tag {
  margin-right: 6px;
}

/* 商品表单 */
.product-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 4px;
}

.tag-wrap {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rating-bars {
  margin-top: 12px;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  padding: 10px 12px;
  background: linear-gradient(180deg, #f8fbff, #ffffff);
  display: grid;
  gap: 8px;
}

.bar-row {
  display: grid;
  grid-template-columns: 40px 1fr 30px;
  align-items: center;
  gap: 8px;
}

.star-label,
.bar-value {
  color: #5f6b7a;
  font-size: 12px;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: #edf2f8;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #26b48a, #0b785e);
}
</style>
