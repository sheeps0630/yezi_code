<!-- 我的页：个人信息、收藏夹、家庭菜谱与家庭成员 -->
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addFamilyMember,
  getProfile,
  listFamilyMembers,
  listFamilyRecipes,
  listFavorites,
  removeFamilyMember,
  removeFamilyRecipe,
  removeFavorite,
  setFavoritePinned,
  updateFamilyMember,
  updateProfile,
} from '../api/modules'
import type { FamilyMember, FamilyRecipeItem, FavoriteItem, UserProfile } from '../api/types'

const avatarStorageKey = 'yechijia.local-avatar.v1'
const route = useRoute()
const router = useRouter()
const profileForm = reactive<UserProfile>({ nickname: '', avatarUrl: '', phone: '' })
const localAvatarUrl = ref('')
const avatarFileInput = ref<HTMLInputElement | null>(null)
const favorites = ref<FavoriteItem[]>([])
const familyRecipes = ref<FamilyRecipeItem[]>([])
const familyMembers = ref<FamilyMember[]>([])
const newMemberName = ref('')
const newMemberRelation = ref('')
const newMemberPhone = ref('')
const editingMemberId = ref('')
const avatarError = ref('')
const profileError = ref('')
const favoriteError = ref('')
const recipeError = ref('')
const memberError = ref('')
const toast = ref('')
const displayedAvatar = computed(() =>
  localAvatarUrl.value
  || (profileForm.avatarUrl.includes('example.com') ? '' : profileForm.avatarUrl),
)
const mobilePhonePattern = /^1[3-9]\d{9}$/

/**
 * 加载用户资料、收藏、家庭菜谱和家庭成员。
 */
async function loadAll(): Promise<void> {
  try {
    const profile = await getProfile()
    profileForm.nickname = profile.nickname
    profileForm.avatarUrl = profile.avatarUrl
    profileForm.phone = profile.phone
    favorites.value = await listFavorites()
    familyRecipes.value = await listFamilyRecipes()
    familyMembers.value = await listFamilyMembers()
  } catch (error) {
    profileError.value = error instanceof Error ? error.message : '加载个人信息失败'
  }
}

/**
 * 从浏览器本地存储读取仅在当前浏览器生效的头像。
 */
function loadLocalAvatar(): void {
  try {
    localAvatarUrl.value = window.localStorage.getItem(avatarStorageKey) ?? ''
  } catch {
    avatarError.value = '无法读取本地头像，请检查浏览器存储设置'
  }
}

/**
 * 打开系统图片选择器。
 */
function chooseAvatar(): void {
  avatarFileInput.value?.click()
}

/**
 * 压缩并保存用户选择的头像到当前浏览器。
 *
 * @param event 头像文件选择事件
 */
async function handleAvatarSelection(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  avatarError.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    avatarError.value = '请选择 JPG、PNG 或 WebP 图片'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    avatarError.value = '图片不能超过 5MB，请选择较小的图片'
    return
  }

  try {
    const image = await createImageBitmap(file)
    const canvas = document.createElement('canvas')
    canvas.width = 256
    canvas.height = 256
    const context = canvas.getContext('2d')
    if (!context) throw new Error('浏览器暂不支持图片处理')
    const cropSize = Math.min(image.width, image.height)
    const cropX = (image.width - cropSize) / 2
    const cropY = (image.height - cropSize) / 2
    context.drawImage(image, cropX, cropY, cropSize, cropSize, 0, 0, 256, 256)
    image.close()
    const avatarDataUrl = await new Promise<string>((resolve, reject) => {
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('头像处理失败，请换一张图片重试'))
          return
        }
        const reader = new FileReader()
        reader.onload = () => {
          if (typeof reader.result === 'string') resolve(reader.result)
          else reject(new Error('读取头像失败，请重试'))
        }
        reader.onerror = () => reject(new Error('读取头像失败，请重试'))
        reader.readAsDataURL(blob)
      }, 'image/jpeg', 0.82)
    })
    window.localStorage.setItem(avatarStorageKey, avatarDataUrl)
    localAvatarUrl.value = avatarDataUrl
  } catch (error) {
    avatarError.value = error instanceof Error ? error.message : '保存头像失败，请重试'
  }
}

/**
 * 保存昵称和个人手机号。
 */
async function handleSaveProfile(): Promise<void> {
  try {
    profileError.value = ''
    await updateProfile({ ...profileForm })
    toast.value = '个人信息已保存'
  } catch (error) {
    profileError.value = error instanceof Error ? error.message : '保存失败'
  }
}

/**
 * 打开一道菜的详情页，并记住当前来源页面。
 *
 * @param dishId 菜品标识
 */
function openRecipe(dishId: string): void {
  void router.push({ name: 'recipe-detail', params: { dishId }, query: { from: route.fullPath } })
}

/**
 * 取消一条收藏。
 *
 * @param favorite 收藏条目
 */
async function handleRemoveFavorite(favorite: FavoriteItem): Promise<void> {
  if (!window.confirm(`确定删除收藏“${favorite.title}”吗？此操作无法撤销。`)) return
  try {
    favoriteError.value = ''
    await removeFavorite(favorite.id)
    favorites.value = await listFavorites()
  } catch (error) {
    favoriteError.value = error instanceof Error ? error.message : '取消收藏失败'
  }
}

/**
 * 切换一条收藏的置顶状态。
 *
 * @param favorite 收藏条目
 */
async function handleToggleFavoritePinned(favorite: FavoriteItem): Promise<void> {
  try {
    favoriteError.value = ''
    await setFavoritePinned(favorite.id, !favorite.pinned)
    favorites.value = await listFavorites()
  } catch (error) {
    favoriteError.value = error instanceof Error ? error.message : '更新置顶状态失败'
  }
}

/**
 * 从家庭菜谱移除一道菜。
 *
 * @param item 家庭菜谱条目
 */
async function handleRemoveFamilyRecipe(item: FamilyRecipeItem): Promise<void> {
  try {
    recipeError.value = ''
    await removeFamilyRecipe(item.id)
    familyRecipes.value = await listFamilyRecipes()
  } catch (error) {
    recipeError.value = error instanceof Error ? error.message : '移除家庭菜谱失败'
  }
}

/**
 * 清空家庭成员编辑表单。
 */
function resetMemberForm(): void {
  editingMemberId.value = ''
  newMemberName.value = ''
  newMemberRelation.value = ''
  newMemberPhone.value = ''
}

/**
 * 把现有家庭成员载入编辑表单。
 *
 * @param member 待编辑成员
 */
function editMember(member: FamilyMember): void {
  editingMemberId.value = member.id
  newMemberName.value = member.name
  newMemberRelation.value = member.relation
  newMemberPhone.value = member.phone
  memberError.value = ''
}

/**
 * 新增或更新家庭成员的联系方式。
 */
async function saveMember(): Promise<void> {
  const name = newMemberName.value.trim()
  const phone = newMemberPhone.value.trim()
  if (!name) {
    memberError.value = '请输入成员名称'
    return
  }
  if (!mobilePhonePattern.test(phone)) {
    memberError.value = '请输入有效的 11 位手机号'
    return
  }
  try {
    memberError.value = ''
    if (editingMemberId.value) {
      await updateFamilyMember(editingMemberId.value, name, newMemberRelation.value.trim(), phone)
    } else {
      await addFamilyMember(name, newMemberRelation.value.trim(), phone)
    }
    familyMembers.value = await listFamilyMembers()
    resetMemberForm()
  } catch (error) {
    memberError.value = error instanceof Error ? error.message : '保存家庭成员失败'
  }
}

/**
 * 删除一位家庭成员。
 *
 * @param member 成员对象
 */
async function handleRemoveMember(member: FamilyMember): Promise<void> {
  try {
    memberError.value = ''
    await removeFamilyMember(member.id)
    familyMembers.value = await listFamilyMembers()
    if (editingMemberId.value === member.id) resetMemberForm()
  } catch (error) {
    memberError.value = error instanceof Error ? error.message : '删除成员失败'
  }
}

onMounted(() => {
  loadLocalAvatar()
  void loadAll()
})
</script>

<template>
  <div>
    <h2 class="page-title">我的椰子小家</h2>
    <p class="page-subtitle">把喜欢的味道，留在家里。</p>

    <section class="card profile-card">
      <div class="section-title">个人信息</div>
      <div class="avatar-row">
        <button class="avatar-button" type="button" aria-label="更换头像" @click="chooseAvatar">
          <img v-if="displayedAvatar" :src="displayedAvatar" alt="我的头像" class="profile-avatar" />
          <span v-else class="avatar-placeholder">椰</span>
          <span class="avatar-edit-mark">更换</span>
        </button>
        <div>
          <strong>{{ profileForm.nickname || '椰子' }}</strong>
          <p class="hint">点击头像选择本机图片，仅保存在当前浏览器</p>
        </div>
        <input
          ref="avatarFileInput"
          class="visually-hidden"
          type="file"
          accept="image/png,image/jpeg,image/webp"
          @change="handleAvatarSelection"
        />
      </div>
      <div v-if="avatarError" class="error-tip inline-error">{{ avatarError }}</div>
      <label class="field-label" for="profile-nickname">昵称</label>
      <input id="profile-nickname" v-model="profileForm.nickname" class="input" placeholder="输入昵称" />
      <label class="field-label" for="profile-phone">手机号</label>
      <input
        id="profile-phone"
        v-model="profileForm.phone"
        class="input"
        type="tel"
        inputmode="numeric"
        placeholder="可填写 11 位手机号"
      />
      <div v-if="profileError" class="error-tip inline-error">{{ profileError }}</div>
      <button class="btn" @click="handleSaveProfile">保存资料</button>
    </section>

    <section class="card">
      <div class="section-title">我的收藏</div>
      <div v-if="favoriteError" class="error-tip inline-error">{{ favoriteError }}</div>
      <div v-if="favorites.length === 0" class="hint">还没有收藏，去菜谱页点「加入收藏」吧。</div>
      <div v-for="favorite in favorites" :key="favorite.id" class="list-row saved-item-row">
        <button
          v-if="favorite.targetType === 'dish'"
          class="saved-item-link"
          type="button"
          @click="openRecipe(favorite.targetId)"
        >
          <span class="row-title">{{ favorite.pinned ? '置顶 · ' : '' }}{{ favorite.title }}</span>
          <span class="hint">查看菜谱 →</span>
        </button>
        <div v-else class="saved-item-link">
          <span class="row-title">{{ favorite.pinned ? '置顶 · ' : '' }}{{ favorite.title }}</span>
          <span class="hint">
            {{
              favorite.targetType === 'restaurant'
                ? '门店收藏'
                : favorite.targetType === 'drink-recipe'
                  ? '自制配方收藏'
                  : '饮品收藏'
            }}
          </span>
        </div>
        <div class="member-actions">
          <button class="btn btn-outline compact-button" @click="handleToggleFavoritePinned(favorite)">
            {{ favorite.pinned ? '取消置顶' : '置顶' }}
          </button>
          <button class="btn btn-danger compact-button" @click="handleRemoveFavorite(favorite)">删除</button>
        </div>
      </div>
      <p class="hint">菜品收藏可查看完整菜谱；门店和饮品收藏仅保存名称。</p>
    </section>

    <section class="card">
      <div class="section-title">家庭菜谱</div>
      <div v-if="recipeError" class="error-tip inline-error">{{ recipeError }}</div>
      <div v-if="familyRecipes.length === 0" class="hint">还没有家庭菜谱，在菜谱页点「加入家庭菜谱」吧。</div>
      <div v-for="item in familyRecipes" :key="item.id" class="list-row saved-item-row">
        <button class="saved-item-link" type="button" @click="openRecipe(item.dishId)">
          <span class="row-title">{{ item.dishName }}</span>
          <span class="hint">{{ item.addedBy }} 添加 · 查看菜谱 →</span>
        </button>
        <button class="btn btn-danger compact-button" @click="handleRemoveFamilyRecipe(item)">删除</button>
      </div>
    </section>

    <section class="card">
      <div class="section-title">家庭成员</div>
      <div v-for="member in familyMembers" :key="member.id" class="list-row member-row">
        <div>
          <span class="row-title">{{ member.name }}</span>
          <span class="hint"> · {{ member.relation || '家庭成员' }} · {{ member.phone || '未填写手机号' }}</span>
        </div>
        <div class="member-actions">
          <button class="btn btn-outline compact-button" @click="editMember(member)">修改</button>
          <button class="btn btn-danger compact-button" @click="handleRemoveMember(member)">删除</button>
        </div>
      </div>
      <p class="hint phone-notice">手机号仅作联系方式保存，不会验证或关联其他账号。</p>
      <div class="member-form">
        <div class="section-title member-form-title">{{ editingMemberId ? '修改成员' : '添加成员' }}</div>
        <input v-model="newMemberName" class="input" placeholder="成员名称" aria-label="成员名称" />
        <input v-model="newMemberRelation" class="input" placeholder="关系，如：妈妈" aria-label="成员关系" />
        <input
          v-model="newMemberPhone"
          class="input"
          type="tel"
          inputmode="numeric"
          maxlength="11"
          placeholder="11 位手机号"
          aria-label="成员手机号"
          @keyup.enter="saveMember"
        />
        <div v-if="memberError" class="error-tip inline-error">{{ memberError }}</div>
        <div class="member-form-actions">
          <button class="btn" @click="saveMember">{{ editingMemberId ? '保存修改' : '添加成员' }}</button>
          <button v-if="editingMemberId" class="btn btn-outline" @click="resetMemberForm">取消</button>
        </div>
      </div>
    </section>
  </div>
</template>
