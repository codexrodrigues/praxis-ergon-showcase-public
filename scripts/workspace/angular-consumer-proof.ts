// Proof-only consumer: it imports the official runtime; no host formula is implemented here.
import '@angular/compiler';
import { FormControl, FormGroup } from '@angular/forms';
import { normalizeSchemaFields } from '@praxisui/core/schema';
import { Observable } from 'rxjs';
import { ReactiveDeterminationRuntimeService } from 'OFFICIAL_RUNTIME_PATH';
import { BulkOperationClientService } from 'OFFICIAL_BULK_RUNTIME_PATH';
import { ResourceDiscoveryService } from 'OFFICIAL_DISCOVERY_RUNTIME_PATH';
import { HttpClient } from '@angular/common/http';
import { Injector, runInInjectionContext } from '@angular/core';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';

const [baseUrl, evidenceDir] = process.argv.slice(2);
assert(baseUrl && evidenceDir);
const expectedProofBranch = process.env.BULK_PROOF_DISPOSABLE_BRANCH ?? 'br-super-sound-b7urizmd';
assert.match(expectedProofBranch, /^br-[a-z0-9-]+$/u);
assert.equal(process.env.PROOF_NEON_BRANCH, expectedProofBranch, 'Use the explicitly provisioned isolated proof branch');
const auth = 'Basic ' + Buffer.from(process.env.SHOWCASE_USERNAME + ':' + process.env.SHOWCASE_PASSWORD).toString('base64');
async function http(url: string, method = 'GET', body?: unknown, extra: any = {}) {
  const response = await fetch(new URL(url, baseUrl), { method, headers: { Authorization: auth, 'Content-Type': 'application/json', 'X-Tenant': '1', ...extra }, body: body === undefined ? undefined : JSON.stringify(body) });
  const result = response.status === 204 ? null : await response.json();
  if (!response.ok) throw Object.assign(new Error('HTTP ' + response.status), { status: response.status });
  return result?.data ?? result;
}
const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));
const receipt: any = { schemas: [], executions: [], bulkContracts: [], staleResultProtected: false, rejectionVisible: false, fixtureRemoved: false };
// This proof exercises the official contract reader, not its transport methods.
const injector = Injector.create({providers: [
  {provide: HttpClient, useValue: {}}, {provide: ResourceDiscoveryService, useValue: {}}
]});
const bulkClient = runInInjectionContext(injector, () => new BulkOperationClientService());
for (const resource of ['vinculos', 'servidor-pessoas', 'cargos']) {
  const capabilities = await http('/api/ergon/' + resource + '/capabilities');
  for (const operationId of ['bulk-update', 'bulk-update-items']) {
    const contract = bulkClient.fromCapability(capabilities.operations[operationId]);
    assert(contract.editableFields?.writableFields.length, 'Backend must publish writable fields');
    assert(contract.editableFields?.clearableFields.length, 'Backend must publish nullable clearable fields');
    assert(contract.editableFields!.clearableFields.every(field => contract.editableFields!.writableFields.includes(field)));
    assert.equal(contract.evaluationOperation.operation.method, 'POST');
    assert.equal(contract.confirmationOperation.operation.method, 'POST');
    assert.notEqual(contract.evaluationOperation.operation.operationId, contract.confirmationOperation.operation.operationId);
    receipt.bulkContracts.push({ resource, operationId, mode: contract.mode,
      writableFields: contract.editableFields!.writableFields, clearableFields: contract.editableFields!.clearableFields,
      evaluation: contract.evaluationOperation.operation, confirmation: contract.confirmationOperation.operation });
  }
}
let fixtureCreated = false;
try {
  const existing = await fetch(new URL('/api/ergon/cargos/9590', baseUrl), { headers: { Authorization: auth } });
  assert.equal(existing.status, 404, 'Reserved fixture identity must be absent');
  await http('/api/ergon/cargos', 'POST', { cargo: 9590, empCodigo: 1, nome: 'Agregador temporário da prova de consumo', categoria: 'SAUDE', controleVaga: false, eAglutinador: true });
  fixtureCreated = true;
  const selected = (await http('/api/ergon/cargos/101')).id;
  for (const resource of ['servidor-pessoas', 'vinculos', 'cargos']) {
    for (const mode of ['post', 'put']) {
      const schema = JSON.parse(await fs.readFile(path.join(evidenceDir, resource + '-' + mode + '-determinations.json'), 'utf8'));
      const fields = normalizeSchemaFields(schema);
      const definitions = schema['x-ui'].reactiveDeterminations;
      assert.equal(definitions.length, 1);
      const definition = definitions[0];
      assert.equal(definition.scope.formMode, mode === 'post' ? 'create' : 'edit');
      const controls: any = {};
      for (const binding of [...definition.inputs, ...definition.outputs]) {
        const name = binding.fieldPath.slice(1);
        assert(fields.some(f => f.name === name), 'Normalized field missing: ' + name);
        controls[name] = new FormControl(name.startsWith('def') ? false : null);
      }
      const form = new FormGroup(controls);
      const runtime = new ReactiveDeterminationRuntimeService();
      const events: any[] = [];
      const subscription = runtime.connect(form, definitions, (def: any, body: any) => new Observable<any>(subscriber => {
        let active = true;
        http(def.capability.href, 'POST', body).then(async result => {
          // Force an old generation to finish later, after a new snapshot has superseded it.
          await sleep(body.defvisual === true ? 800 : 0);
          if (active) { subscriber.next(result); subscriber.complete(); }
        }).catch(error => { if (active) subscriber.error(error); });
        return () => { active = false; };
      }), event => events.push(event), 5000, fields as any);
      const source = definition.inputs[0].fieldPath.slice(1);
      if (resource === 'servidor-pessoas') {
        form.get('defvisual')!.setValue(true);
        await sleep(400);
        form.get('defvisual')!.setValue(false);
        await sleep(1400);
        assert.equal(form.get('possuiDeficienciaPcDPrevisto')!.value, false);
        receipt.staleResultProtected = true;
      } else {
        form.get(source)!.setValue(resource === 'cargos' ? 9590 : selected);
        await sleep(1000);
        assert.equal(await runtime.waitForStable(form, 5000), true);
        const target = definition.outputs[0].fieldPath.slice(1);
        assert(form.get(target)!.value, 'The backend must fill the draft without prefilled output');
        assert(form.get(target)!.disabled, 'Derived target is owned by the official runtime');
        if (resource === 'vinculos') {
          form.get(source)!.setValue(99999999);
          await sleep(900);
          assert.equal(runtime.hasUnsatisfied(form), true);
          assert(events.some(e => e.status === 'error'));
          receipt.rejectionVisible = true;
        }
      }
      assert(events.some(e => e.status === 'success'), 'Successful runtime application required');
      receipt.schemas.push({ resource, operation: definition.scope.schemaOperationId, normalizedFields: fields.length });
      receipt.executions.push({ resource, mode, statuses: [...new Set(events.map(e => e.status))], provenance: definition.provenance });
      subscription.unsubscribe();
    }
  }
} finally {
  if (fixtureCreated) {
    await http('/api/ergon/cargos/9590', 'DELETE');
    const missing = await fetch(new URL('/api/ergon/cargos/9590', baseUrl), { headers: { Authorization: auth } });
    assert.equal(missing.status, 404);
    receipt.fixtureRemoved = true;
  }
}
await fs.writeFile(path.join(evidenceDir, 'angular-consumer-proof.json'), JSON.stringify(receipt, null, 2));
console.log(JSON.stringify({ schemas: receipt.schemas.length, executions: receipt.executions.length, bulkContracts: receipt.bulkContracts.length, staleResultProtected: receipt.staleResultProtected, rejectionVisible: receipt.rejectionVisible, fixtureRemoved: receipt.fixtureRemoved }));
