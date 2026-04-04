import { createApp } from "vue"
import App from "./App.vue"
import { router } from "./router"
import "@formatjs/intl-durationformat/polyfill.js"
import "./style.css"

const app = createApp(App)

app.use(router)
app.mount("#app")
