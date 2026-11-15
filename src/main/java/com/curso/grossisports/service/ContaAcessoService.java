package com.curso.grossisports.service;

import com.curso.grossisports.domain.ContaAcesso;
import com.curso.grossisports.domain.PerfilAcesso;
import com.curso.grossisports.domain.Pessoa;
import com.curso.grossisports.domain.StatusContaAcesso;
import com.curso.grossisports.domain.TipoEventoSeguranca;
import com.curso.grossisports.exception.RecursoDuplicadoException;
import com.curso.grossisports.exception.RecursoNaoEncontradoException;
import com.curso.grossisports.repository.ContaAcessoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Casos de uso de conta de acesso (Aulas 12–14).
 *
 * <p>Aula 14: criação e administração de contas exigem ADMINISTRADOR
 * (defesa em profundidade com {@code @PreAuthorize}, além da política
 * HTTP). As invariantes administrativas impedem que a conta fique sem
 * perfil, que o administrador se bloqueie ou se rebaixe e que o último
 * administrador ativo desapareça.</p>
 */
@Service
public class ContaAcessoService {

    private static final int TAMANHO_MINIMO_SENHA = 12;
    private static final int LIMITE_BCRYPT_BYTES = 72;

    private final ContaAcessoRepository repository;
    private final PessoaService pessoaService;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaSegurancaService auditoria;

    public ContaAcessoService(
        ContaAcessoRepository repository,
        PessoaService pessoaService,
        PasswordEncoder passwordEncoder,
        AuditoriaSegurancaService auditoria) {
        this.repository = repository;
        this.pessoaService = pessoaService;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    /**
     * Criação administrativa: cria a pessoa e a conta na mesma
     * transação, sempre com o perfil OPERADOR. Não aceita pessoa
     * existente nem perfil de acesso. O evento {@code CONTA_CRIADA} é
     * gravado na mesma transação: se a auditoria falhar, a criação é
     * revertida.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso cadastrar(
        String nome,
        String email,
        String cpf,
        String login,
        String senhaAberta,
        String loginAdministrador,
        String enderecoOrigem) {

        validarSenhaAberta(senhaAberta);

        if (repository.existsByLoginIgnoreCase(login)) {
            throw new RecursoDuplicadoException(
                "Login já cadastrado");
        }

        Pessoa pessoa = pessoaService.cadastrar(nome, email, cpf);

        String senhaHash = passwordEncoder.encode(senhaAberta);

        ContaAcesso conta = repository.save(
            new ContaAcesso(pessoa, login, senhaHash));

        auditoria.registrar(
            TipoEventoSeguranca.CONTA_CRIADA,
            idDaConta(loginAdministrador),
            conta.getId(),
            null,
            enderecoOrigem);

        return conta;
    }

    @Transactional(readOnly = true)
    public ContaAcesso buscarPorLogin(String login) {
        return repository.findByLoginIgnoreCase(login)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Conta de acesso não encontrada"));
    }

    @Transactional
    public void alterarSenha(
        String login,
        String senhaAtual,
        String novaSenha,
        String enderecoOrigem) {

        validarSenhaAberta(novaSenha);

        ContaAcesso conta = buscarPorLogin(login);

        if (!passwordEncoder.matches(
            senhaAtual, conta.getSenhaHash())) {
            throw new IllegalArgumentException(
                "Senha atual inválida");
        }

        if (passwordEncoder.matches(
            novaSenha, conta.getSenhaHash())) {
            throw new IllegalArgumentException(
                "Nova senha deve ser diferente da senha atual");
        }

        conta.alterarSenha(passwordEncoder.encode(novaSenha));

        auditoria.registrar(
            TipoEventoSeguranca.SENHA_ALTERADA,
            conta.getId(),
            conta.getId(),
            null,
            enderecoOrigem);
    }

    /**
     * Substitui os perfis de uma conta. Só ADMINISTRADOR. Impede que o
     * administrador remova o próprio perfil administrativo e que o
     * último administrador ativo perca o perfil.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarPerfis(
        Long id,
        Set<PerfilAcesso> novosPerfis,
        String loginAutenticado,
        String enderecoOrigem) {

        if (novosPerfis == null || novosPerfis.isEmpty()) {
            throw new IllegalArgumentException(
                "A conta deve possuir ao menos um perfil");
        }

        travarAdministradores();

        ContaAcesso conta = buscarDetalhadaPorId(id);

        boolean alvoEraAdminAtivo =
            conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR)
                && conta.getStatus() == StatusContaAcesso.ATIVA;

        boolean perdeAdministrador = alvoEraAdminAtivo
            && !novosPerfis.contains(PerfilAcesso.ADMINISTRADOR);

        if (perdeAdministrador) {
            impedirAutoRebaixamento(conta, loginAutenticado);
            impedirUltimoAdministrador();
        }

        conta.substituirPerfis(novosPerfis);

        auditoria.registrar(
            TipoEventoSeguranca.PERFIS_ALTERADOS,
            idDaConta(loginAutenticado),
            conta.getId(),
            null,
            enderecoOrigem);

        return conta;
    }

    /**
     * Ativa ou bloqueia uma conta. Só ADMINISTRADOR. Impede que o
     * administrador bloqueie a própria conta e que o último
     * administrador ativo seja bloqueado.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarStatus(
        Long id,
        StatusContaAcesso status,
        String loginAutenticado,
        String enderecoOrigem) {

        if (status == null) {
            throw new IllegalArgumentException(
                "Status é obrigatório");
        }

        travarAdministradores();

        ContaAcesso conta = buscarDetalhadaPorId(id);

        boolean bloqueando =
            status == StatusContaAcesso.BLOQUEADA;

        boolean alvoEraAdminAtivo =
            conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR)
                && conta.getStatus() == StatusContaAcesso.ATIVA;

        if (bloqueando && alvoEraAdminAtivo) {
            impedirAutoBloqueio(conta, loginAutenticado);
            impedirUltimoAdministrador();
        }

        if (bloqueando) {
            conta.bloquear();
        } else {
            conta.ativar();
        }

        auditoria.registrar(
            TipoEventoSeguranca.STATUS_ALTERADO,
            idDaConta(loginAutenticado),
            conta.getId(),
            null,
            enderecoOrigem);

        return conta;
    }

    /**
     * Regra de senha compartilhada (criação, troca e bootstrap).
     * Não é anotada com {@code @PreAuthorize}: é uma validação de
     * domínio reutilizada por outros serviços.
     */
    public void validarSenhaAberta(String senha) {
        if (senha == null
            || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new IllegalArgumentException(
                "Senha deve possuir ao menos 12 caracteres");
        }

        if (senha.getBytes(StandardCharsets.UTF_8).length
            > LIMITE_BCRYPT_BYTES) {
            throw new IllegalArgumentException(
                "Senha deve possuir no máximo 72 bytes em UTF-8");
        }
    }

    private ContaAcesso buscarDetalhadaPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() ->
                new RecursoNaoEncontradoException(
                    "Conta de acesso não encontrada"));
    }

    /**
     * Resolve o identificador histórico do ator a partir do login
     * autenticado. Devolve {@code null} quando o login não corresponde a
     * uma conta, preservando o evento sem inventar um id.
     */
    private Long idDaConta(String login) {
        if (login == null) {
            return null;
        }

        return repository.findByLoginIgnoreCase(login)
            .map(ContaAcesso::getId)
            .orElse(null);
    }

    private void travarAdministradores() {
        repository.travarPorPerfil(PerfilAcesso.ADMINISTRADOR);
    }

    private void impedirAutoRebaixamento(
        ContaAcesso conta, String loginAutenticado) {

        if (conta.getLogin().equalsIgnoreCase(loginAutenticado)) {
            throw new IllegalArgumentException(
                "Não é permitido remover o próprio perfil de "
                    + "administrador");
        }
    }

    private void impedirAutoBloqueio(
        ContaAcesso conta, String loginAutenticado) {

        if (conta.getLogin().equalsIgnoreCase(loginAutenticado)) {
            throw new IllegalArgumentException(
                "Não é permitido bloquear a própria conta");
        }
    }

    private void impedirUltimoAdministrador() {
        long ativos = repository.contarPorPerfilEStatus(
            PerfilAcesso.ADMINISTRADOR, StatusContaAcesso.ATIVA);

        if (ativos <= 1) {
            throw new IllegalArgumentException(
                "O sistema deve manter ao menos um administrador ativo");
        }
    }
}
