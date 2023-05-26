package it.gov.pagopa.common.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
public class ErrorDTO {

    @NotBlank
    @ApiModelProperty(required = true, value = "Code of the error message", example = "Code")
    private String code;
    @NotBlank
    @ApiModelProperty(required = true, value = "Content of the error message", example = "Message")
    private String message;

}
