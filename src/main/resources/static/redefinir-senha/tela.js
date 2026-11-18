"use strict";

const formulario = document.getElementById("formulario");
const novaSenha = document.getElementById("nova-senha");
const confirmacao = document.getElementById("confirmacao");
const salvar = document.getElementById("salvar");
const mostrarSenhas = document.getElementById("mostrar-senhas");
const mensagem = document.getElementById("mensagem");
let token = new URLSearchParams(location.search).get("token");

if (location.search || location.hash) {
  history.replaceState(null, "", location.pathname);
}

function atualizarVisibilidade() {
  const tipo = mostrarSenhas.checked ? "text" : "password";
  novaSenha.type = tipo;
  confirmacao.type = tipo;
}

function informar(texto, estado) {
  mensagem.textContent = texto;
  mensagem.dataset.estado = estado;
}

mostrarSenhas.addEventListener("change", atualizarVisibilidade);

if (!token || !/^[A-Za-z0-9_-]{43}$/.test(token)) {
  token = null;
  informar("Este link é inválido. Peça um novo link pelo TV Score.", "erro");
} else {
  formulario.hidden = false;
}

formulario.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  if (!token || salvar.disabled) return;

  const senha = novaSenha.value;
  if ([...senha].length < 6 || /^\p{White_Space}*$/u.test(senha)) {
    informar("A senha deve ter pelo menos 6 caracteres e não pode conter apenas espaços.", "erro");
    novaSenha.focus();
    return;
  }
  if (/[\u20E3\uFE0F]/u.test(senha) ||
      [...senha].some(char => char.codePointAt(0) > 127 && /\p{Emoji}/u.test(char))) {
    informar("Emojis não são permitidos na senha.", "erro");
    novaSenha.focus();
    return;
  }
  if (new TextEncoder().encode(senha).length > 72) {
    informar("A senha está muito longa. Escolha uma senha mais curta.", "erro");
    novaSenha.focus();
    return;
  }
  if (senha !== confirmacao.value) {
    informar("As duas senhas precisam ser iguais.", "erro");
    confirmacao.focus();
    return;
  }

  salvar.disabled = true;
  informar("Salvando sua nova senha…", "aguarde");
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
      atualizarVisibilidade();
      formulario.hidden = true;
      informar("Senha alterada. Volte ao app TV Score e faça login com a nova senha.", "sucesso");
    } else if (resposta.status === 400) {
      informar("Não foi possível redefinir a senha. Confira a senha ou peça um novo link se ele expirou ou já foi usado.", "erro");
    } else if (resposta.status === 429) {
      informar("Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.", "erro");
    } else {
      informar("O serviço está indisponível no momento. Tente novamente mais tarde.", "erro");
    }
  } catch {
    informar("Não foi possível conectar. Confira a internet e tente novamente.", "erro");
  } finally {
    salvar.disabled = false;
  }
});
