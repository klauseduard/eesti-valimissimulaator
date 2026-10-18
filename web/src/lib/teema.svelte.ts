// Lehe värviteema (hele või tume) reaktiivse olekuna, et erakondade värvid saaks teema järgi valida. Teema
// tuleb atribuudist data-theme, kui see on määratud, muidu süsteemi eelistusest.

const päring = typeof matchMedia === 'undefined' ? null : matchMedia('(prefers-color-scheme: dark)');

function onTume(): boolean {
  const määratud = typeof document === 'undefined' ? undefined : document.documentElement.dataset.theme;
  if (määratud === 'dark') return true;
  if (määratud === 'light') return false;
  return päring?.matches ?? false;
}

export const teema = $state({ tume: onTume() });

päring?.addEventListener('change', () => (teema.tume = onTume()));
if (typeof MutationObserver !== 'undefined' && typeof document !== 'undefined') {
  new MutationObserver(() => (teema.tume = onTume())).observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-theme'],
  });
}
