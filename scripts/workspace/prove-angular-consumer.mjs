import { createRequire } from 'node:module';
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
const root = process.cwd();
const angularRoot = path.resolve(process.argv[2]);
const baseUrl = process.argv[3];
const evidenceDir = path.resolve(process.argv[4] || 'target/proof');
if (!baseUrl) throw new Error('Usage: node scripts/workspace/prove-angular-consumer.mjs <angular-root> <base-url> [evidence-dir]');
const require = createRequire(path.join(root, '.local/angular-proof/package.json'));
const { build } = require('esbuild');
// Workspace source imports and the harness must share one Angular injector/HttpClient identity.
// Resolve framework dependencies from this proof's lockfile, without changing workspace configs.
const frameworkResolution = { name: 'proof-framework-resolution', setup(builder) {
  builder.onResolve({filter: /^(?:@angular\/|rxjs(?:\/|$))/}, args => {
    if (args.pluginData?.proofFrameworkResolved) return;
    return builder.resolve(args.path, {kind: args.kind, resolveDir: path.join(root, '.local/angular-proof'),
      pluginData: {proofFrameworkResolved: true}});
  });
} };
const source = (await readFile('scripts/workspace/angular-consumer-proof.ts', 'utf8'))
  .replace('OFFICIAL_RUNTIME_PATH', path.join(angularRoot, 'projects/praxis-dynamic-form/src/lib/services/reactive-determination-runtime.service.ts').replaceAll('\\', '/'))
  .replace('OFFICIAL_BULK_RUNTIME_PATH', path.join(angularRoot, 'projects/praxis-core/src/lib/services/bulk-operation-client.service.ts').replaceAll('\\', '/'))
  .replace('OFFICIAL_DISCOVERY_RUNTIME_PATH', path.join(angularRoot, 'projects/praxis-core/src/lib/services/resource-discovery.service.ts').replaceAll('\\', '/'))
  .replace('OFFICIAL_API_TOKEN_PATH', path.join(angularRoot, 'projects/praxis-core/src/lib/tokens/api-url.token.ts').replaceAll('\\', '/'))
  .replace('OFFICIAL_NORMALIZER_PATH', path.join(angularRoot, 'projects/praxis-core/src/lib/services/schema-normalizer.service.ts').replaceAll('\\', '/'))
  .replace('OFFICIAL_QUICK_VIEW_MODEL_PATH', path.join(angularRoot, 'projects/praxis-core/src/lib/models/resource-quick-view.model.ts').replaceAll('\\', '/'))
  .replace('OFFICIAL_SUBMIT_PATH', path.join(angularRoot, 'projects/praxis-dynamic-form/src/lib/utils/prepare-submit-payload.ts').replaceAll('\\', '/'));
const entry = path.join(root, '.local/angular-proof/entry.ts');
const output = path.join(root, '.local/angular-proof/runtime-proof.mjs');
await writeFile(entry, source);
await mkdir(evidenceDir, { recursive: true });
await build({ entryPoints: [entry], outfile: output, bundle: true, format: 'esm', platform: 'node', target: 'es2022', plugins: [frameworkResolution], nodePaths: [path.join(root, '.local/angular-proof/node_modules')], tsconfig: path.join(angularRoot, 'tsconfig.json'), logLevel: 'warning' });
if (process.argv.includes("--build-only")) { console.log("Official consumer bundle compiled"); process.exit(0); }
const result = spawnSync(process.execPath, [output, baseUrl, evidenceDir], { stdio: 'inherit', env: process.env });
process.exitCode = result.status ?? 1;
