/// <reference types="svelte" />
/// <reference types="vite/client" />

declare module '@andmed/*?raw' {
  const sisu: string;
  export default sisu;
}

declare module '@veebiandmed/toetused.json' {
  const küsitlused: Record<string, { küsitleja: string; erakonnad: string[]; protsendid: number[][] }>;
  export default küsitlused;
}
