import { defineConfig } from 'vitest/config';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import path from 'node:path';

// projekti juurkaust: Kotlini moodul ja andmefailid on seal (vt ../build.gradle.kts)
const juur = path.resolve(import.meta.dirname, '..');

export default defineConfig({
  // GitHub Pages avaldab lehe alamkaustas /eesti-valimissimulaator/
  base: './',
  plugins: [svelte()],
  resolve: {
    alias: {
      '@simulaator': path.join(juur, 'build/dist/js/productionLibrary/eesti-valimissimulaator.mjs'),
      '@andmed': path.join(juur, 'src/jvmMain/resources'),
      '@veebiandmed': path.join(juur, 'build/veebiandmed'),
    },
  },
  server: { fs: { allow: [juur] } },
  worker: { format: 'es' },
  build: { target: 'es2022', chunkSizeWarningLimit: 1500 },
  test: {
    include: ['tests/**/*.test.ts'],
  },
});
