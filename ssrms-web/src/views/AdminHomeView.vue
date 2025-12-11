<template>
  <!-- initial-page 告诉 UserIndex：默认打开 admin-main -->
  <MainIndex initial-page="admin-main" />
</template>

<script>
import UserIndex from '@/components/UserIndex.vue'

export default {
  name: 'AdminHomeView',
  components: { MainIndex: UserIndex }
}
</script>