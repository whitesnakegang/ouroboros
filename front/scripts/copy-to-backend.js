import { cpSync, existsSync, rmSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

// Get current directory in ES modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const sourceDir = join(__dirname, '..', 'dist');
const targetDir = join(__dirname, '..', '..', 'backend', 'src', 'main', 'resources', 'static', 'ouroboros');

console.log('Copying frontend build to backend...');
console.log('Source:', sourceDir);
console.log('Target:', targetDir);

try {
  // Remove existing target directory if exists
  if (existsSync(targetDir)) {
    console.log('Removing existing target directory...');
    rmSync(targetDir, { recursive: true, force: true });
  }

  // Copy dist folder to backend resources/static/ouroboros
  cpSync(sourceDir, targetDir, { recursive: true });

  console.log('✓ Frontend build copied successfully!');
} catch (error) {
  console.error('Error copying files:', error);
  process.exit(1);
}