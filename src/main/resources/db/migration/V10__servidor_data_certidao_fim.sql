-- FUNCIONARIOS.DT_CERT_FIM no legado indica óbito ou desaparecimento.
ALTER TABLE funcionarios ADD COLUMN IF NOT EXISTS dt_cert_fim DATE;
