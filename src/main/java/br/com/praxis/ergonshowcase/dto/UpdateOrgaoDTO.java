package br.com.praxis.ergonshowcase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.praxisplatform.uischema.annotation.*;

@Getter
@Setter
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"id"})
@Schema(
    description =
        "Manutenção cadastral de órgãos, com identidade preservada e versão exigida em If-Match.")
public class UpdateOrgaoDTO extends OrgaoDados {}
