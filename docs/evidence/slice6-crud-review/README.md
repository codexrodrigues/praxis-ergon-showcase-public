# Reprodução da revisão do CRUD Slice 6

**Artefato histórico anterior às correções R9–R15.** Os runners abaixo esperam
os defeitos e não constituem gate do código corrigido. A prova atual é
`TempoLicencaSevenFixesRegressionTest`, que exige as recusas e os saldos corretos.
O manifesto preserva os hashes do código anterior, não os bytes atuais.

Estes probes são artefatos de revisão, fora da suíte de produção. Compartilham a
fixture de `TempoLicencaCrudContractTest`, usando os serviços e mappers reais,
com EntityManager e repositórios Mockito. Não abrem conexão de banco.

- `CrudReviewProbe.java`: C1–C8, incluindo a tentativa C7b; resultados em `counterexamples.txt`.
- `FilterReviewProbe.java`: metamodel Hibernate real e construção do Criteria `IS_FALSE`; não executa SQL.
- `source-manifest.json`: SHA-256 dos serviços, POM, fixture e auditor inspecionados.
- `../SLICE-6-CRUD-REVISAO-SCORECARD.md`: auditor estrito reexecutado, exit 1.

Na raiz do host, com Java 21 e as classes de teste compiladas:

```powershell
mvn test "-Dtest=!*Neon*"
mvn dependency:build-classpath "-Dmdep.includeScope=test" "-Dmdep.outputFile=.local/review-slice6-classpath.txt"
$reviewCp = 'target/test-classes;target/classes;' + (Get-Content .local/review-slice6-classpath.txt -Raw).Trim()
javac -proc:none -cp $reviewCp -d .local/review-slice6/classes docs/evidence/slice6-crud-review/CrudReviewProbe.java docs/evidence/slice6-crud-review/FilterReviewProbe.java
java -cp ('.local/review-slice6/classes;' + $reviewCp) br.com.praxis.ergonshowcase.CrudReviewProbe
java -cp ('.local/review-slice6/classes;' + $reviewCp) FilterReviewProbe
```

O runner confirma que o comportamento indevido continua reproduzível. Seu exit
zero significa que os contraexemplos descritos ocorreram; não significa aceite
funcional. Após corrigir o domínio, convertê-los em testes de regressão que
exijam a recusa/contabilidade correta, sem conservar expectativas do defeito.
