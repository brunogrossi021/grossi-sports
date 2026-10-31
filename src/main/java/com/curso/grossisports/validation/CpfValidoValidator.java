
package com.curso.grossisports.validation;

import com.curso.grossisports.domain.DocumentoFiscal;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidoValidator
    implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(
        String cpf,
        ConstraintValidatorContext context) {

        if (cpf == null || cpf.isBlank()) {
            return true;
        }

        if (!cpf.matches("\\d{11}")) {
            return true;
        }

        return DocumentoFiscal.cpfValido(cpf);
    }
}
