import { createRouter, createWebHashHistory } from "vue-router"
import DetailPage from "./pages/DetailPage.vue"
import MainPage from "./pages/MainPage.vue"

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/", component: MainPage },
    { path: "/detail/:id", component: DetailPage },
  ],
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition ?? { top: 0 }
  },
})
