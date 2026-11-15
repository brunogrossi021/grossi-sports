package com.curso.grossisports.service;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.ProtecaoAutenticacao;
import com.curso.grossisports.domain.TipoEventoSeguranca;
import com.curso.grossisports.repository.ContaAcessoRepository;
import com.curso.grossisports.repository.ProtecaoAutenticacaoRepository;
import com.curso.grossisports.security.ProtecaoAutenticacaoProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Controle persistente de tentativas de login (Aula 17).
 *
 * <p>O login é normalizado ({@code trim()} e minúsculas) e reduzido a um
 * hash SHA-256 hexadecimal. A tabela de proteção guarda apenas o hash —
 * o login original nunca é persistido. Cada método transacional confirma
 * suas gravações ao retornar, de modo que o contador e a evidência
 * sobrevivam à exceção genérica {@code 401} lançada pelo caso de uso.</p>
 *
 * <p>A resposta externa é a mesma para login inexistente, senha
 * incorreta, conta bloqueada e bloqueio temporário ativo; a decisão de
 * recusar acontece antes de qualquer comparação BCrypt.</p>
 */
@Service
public class ProtecaoAutenticacaoService {

    private static final String ALGORITMO_HASH = "SHA-256";

    private final ProtecaoAutenticacaoRepository protecaoRepository;
    private final ContaAcessoRepository contaRepository;
    private final AuditoriaSegurancaService auditoria;
    private final ProtecaoAutenticacaoProperties properties;
    private final Clock clock;

    public ProtecaoAutenticacaoService(
        ProtecaoAutenticacaoRepository protecaoRepository,
        ContaAcessoRepository contaRepository,
        AuditoriaSegurancaService auditoria,
        ProtecaoAutenticacaoProperties properties,
        Clock clock) {

        this.protecaoRepository = protecaoRepository;
        this.contaRepository = contaRepository;
        this.auditoria = auditoria;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Decide se a tentativa pode seguir para a verificação de senha. Um
     * bloqueio ativo é recusado aqui, antes de executar BCrypt, e gera o
     * evento {@code LOGIN_RECUSADO_BLOQUEIO}.
     */
    @Transactional
    public boolean permitirTentativa(
        String login, String enderecoOrigem) {

        String chave = hash(login);
        Instant agora = clock.instant();

        boolean bloqueada = protecaoRepository.findByChaveLogin(chave)
            .map(protecao -> protecao.estaBloqueada(agora))
            .orElse(false);

        if (bloqueada) {
            auditoria.registrar(
                TipoEventoSeguranca.LOGIN_RECUSADO_BLOQUEIO,
                null,
                null,
                chave,
                enderecoOrigem);
        }

        return !bloqueada;
    }

    /**
     * Limpa o contador após autenticação válida e registra o evento de
     * sucesso com o ator conhecido.
     */
    @Transactional
    public void registrarSucesso(
        String login, String enderecoOrigem) {

        String chave = hash(login);

        protecaoRepository.buscarParaAtualizacao(chave)
            .ifPresent(ProtecaoAutenticacao::registrarSucesso);

        Long contaId = contaRepository
            .findByLoginIgnoreCase(login)
            .map(ContaAcesso::getId)
            .orElse(null);

        // O hash do identificador correlaciona o sucesso às falhas do
        // mesmo login sem guardar o login original.
        auditoria.registrar(
            TipoEventoSeguranca.LOGIN_SUCESSO,
            contaId,
            contaId,
            chave,
            enderecoOrigem);
    }

    /**
     * Registra uma falha de credencial. A linha é criada por
     * <em>upsert</em> quando ainda não existe e relida com bloqueio
     * pessimista, garantindo que duas falhas simultâneas não percam
     * atualização. A falha que ativa o bloqueio gera o evento adicional
     * {@code BLOQUEIO_TEMPORARIO_ATIVADO}.
     */
    @Transactional
    public void registrarFalha(
        String login, String enderecoOrigem) {

        String chave = hash(login);
        Instant agora = clock.instant();

        protecaoRepository.inserirSeAusente(chave);

        ProtecaoAutenticacao protecao = protecaoRepository
            .buscarParaAtualizacao(chave)
            .orElseThrow(() -> new IllegalStateException(
                "Contador de proteção ausente após a inserção"));

        boolean ativouBloqueio = protecao.registrarFalha(
            agora,
            properties.maxFailedAttempts(),
            properties.failureWindow(),
            properties.lockDuration());

        if (ativouBloqueio) {
            auditoria.registrar(
                TipoEventoSeguranca.BLOQUEIO_TEMPORARIO_ATIVADO,
                null,
                null,
                chave,
                enderecoOrigem);
        }

        auditoria.registrar(
            TipoEventoSeguranca.LOGIN_FALHA,
            null,
            null,
            chave,
            enderecoOrigem);
    }

    /**
     * Hash SHA-256 hexadecimal do login normalizado. Login ausente é
     * tratado como texto vazio para nunca persistir {@code null} nem
     * quebrar o fluxo.
     */
    public String hash(String login) {
        String normalizado = login == null
            ? ""
            : login.trim().toLowerCase(Locale.ROOT);

        try {
            byte[] resumo = MessageDigest
                .getInstance(ALGORITMO_HASH)
                .digest(normalizado.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "Algoritmo SHA-256 indisponível", exception);
        }
    }
}
