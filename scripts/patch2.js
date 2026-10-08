const fs=require('fs'),os=require('os');
const p=os.homedir()+'/nox/core/server/server.js';
let s=fs.readFileSync(p,'utf8');
const a='" Fatos que você sabe sobre o usuário: " + facts.join("; ")';
if(!s.includes(a)){console.error('Âncora não achada');process.exit(1)}
s=s.replace(a,()=>String.raw`" IMPORTANTE: eu, meu e minha nas frases abaixo se referem ao USUÁRIO, nunca a você (NOX). Informações dadas pelo usuário: " + facts.map(f => 'O usuário disse: "' + f + '"').join("; ")`);
fs.writeFileSync(p,s);
console.log('Patch 2 aplicado.');
