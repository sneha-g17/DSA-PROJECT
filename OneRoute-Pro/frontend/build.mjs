import { build } from 'esbuild';
import { copyFile, mkdir, readFile, writeFile } from 'node:fs/promises';
await mkdir('../web', { recursive: true });
await build({ entryPoints: ['src/main.jsx'], outfile: '../web/app.js', bundle: true, minify: true, format: 'iife', target: ['es2020'], define: { 'process.env.NODE_ENV': '"production"' }, legalComments: 'inline' });
await copyFile('src/style.css', '../web/style.css');
await copyFile('index.html', '../web/index.html');
const notices = await Promise.all(['react', 'react-dom', 'scheduler'].map(async name => `${name}\n${await readFile(`node_modules/${name}/LICENSE`, 'utf8')}`));
await writeFile('../web/THIRD_PARTY_LICENSES.txt', notices.join('\n\n---\n\n'));
console.log('React production bundle written to ../web; Java serves these assets.');
