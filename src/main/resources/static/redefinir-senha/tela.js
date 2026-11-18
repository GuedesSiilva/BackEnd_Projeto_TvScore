"use strict";

const formulario = document.getElementById("formulario");
const novaSenha = document.getElementById("nova-senha");
const confirmacao = document.getElementById("confirmacao");
const salvar = document.getElementById("salvar");
const mensagem = document.getElementById("mensagem");
let token = new URLSearchParams(location.search).get("token");

if (location.search || location.hash) {
  history.replaceState(null, "", location.pathname);
}

if (!token || !/^[A-Za-z0-9_-]{43}$/.test(token)) {
  token = null;
  mensagem.textContent = "Este link é inválido. Peça um novo link pelo TV Score.";
} else {
  formulario.hidden = false;
}

formulario.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  if (!token || salvar.disabled) return;

  const senha = novaSenha.value;
  const bytes = new TextEncoder().encode(senha).length;
  if (bytes < 8 || bytes > 72) {
    mensagem.textContent = "A senha deve ter entre 8 e 72 bytes.";
    novaSenha.focus();
    return;
  }
  if (senha !== confirmacao.value) {
    mensagem.textContent = "As duas senhas precisam ser iguais.";
    confirmacao.focus();
    return;
  }

  salvar.disabled = true;
  mensagem.textContent = "Salvando sua nova senha…";
  try {
    const resposta = await fetch("/api/auth/senha/redefinicao", {
      method: "POST",
      credentials: "omit",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token, novaSenha: senha }),
    });
    if (resposta.status === 204) {
      token = null;
      formulario.reset();
      formulario.hidden = true;
      mensagem.textContent = "Senha alterada. Volte ao app TV Score e faça login com a nova senha.";
    } else if (resposta.status === 400) {
      mensagem.textContent = "Não foi possível redefinir a senha. Confira a senha ou peça um novo link se ele expirou ou já foi usado.";
    } else if (resposta.status === 429) {
      mensagem.textContent = "Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.";
    } else {
      mensagem.textContent = "O serviço está indisponível no momento. Tente novamente mais tarde.";
    }
  } catch {
    mensagem.textContent = "Não foi possível conectar. Confira a internet e tente novamente.";
  } finally {
    salvar.disabled = false;
  }
});
