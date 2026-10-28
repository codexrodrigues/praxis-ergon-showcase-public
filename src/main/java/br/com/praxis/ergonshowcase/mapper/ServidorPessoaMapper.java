package br.com.praxis.ergonshowcase.mapper;

import br.com.praxis.ergonshowcase.domain.*;
import br.com.praxis.ergonshowcase.dto.*;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.springframework.stereotype.Component;

/** Fronteira explícita: somente campos cadastrais são copiados na manutenção. */
@Component
public class ServidorPessoaMapper
    implements ResourceMapper<
        ServidorPessoa,
        ServidorPessoaResponseDTO,
        CreateServidorPessoaDTO,
        UpdateServidorPessoaDTO,
        Long> {
  public ServidorPessoaResponseDTO toResponse(ServidorPessoa entity) {
    var dto = new ServidorPessoaResponseDTO();
    dto.setNome(entity.getNome());
    dto.setNomeSocial(entity.getNomeSocial());
    dto.setCpf(entity.getCpf());
    dto.setDtnasc(entity.getDtnasc());
    dto.setEMail(entity.getEMail());
    dto.setTelefone(entity.getTelefone());
    dto.setNumtelCelular(entity.getNumtelCelular());
    dto.setPispasep(entity.getPispasep());
    dto.setCustomFields(entity.getCustomFields());
    dto.setId(entity.getNumero());
    dto.setTratamento(entity.getTratamento());
    dto.setSexo(entity.getSexo());
    dto.setGenero(entity.getGenero());
    dto.setCidnasc(entity.getCidnasc());
    dto.setUfnasc(entity.getUfnasc());
    dto.setGSanguineo(entity.getGSanguineo());
    dto.setRacaCor(entity.getRacaCor());
    dto.setEstcivil(entity.getEstcivil());
    dto.setUniaoEstavel(entity.getUniaoEstavel());
    dto.setEscolaridade(entity.getEscolaridade());
    dto.setNacionalidade(entity.getNacionalidade());
    dto.setChegbrasil(entity.getChegbrasil());
    dto.setUfempant(entity.getUfempant());
    dto.setAnoprimemp(entity.getAnoprimemp());
    dto.setPai(entity.getPai());
    dto.setMae(entity.getMae());
    dto.setNumrg(entity.getNumrg());
    dto.setTiporg(entity.getTiporg());
    dto.setOrgaorg(entity.getOrgaorg());
    dto.setUfrg(entity.getUfrg());
    dto.setExpedrg(entity.getExpedrg());
    dto.setDatapis(entity.getDatapis());
    dto.setBancopis(entity.getBancopis());
    dto.setNumcartpro(entity.getNumcartpro());
    dto.setSercartpro(entity.getSercartpro());
    dto.setUfcartpro(entity.getUfcartpro());
    dto.setNumtitel(entity.getNumtitel());
    dto.setZonatitel(entity.getZonatitel());
    dto.setSectitel(entity.getSectitel());
    dto.setUftitel(entity.getUftitel());
    dto.setNumdocmili(entity.getNumdocmili());
    dto.setSerdocmili(entity.getSerdocmili());
    dto.setCnh(entity.getCnh());
    dto.setCatcnh(entity.getCatcnh());
    dto.setValidcnh(entity.getValidcnh());
    dto.setUfcnh(entity.getUfcnh());
    dto.setIdentprof(entity.getIdentprof());
    dto.setTipoidprof(entity.getTipoidprof());
    dto.setDocumentoLegalId(entity.getDocumentoLegalId());
    dto.setTipologender(entity.getTipologender());
    dto.setNomelogender(entity.getNomelogender());
    dto.setNumender(entity.getNumender());
    dto.setComplender(entity.getComplender());
    dto.setBairroender(entity.getBairroender());
    dto.setCidadeender(entity.getCidadeender());
    dto.setUfender(entity.getUfender());
    dto.setCepender(entity.getCepender());
    dto.setRamal(entity.getRamal());
    dto.setBanco(entity.getBanco());
    dto.setAgencia(entity.getAgencia());
    dto.setConta(entity.getConta());
    dto.setTipopag(entity.getTipopag());
    dto.setDeffisica(entity.getDeffisica());
    dto.setDefvisual(entity.getDefvisual());
    dto.setDefauditiva(entity.getDefauditiva());
    dto.setDefmental(entity.getDefmental());
    dto.setDefintelectual(entity.getDefintelectual());
    dto.setObsDeficiencia(entity.getObsDeficiencia());
    dto.setDtRecadast(entity.getDtRecadast());
    dto.setIdadeAnos(
        entity.getDtnasc() == null
            ? null
            : java.time.Period.between(
                    entity.getDtnasc(), java.time.LocalDate.now(java.time.ZoneOffset.UTC))
                .getYears());
    dto.setPossuiDeficienciaPcD(
        br.com.praxis.ergonshowcase.service.ErgonDerivedFacts.possuiDeficiencia(
            entity.getDeffisica(),
            entity.getDefvisual(),
            entity.getDefauditiva(),
            entity.getDefmental(),
            entity.getDefintelectual()));
    return dto;
  }

  public ServidorPessoa newEntity(CreateServidorPessoaDTO dto) {
    var entity = new ServidorPessoa();
    entity.setNome(dto.getNome());
    entity.setNomeSocial(dto.getNomeSocial());
    entity.setCpf(dto.getCpf());
    entity.setDtnasc(dto.getDtnasc());
    entity.setEMail(dto.getEMail());
    entity.setTelefone(dto.getTelefone());
    entity.setNumtelCelular(dto.getNumtelCelular());
    entity.setPispasep(dto.getPispasep());
    entity.setCustomFields(dto.getCustomFields());
    entity.setNumero(dto.getNumero());
    entity.setTratamento(dto.getTratamento());
    entity.setSexo(dto.getSexo());
    entity.setGenero(dto.getGenero());
    entity.setCidnasc(dto.getCidnasc());
    entity.setUfnasc(dto.getUfnasc());
    entity.setGSanguineo(dto.getGSanguineo());
    entity.setRacaCor(dto.getRacaCor());
    entity.setEstcivil(dto.getEstcivil());
    entity.setUniaoEstavel(dto.getUniaoEstavel());
    entity.setEscolaridade(dto.getEscolaridade());
    entity.setNacionalidade(dto.getNacionalidade());
    entity.setChegbrasil(dto.getChegbrasil());
    entity.setUfempant(dto.getUfempant());
    entity.setAnoprimemp(dto.getAnoprimemp());
    entity.setPai(dto.getPai());
    entity.setMae(dto.getMae());
    entity.setNumrg(dto.getNumrg());
    entity.setTiporg(dto.getTiporg());
    entity.setOrgaorg(dto.getOrgaorg());
    entity.setUfrg(dto.getUfrg());
    entity.setExpedrg(dto.getExpedrg());
    entity.setDatapis(dto.getDatapis());
    entity.setBancopis(dto.getBancopis());
    entity.setNumcartpro(dto.getNumcartpro());
    entity.setSercartpro(dto.getSercartpro());
    entity.setUfcartpro(dto.getUfcartpro());
    entity.setNumtitel(dto.getNumtitel());
    entity.setZonatitel(dto.getZonatitel());
    entity.setSectitel(dto.getSectitel());
    entity.setUftitel(dto.getUftitel());
    entity.setNumdocmili(dto.getNumdocmili());
    entity.setSerdocmili(dto.getSerdocmili());
    entity.setCnh(dto.getCnh());
    entity.setCatcnh(dto.getCatcnh());
    entity.setValidcnh(dto.getValidcnh());
    entity.setUfcnh(dto.getUfcnh());
    entity.setIdentprof(dto.getIdentprof());
    entity.setTipoidprof(dto.getTipoidprof());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setTipologender(dto.getTipologender());
    entity.setNomelogender(dto.getNomelogender());
    entity.setNumender(dto.getNumender());
    entity.setComplender(dto.getComplender());
    entity.setBairroender(dto.getBairroender());
    entity.setCidadeender(dto.getCidadeender());
    entity.setUfender(dto.getUfender());
    entity.setCepender(dto.getCepender());
    entity.setRamal(dto.getRamal());
    entity.setBanco(dto.getBanco());
    entity.setAgencia(dto.getAgencia());
    entity.setConta(dto.getConta());
    entity.setTipopag(dto.getTipopag());
    entity.setDeffisica(dto.getDeffisica());
    entity.setDefvisual(dto.getDefvisual());
    entity.setDefauditiva(dto.getDefauditiva());
    entity.setDefmental(dto.getDefmental());
    entity.setDefintelectual(dto.getDefintelectual());
    entity.setObsDeficiencia(dto.getObsDeficiencia());
    entity.setDtRecadast(dto.getDtRecadast());
    return entity;
  }

  public void applyUpdate(ServidorPessoa entity, UpdateServidorPessoaDTO dto) {
    entity.setNome(dto.getNome());
    entity.setNomeSocial(dto.getNomeSocial());
    entity.setCpf(dto.getCpf());
    entity.setDtnasc(dto.getDtnasc());
    entity.setEMail(dto.getEMail());
    entity.setTelefone(dto.getTelefone());
    entity.setNumtelCelular(dto.getNumtelCelular());
    entity.setPispasep(dto.getPispasep());
    entity.setCustomFields(dto.getCustomFields());
    entity.setTratamento(dto.getTratamento());
    entity.setSexo(dto.getSexo());
    entity.setGenero(dto.getGenero());
    entity.setCidnasc(dto.getCidnasc());
    entity.setUfnasc(dto.getUfnasc());
    entity.setGSanguineo(dto.getGSanguineo());
    entity.setRacaCor(dto.getRacaCor());
    entity.setEstcivil(dto.getEstcivil());
    entity.setUniaoEstavel(dto.getUniaoEstavel());
    entity.setEscolaridade(dto.getEscolaridade());
    entity.setNacionalidade(dto.getNacionalidade());
    entity.setChegbrasil(dto.getChegbrasil());
    entity.setUfempant(dto.getUfempant());
    entity.setAnoprimemp(dto.getAnoprimemp());
    entity.setPai(dto.getPai());
    entity.setMae(dto.getMae());
    entity.setNumrg(dto.getNumrg());
    entity.setTiporg(dto.getTiporg());
    entity.setOrgaorg(dto.getOrgaorg());
    entity.setUfrg(dto.getUfrg());
    entity.setExpedrg(dto.getExpedrg());
    entity.setDatapis(dto.getDatapis());
    entity.setBancopis(dto.getBancopis());
    entity.setNumcartpro(dto.getNumcartpro());
    entity.setSercartpro(dto.getSercartpro());
    entity.setUfcartpro(dto.getUfcartpro());
    entity.setNumtitel(dto.getNumtitel());
    entity.setZonatitel(dto.getZonatitel());
    entity.setSectitel(dto.getSectitel());
    entity.setUftitel(dto.getUftitel());
    entity.setNumdocmili(dto.getNumdocmili());
    entity.setSerdocmili(dto.getSerdocmili());
    entity.setCnh(dto.getCnh());
    entity.setCatcnh(dto.getCatcnh());
    entity.setValidcnh(dto.getValidcnh());
    entity.setUfcnh(dto.getUfcnh());
    entity.setIdentprof(dto.getIdentprof());
    entity.setTipoidprof(dto.getTipoidprof());
    entity.setDocumentoLegalId(dto.getDocumentoLegalId());
    entity.setTipologender(dto.getTipologender());
    entity.setNomelogender(dto.getNomelogender());
    entity.setNumender(dto.getNumender());
    entity.setComplender(dto.getComplender());
    entity.setBairroender(dto.getBairroender());
    entity.setCidadeender(dto.getCidadeender());
    entity.setUfender(dto.getUfender());
    entity.setCepender(dto.getCepender());
    entity.setRamal(dto.getRamal());
    entity.setBanco(dto.getBanco());
    entity.setAgencia(dto.getAgencia());
    entity.setConta(dto.getConta());
    entity.setTipopag(dto.getTipopag());
    entity.setDeffisica(dto.getDeffisica());
    entity.setDefvisual(dto.getDefvisual());
    entity.setDefauditiva(dto.getDefauditiva());
    entity.setDefmental(dto.getDefmental());
    entity.setDefintelectual(dto.getDefintelectual());
    entity.setObsDeficiencia(dto.getObsDeficiencia());
    entity.setDtRecadast(dto.getDtRecadast());
  }

  public Long extractId(ServidorPessoa entity) {
    return entity.getNumero();
  }
}
