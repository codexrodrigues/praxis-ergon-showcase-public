// Proof-only consumer: it imports the official runtime; no host formula is implemented here.
import '@angular/compiler';
import { FormControl, FormGroup } from '@angular/forms';
import { normalizeSchemaFields } from '@praxisui/core/schema';
import { Observable, firstValueFrom } from 'rxjs';
import { ReactiveDeterminationRuntimeService } from 'OFFICIAL_RUNTIME_PATH';
import { BulkOperationClientService } from 'OFFICIAL_BULK_RUNTIME_PATH';
import { ResourceDiscoveryService } from 'OFFICIAL_DISCOVERY_RUNTIME_PATH';
import { HttpClient, HttpResponse, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { API_URL } from 'OFFICIAL_API_TOKEN_PATH';
import { SchemaNormalizerService } from 'OFFICIAL_NORMALIZER_PATH';
import { resolveFieldQuickViewBinding, resolveQuickViewTargetFromRecord } from 'OFFICIAL_QUICK_VIEW_MODEL_PATH';
import { prepareSubmitPayload } from 'OFFICIAL_SUBMIT_PATH';
import { Injector, runInInjectionContext } from '@angular/core';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';

const [baseUrl, evidenceDir] = process.argv.slice(2);
assert(baseUrl && evidenceDir);
const expectedProofBranch = process.env.BULK_PROOF_DISPOSABLE_BRANCH;
assert(expectedProofBranch, 'An explicitly provisioned disposable branch is mandatory');
assert.notEqual(expectedProofBranch, 'br-super-sound-b7urizmd');
assert.match(expectedProofBranch, /^br-[a-z0-9-]+$/u);
assert.equal(process.env.PROOF_NEON_BRANCH, expectedProofBranch, 'Use the explicitly provisioned isolated proof branch');
assert(process.env.BULK_PROOF_EXPECTED_HOST, 'A verified disposable database endpoint is mandatory');
assert.equal(new URL(process.env.SPRING_DATASOURCE_URL!.replace(/^jdbc:/u, '')).hostname,
  process.env.BULK_PROOF_EXPECTED_HOST);
const auth = 'Basic ' + Buffer.from(process.env.SHOWCASE_USERNAME + ':' + process.env.SHOWCASE_PASSWORD).toString('base64');
async function http(url: string, method = 'GET', body?: unknown, extra: any = {}) {
  const response = await fetch(new URL(url, baseUrl), { method, headers: { Authorization: auth, 'Content-Type': 'application/json', 'X-Tenant': '1', ...extra }, body: body === undefined ? undefined : JSON.stringify(body) });
  const result = response.status === 204 ? null : await response.json();
  if (!response.ok) throw Object.assign(new Error('HTTP ' + response.status), { status: response.status });
  return result?.data ?? result;
}
const sleep = (ms: number) => new Promise(r => setTimeout(r, ms));
const receipt: any = { completed: false, schemas: [], executions: [], bulkContracts: [], quickViewContracts: [], corsCorrelationHeaderVerified: false, staleResultProtected: false, rejectionVisible: false, fixtureRemoved: false };
// Real Angular HttpClient and Discovery; only the Node network backend is adapted to fetch.
const transport = new HttpClient({ handle(request: any) {
  return new Observable<any>(subscriber => {
    const abort = new AbortController();
    const url = new URL(request.urlWithParams, baseUrl);
    assert.equal(url.origin, new URL(baseUrl).origin);
    const headers: any = { Authorization: auth, 'Content-Type': 'application/json', 'X-Tenant': '1' };
    for (const key of request.headers.keys()) headers[key] = request.headers.get(key);
    fetch(url, { method: request.method, headers, signal: abort.signal,
      body: request.body == null ? undefined : JSON.stringify(request.body) }).then(async response => {
      const body = response.status === 204 ? null : await response.json();
      if (!response.ok) throw new HttpErrorResponse({ status: response.status, error: body, url: url.href });
      subscriber.next(new HttpResponse({ body, status: response.status, url: url.href,
        headers: new HttpHeaders(Object.fromEntries(response.headers.entries())) }));
      subscriber.complete();
    }).catch(error => subscriber.error(error));
    return () => abort.abort();
  });
} });
const injector = Injector.create({providers: [
  {provide: HttpClient, useValue: transport},
  {provide: API_URL, useValue: {default: {baseUrl}}},
  {provide: SchemaNormalizerService, useFactory: () => new SchemaNormalizerService()},
  {provide: ResourceDiscoveryService, useFactory: () => new ResourceDiscoveryService()}
]});
const bulkClient = runInInjectionContext(injector, () => new BulkOperationClientService());
const discovery = runInInjectionContext(injector, () => new ResourceDiscoveryService());
const quickViewTargets: Array<{ resource: string; sampleId: string; expectIntents: boolean; expectRelatedChildren: boolean }> = [
  { resource: 'vinculos', sampleId: '1001:1', expectIntents: true, expectRelatedChildren: true },
  { resource: 'servidor-pessoas', sampleId: '1001', expectIntents: true, expectRelatedChildren: true },
  { resource: 'cargos', sampleId: '101', expectIntents: true, expectRelatedChildren: false },
  { resource: 'averbacoes-tempo-servico', sampleId: '1', expectIntents: true, expectRelatedChildren: true },
];
for (const target of quickViewTargets) {
  const responseSchema = await http('/schemas/filtered?' + new URLSearchParams({
    path: '/api/ergon/' + target.resource + '/all', operation: 'get', schemaType: 'response'
  }));
  const identity = normalizeResourceIdentityConfig(
    responseSchema['x-ui']?.resource?.identity ?? responseSchema['x-ui']?.resourceIdentity
  );
  assert(identity?.keyField && identity?.titleField && identity?.subtitleField,
    'Resource ' + target.resource + ' must publish keyField, titleField, and subtitleField');
  assert((identity.metadataFields?.length ?? 0) >= 2,
    'Resource ' + target.resource + ' must publish at least 2 executive metadataFields');
  const headerFields = new Set([identity.keyField, identity.titleField, identity.subtitleField, identity.avatarField].filter(Boolean));
  for (const metaField of identity.metadataFields ?? []) {
    assert(!headerFields.has(metaField), 'metadataFields must not repeat header identity field: ' + metaField);
  }
  const responseFields = normalizeSchemaFields(responseSchema);
  const quickViewFields = responseFields.filter((f: any) => f.quickViewVisible === true);
  assert(quickViewFields.length >= 4 && quickViewFields.length <= 8,
    'Resource ' + target.resource + ' must curate 4..8 quickViewVisible fields');
  const reservedFields = new Set([...headerFields, ...(identity.metadataFields ?? [])]);
  for (const qvField of quickViewFields) {
    assert(!reservedFields.has(qvField.name),
      'quickViewVisible field ' + qvField.name + ' must not overlap with ResourceIdentity');
  }
  const titleField = responseFields.find((f: any) => f.name === identity.titleField);
  assert(titleField, 'Normalized titleField must exist for ' + target.resource);
  const titleBinding = resolveFieldQuickViewBinding(titleField as any);
  assert(titleBinding?.enabled && titleBinding?.previewSurfaceId,
    'titleField ' + identity.titleField + ' must resolve an enabled QuickView binding');
  if (target.resource === 'vinculos') {
    const numfuncField = responseFields.find((f: any) => f.name === 'numfunc');
    assert(numfuncField, 'vinculos response schema must expose numfunc');
    const numfuncBinding = resolveFieldQuickViewBinding(numfuncField as any);
    assert.equal(numfuncBinding?.sourceIdentityField, 'numfunc',
      'ENTITY_LOOKUP with optionSource.valuePropertyPath !== id must preserve fieldName as sourceIdentityField');
  }
  const itemSurfaces = await firstValueFrom(discovery.getItemSurfaces('/api/ergon/' + target.resource, target.sampleId));
  assert(itemSurfaces.surfaces.some((s: any) => s.id === titleBinding.previewSurfaceId && s.availability?.allowed === true),
    'Contextual item surfaces must authorize previewSurfaceId ' + titleBinding.previewSurfaceId);
  if (target.expectIntents) {
    assert(itemSurfaces.surfaces.some((s: any) => s.kind === 'PARTIAL_FORM' && s.scope === 'ITEM'),
      'Resource ' + target.resource + ' must publish contextual PARTIAL_FORM intents for Quick View actions');
  }
  if (target.expectRelatedChildren) {
    assert(itemSurfaces.surfaces.some((s: any) => s.relatedResource?.childResourceKey && s.relatedResource?.childResourcePath),
      'Resource ' + target.resource + ' must publish 1:N relatedResource child surfaces for 360 drawer');
  }
  receipt.quickViewContracts.push({
    resource: target.resource,
    keyField: identity.keyField,
    titleField: identity.titleField,
    subtitleField: identity.subtitleField,
    metadataFields: identity.metadataFields,
    quickViewFieldCount: quickViewFields.length,
    previewSurfaceId: titleBinding.previewSurfaceId,
    surfaceCount: itemSurfaces.surfaces.length
  });
}
const corsPreflight = await fetch(new URL('/api/ergon/vinculos/determinations/provimento', baseUrl), {
  method: 'OPTIONS',
  headers: {
    Origin: 'https://praxis-ergon-showcase.web.app',
    'Access-Control-Request-Method': 'POST',
    'Access-Control-Request-Headers': 'authorization,content-type,x-correlation-id,x-tenant'
  }
});
assert.equal(corsPreflight.status, 200, 'CORS preflight with X-Correlation-Id must succeed');
assert((corsPreflight.headers.get('access-control-allow-headers') ?? '').toLowerCase().includes('x-correlation-id'),
  'Access-Control-Allow-Headers must authorize X-Correlation-Id for Reactive Determinations');
receipt.corsCorrelationHeaderVerified = true;
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
let personCreated = false;
let secondCargoCreated = false;
try {
  const existing = await fetch(new URL('/api/ergon/cargos/9590', baseUrl), { headers: { Authorization: auth } });
  assert.equal(existing.status, 404, 'Reserved fixture identity must be absent');
  await http('/api/ergon/cargos', 'POST', { cargo: 9590, empCodigo: 1, nome: 'Agregador temporário da prova de consumo', categoria: 'SAUDE', controleVaga: false, porReferencia: false, eAglutinador: true });
  fixtureCreated = true;
  const capabilities = await http('/api/ergon/cargos/capabilities');
  const contract = bulkClient.fromCapability(capabilities.operations['bulk-update']);
  const target = await firstValueFrom(bulkClient.readTargetVersion({self: {href: '/api/ergon/cargos/9590'}}, 9590, 'id'));
  const proposal = await firstValueFrom(bulkClient.evaluate(contract, {executionMode: 'SYNC',
    selection: {mode: 'EXPLICIT', targets: [target]}, changes: [{field: 'subcategoria', operator: 'SET', value: 'PROVA'}]}));
  assert.equal(proposal.status, 'READY');
  assert.equal((await firstValueFrom(bulkClient.readProposal(contract, proposal.proposalId))).proposalId, proposal.proposalId);
  assert.equal((await firstValueFrom(bulkClient.readProposalResults(contract, proposal.proposalId))).content.length, 1);
  const key = crypto.randomUUID();
  const confirmed = await firstValueFrom(bulkClient.confirm(contract, proposal, key));
  const executionId = confirmed.execution.executionId;
  assert.equal((await firstValueFrom(bulkClient.confirm(contract, proposal, key))).execution.executionId, executionId);
  const reloadedClient = runInInjectionContext(injector, () => new BulkOperationClientService());
  assert.equal((await firstValueFrom(reloadedClient.readExecution(contract, executionId))).status, 'COMPLETED');
  assert.equal((await firstValueFrom(reloadedClient.readResults(contract, executionId))).content[0].status, 'CONFIRMED');
  assert.equal((await firstValueFrom(reloadedClient.requestCancellation(contract, executionId))).status, 'COMPLETED');
  assert.equal((await http('/api/ergon/cargos/9590')).subcategoria, 'PROVA');
  const stale = await firstValueFrom(bulkClient.evaluate(contract, {executionMode: 'SYNC',
    selection: {mode: 'EXPLICIT', targets: [target]}, changes: [{field: 'subcategoria', operator: 'SET', value: 'OBSOLETA'}]}));
  assert.equal(stale.status, 'BLOCKED');
  assert.throws(() => bulkClient.confirm(contract, stale, crypto.randomUUID()), /READY/);
  assert(JSON.stringify(await firstValueFrom(bulkClient.readProposalResults(contract, stale.proposalId))).includes('CONFLICT'));
  const current = await firstValueFrom(bulkClient.readTargetVersion({self: {href: '/api/ergon/cargos/9590'}}, 9590, 'id'));
  const itemsContract = bulkClient.fromCapability(capabilities.operations['bulk-update-items']);
  const clear = await firstValueFrom(bulkClient.evaluate(itemsContract, {executionMode: 'SYNC', items: [
    {...current, changes: [{field: 'subcategoria', operator: 'CLEAR'}]}]}));
  assert.equal(clear.status, 'READY');
  const cleared = await firstValueFrom(bulkClient.confirm(itemsContract, clear, crypto.randomUUID()));
  assert.equal((await firstValueFrom(bulkClient.readResults(itemsContract, cleared.execution.executionId))).content[0].status, 'CONFIRMED');
  assert.equal((await http('/api/ergon/cargos/9590')).subcategoria ?? null, null);
  receipt.bulkHttpCycle = {proposalId: proposal.proposalId, executionId, confirmed: true, retryStable: true,
    reloaded: true, terminalCancellationPreserved: true, staleProposalBlocked: true, perItemClear: true,
    transport: 'Angular HttpClient over real HTTP'};
  assert.equal((await fetch(new URL('/api/ergon/cargos/9592', baseUrl), {headers: {Authorization: auth}})).status, 404);
  const originBeforeDraft = await fetch(new URL('/api/ergon/cargos/9590', baseUrl), {headers: {Authorization: auth, 'X-Tenant': '1'}});
  assert.equal(originBeforeDraft.status, 200);
  const originEnvelope = await originBeforeDraft.json();
  const draftLink = originEnvelope._links?.['duplicate-draft']?.href;
  assert(draftLink, 'The canonical item link must authorize preparation; no inferred fallback URL');
  const draftCapabilities = await http('/api/ergon/cargos/9590/capabilities');
  assert.equal(draftCapabilities.operations['duplicate-draft'].availability.allowed, true);
  const cargoDraft = await http(draftLink, 'POST');
  assert.equal(cargoDraft.cargo ?? null, null);
  assert.equal(cargoDraft.empCodigo ?? null, null);
  for (const flag of ['controleVaga', 'porReferencia', 'eAglutinador']) assert.equal(cargoDraft[flag], null);
  assert.deepEqual(cargoDraft.customFields, {});
  const originAfterDraft = await fetch(new URL('/api/ergon/cargos/9590', baseUrl), {headers: {Authorization: auth, 'X-Tenant': '1'}});
  assert.equal(originAfterDraft.headers.get('ETag'), originBeforeDraft.headers.get('ETag'));
  const cargoCreateSchema = await http('/schemas/filtered?' + new URLSearchParams({
    resourcePath: '/api/ergon/cargos', operation: 'post', schemaType: 'request'}));
  const cargoCreateFields = normalizeSchemaFields(cargoCreateSchema);
  assert.equal(cargoCreateSchema.properties.cargo['x-ui'].controlType, 'input');
  const completedDraft = {...cargoDraft, cargo: 9592, empCodigo: 1,
    nome: 'Cargo da prova de conflito concorrente', controleVaga: false, porReferencia: false, eAglutinador: false};
  const createPayload = prepareSubmitPayload(completedDraft, cargoCreateFields as any);
  assert.equal(createPayload.cargo, 9592);
  assert.equal(createPayload.empCodigo, 1);
  assert(!('id' in createPayload) && !('version' in createPayload) && !('dtExtincao' in createPayload));
  await http('/api/ergon/cargos', 'POST', createPayload);
  secondCargoCreated = true;
  receipt.duplicateDraft = {canonicalLinkUsed: true, availabilityChecked: true,
    identityAndOrganizationNotCopied: true, sourceEtagUnchanged: true,
    createSchemaNormalized: true, createPayloadPrepared: true, explicitCreationSucceeded: true,
    scope: 'Official schema/submit pipeline over HTTP; no visual or duplicate-action UI orchestration acceptance'};
  const firstMixedTarget = await firstValueFrom(bulkClient.readTargetVersion({self: {href: '/api/ergon/cargos/9590'}}, 9590, 'id'));
  const secondMixedTarget = await firstValueFrom(bulkClient.readTargetVersion({self: {href: '/api/ergon/cargos/9592'}}, 9592, 'id'));
  const mixed = await firstValueFrom(bulkClient.evaluate(contract, {executionMode: 'SYNC',
    selection: {mode: 'EXPLICIT', targets: [firstMixedTarget, secondMixedTarget]},
    changes: [{field: 'subcategoria', operator: 'SET', value: 'LOTE'}]}));
  assert.equal(mixed.status, 'READY');
  await http('/api/ergon/cargos/9592', 'PUT', {nome: 'Cargo da prova de conflito concorrente',
    categoria: 'SAUDE', subcategoria: 'EDICAO EXTERNA'}, {'If-Match': secondMixedTarget.expectedVersion});
  const mixedExecution = await firstValueFrom(bulkClient.confirm(contract, mixed, crypto.randomUUID()));
  const mixedResults = await firstValueFrom(bulkClient.readResults(contract, mixedExecution.execution.executionId));
  assert.deepEqual(mixedResults.content.map(item => item.status).sort(), ['CONFIRMED', 'CONFLICT']);
  assert.equal((await http('/api/ergon/cargos/9590')).subcategoria, 'LOTE');
  assert.equal((await http('/api/ergon/cargos/9592')).subcategoria, 'EDICAO EXTERNA');
  receipt.bulkHttpCycle.partialResult = {executionId: mixedExecution.execution.executionId,
    statuses: mixedResults.content.map(item => item.status), concurrentEditPreserved: true};
  const personPath = '/api/ergon/servidor-pessoas/9591';
  assert.equal((await fetch(new URL(personPath, baseUrl), {headers: {Authorization: auth}})).status, 404);
  await http('/api/ergon/servidor-pessoas', 'POST', {numero: 9591, nome: 'Pessoa da prova de submit',
    cpf: '00000009591', dtnasc: '1990-01-01', eMail: 'prova@example.org', telefone: '12345678'});
  personCreated = true;
  const partialSchema = await http('/schemas/filtered?' + new URLSearchParams({
    resourcePath: '/api/ergon/servidor-pessoas/{id}/dados-bancarios-contato', operation: 'patch', schemaType: 'request'}));
  const partialFields = normalizeSchemaFields(partialSchema);
  const partialForm = new FormGroup({eMail: new FormControl<string | null>('prova@example.org'),
    telefone: new FormControl<string | null>(null)});
  partialForm.controls.eMail.setValue(null); partialForm.controls.eMail.markAsDirty();
  const patchPayload = prepareSubmitPayload(partialForm.getRawValue(), partialFields as any, {dirtyFields: ['eMail']});
  assert.deepEqual(patchPayload, {eMail: null});
  const beforePatch = await fetch(new URL(personPath, baseUrl), {headers: {Authorization: auth}});
  assert.equal(beforePatch.status, 200);
  await http(personPath + '/dados-bancarios-contato', 'PATCH', patchPayload, {'If-Match': beforePatch.headers.get('ETag')});
  const afterPatch = await http(personPath);
  assert.equal(afterPatch.eMail ?? null, null); assert.equal(afterPatch.telefone, '12345678');
  receipt.partialForm = {explicitNullPersisted: true, untouchedOptionalOmitted: true, pipeline: 'prepareSubmitPayload'};
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
  receipt.completed = true;
} finally {
  if (secondCargoCreated) {
    await http('/api/ergon/cargos/9592', 'DELETE');
    assert.equal((await fetch(new URL('/api/ergon/cargos/9592', baseUrl), {headers: {Authorization: auth}})).status, 404);
  }
  if (personCreated) {
    await http('/api/ergon/servidor-pessoas/9591', 'DELETE');
    assert.equal((await fetch(new URL('/api/ergon/servidor-pessoas/9591', baseUrl), {headers: {Authorization: auth}})).status, 404);
  }
  if (fixtureCreated) {
    await http('/api/ergon/cargos/9590', 'DELETE');
    const missing = await fetch(new URL('/api/ergon/cargos/9590', baseUrl), { headers: { Authorization: auth } });
    assert.equal(missing.status, 404);
    receipt.fixtureRemoved = true;
  }
  await fs.writeFile(path.join(evidenceDir, 'angular-consumer-proof.json'), JSON.stringify(receipt, null, 2));
}
console.log(JSON.stringify({ schemas: receipt.schemas.length, executions: receipt.executions.length, bulkContracts: receipt.bulkContracts.length, quickViewContracts: receipt.quickViewContracts.length, corsCorrelationHeaderVerified: receipt.corsCorrelationHeaderVerified, staleResultProtected: receipt.staleResultProtected, rejectionVisible: receipt.rejectionVisible, fixtureRemoved: receipt.fixtureRemoved }));
