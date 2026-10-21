import { resolve } from 'path';
import { defineConfig } from 'vite';

export default defineConfig({
  base: './',
  publicDir: 'public',
  build: {
    rollupOptions: {
      input: {
        main: resolve(__dirname, 'index.html'),
        omnilab: resolve(__dirname, 'omnilab.html'),
        spatiallab: resolve(__dirname, 'spatiallab.html')
      }
    }
  }
});
