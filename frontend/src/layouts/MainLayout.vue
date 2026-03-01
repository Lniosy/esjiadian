<template>
  <el-container>
    <el-header>
      <el-menu mode="horizontal" router>
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/products">商品</el-menu-item>
        <el-menu-item index="/orders">订单</el-menu-item>
        <el-menu-item index="/shop">店铺</el-menu-item>
        <el-menu-item index="/chat">聊天</el-menu-item>
        <el-menu-item index="/notifications">通知</el-menu-item>
        <el-menu-item index="/disputes">纠纷</el-menu-item>
        <el-menu-item index="/admin">管理后台</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>
