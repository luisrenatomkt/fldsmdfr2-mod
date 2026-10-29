# Plano de testes

[AUTO] = executado em Java puro por `LogicSelfTest` (nao precisa de Minecraft). [MANUAL] = jogar.

| # | Teste | Como verificar |
|---|---|---|
| 1 | Instalacao/inicializacao [MANUAL] | Jar em `mods/`, Forge 1.8.9: mod "FLDSMDFR" listado, sem crash. |
| 2 | Colocacao [MANUAL] | Bloco na aba Redstone; a capsula aparece virada para voce. |
| 3 | Funcionamento inicial [MANUAL] | Painel abre; "Operacional"; luzes pulsando. |
| 4 | Producao de comida [MANUAL] | Comidas 3D saem dos canhoes e ficam no chao; nao sao pegaveis. |
| 5 | Evolucao visual [AUTO+MANUAL] | Auto: escala, eras, cores crescem sem limite. Manual: `/fldsmdfr evolve 8/20/60/200`: a maquina muda. |
| 6 | Producao ligada ao crescimento [AUTO] | Mais componentes => mais producao (9,3 -> 410). |
| 7 | Instabilidade [MANUAL] | `instabilityRate 50`: barra sobe, faiscas, arcos, luzes piscam. |
| 8 | Varios canhoes [MANUAL] | Comida sai de varios pontos da maquina. |
| 9 | Tamanho da comida cresce [AUTO+MANUAL] | Auto: 0.7 -> 1.6 -> 3.5 -> 7.9. Manual: comparar apos `evolve 10` e `evolve 1000`. |
| 10 | Salvar/carregar [MANUAL] | Sair e reentrar: tempo, evolucao, instabilidade mantidos. |
| 11 | Progressao offline [MANUAL] | Fechar 5+ min, reabrir: evolucao maior. |
| 12 | Chunks nao carregados [MANUAL] | Afastar-se: a logica pausa (so ocorre com o chunk carregado). |
| 13 | Pods estaveis e distintos [AUTO] | 2000 posicoes distintas; pods antigos nunca se movem. |
| 14 | Limite de entidades [MANUAL] | `stormIntensity 100`: `maxActiveFood` nunca e excedido. |
| 15 | Valores muito altos [AUTO+MANUAL] | Auto: 10^500 componentes, 10^10000 em BigNum. Manual: `evolve 400`. |
| 16 | Funcionamento prolongado [AUTO+MANUAL] | Auto: 20 anos simulados sem NaN/Infinity. Manual: 1 h com `evolutionSpeed 100`. |
| 17 | Reinicio do jogo [MANUAL] | Fechar e abrir o Minecraft: estado recuperado. |
