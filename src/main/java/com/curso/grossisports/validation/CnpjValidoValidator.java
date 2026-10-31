
package com.curso.grossisports.validation;

import com.curso.grossisports.domain.DocumentoFiscal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidoValidator
    implements ConstraintValidator<CnpjValido, String> {

    @Override
    public boolean isValid(
        String cnpj,
        ConstraintValidatorContext context) {

        if (cnpj == null || cnpj.isBlank()) {
            return true;
        }

        if (!cnpj.matches("\\d{14}")) {
            return true;
        }

        return DocumentoFiscal.cnpjValido(cnpj);
    }
}
