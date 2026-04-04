import tailwindcss from "@tailwindcss/vite"
import vue from "@vitejs/plugin-vue"
import { defineConfig } from "vite"
import { viteSingleFile } from "vite-plugin-singlefile"
import vueDevTools from "vite-plugin-vue-devtools"

export default defineConfig({
  plugins: [tailwindcss(), vue(), viteSingleFile(), vueDevTools()],
  build: {
    outDir: "dist",
    emptyOutDir: true,
  },
})
