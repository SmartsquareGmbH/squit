import tailwindcss from "@tailwindcss/vite"
import vue from "@vitejs/plugin-vue"
import { viteSingleFile } from "vite-plugin-singlefile"
import vueDevTools from "vite-plugin-vue-devtools"
import { defineConfig } from "vitest/config"

export default defineConfig({
  plugins: [tailwindcss(), vue(), viteSingleFile(), vueDevTools()],
  build: {
    outDir: "dist",
    emptyOutDir: true,
  },
  test: {
    environment: "happy-dom",
    globals: true,
  },
})
