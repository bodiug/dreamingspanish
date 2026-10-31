import { execFileSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

test('automatic fullscreen retries are bounded, cancellable and scoped to the selected video', () => {
  const directory = mkdtempSync(join(tmpdir(), 'ds-tv-fullscreen-'));
  const binary = (name) => process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin', name) : name;
  const projectFile = (path) => fileURLToPath(new URL(path, import.meta.url));
  try {
    execFileSync(binary('javac'), ['-d', directory,
      projectFile('../app/src/main/java/com/bodiug/dreamingspanish/AutoFullscreenRequest.java'),
      projectFile('./java/com/bodiug/dreamingspanish/AutoFullscreenRequestTest.java'),
    ]);
    execFileSync(binary('java'), ['-cp', directory, 'com.bodiug.dreamingspanish.AutoFullscreenRequestTest']);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});
