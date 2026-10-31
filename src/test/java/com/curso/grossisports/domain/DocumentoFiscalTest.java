
package com.curso.grossisports.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoFiscalTest {

    @Test
    void deveValidarCpfCorreto() {
        assertTrue(DocumentoFiscal.cpfValido("52998224725"));
    }

    @Test
    void deveRejeitarCpfInvalido() {
        assertFalse(DocumentoFiscal.cpfValido("52998224724"));
        assertFalse(DocumentoFiscal.cpfValido("11111111111"));
        assertFalse(DocumentoFiscal.cpfValido("123"));
        assertFalse(DocumentoFiscal.cpfValido(null));
    }

    @Test
    void deveValidarCnpjCorreto() {
        assertTrue(DocumentoFiscal.cnpjValido("11222333000181"));
    }

    @Test
    void deveRejeitarCnpjInvalido() {
        assertFalse(DocumentoFiscal.cnpjValido("11222333000180"));
        assertFalse(DocumentoFiscal.cnpjValido("11111111111111"));
        assertFalse(DocumentoFiscal.cnpjValido("123"));
        assertFalse(DocumentoFiscal.cnpjValido(null));
    }
}

