# FLDSMDFR - Maquina de Comida Infinita (Minecraft 1.8.9 / Forge 11.15.1.2318)

Mod inspirado na maquina FLDSMDFR do filme "Ta Chovendo Hamburguer".

## O que muda com a evolucao
* **A propria maquina evolui visualmente, sem limite.** Nada e construido ao redor dela. Ela comeca como a capsula
  branca e laranja (tela com circulo azul, teclado verde, tira de botoes, pernas, leque de luzes azuis) e passa a
  ganhar, continuamente: modulos acoplados (camaras, tanques, tubos, canhoes, turbinas, bracos, antenas,
  condutores, mini-nucleos, plataformas), aneis orbitais com satelites, propulsores, espiras, aura e estrelas
  orbitando. O corpo todo tambem continua crescendo de tamanho.
* **Eras infinitas:** a cada 6 "decadas" de evolucao comeca uma nova era com novo esquema de cores e novos
  aneis/espiras gerados do seed (nao ha lista fixa de modelos). Os nomes das 12 primeiras seguem sua imagem
  (Original, Aprimoramento, Conectividade, Expansao, Propulsao, Defesa e poder, Infinito, Sem limites, Dominio
  total, Onipresenca, Transcendencia, Infinito absoluto); depois disso, "O Criador" para sempre.
* **Comida fisica 3D (nao e item, nao pode ser pega):** hamburguer, pizza, rosquinha, bolo, macarrao,
  cachorro-quente, sanduiche, batata frita e sorvete. Sao entidades solidas que voam em arco, quicam e ficam
  no chao. **O tamanho cresce com a evolucao, sem limite** (0,7 bloco no inicio, ~8 blocos na tier 1000).
* Os canhoes de onde a comida sai sao os mesmos que voce ve na maquina.

## Como jogar
* Receita: `I G I / R C R / I H I` (I = bloco de ferro, G = vidro, R = redstone, C = bolo, H = funil). Criativo: aba Redstone.
* Clique direito: painel de informacoes. Shift + clique direito com a mao vazia: liga/desliga.
* `/fldsmdfr config [chave valor]`, `/fldsmdfr info`, `/fldsmdfr evolve <expoente>` (teste: evolucao = 10^expoente), `/fldsmdfr surge`.
  Comandos exigem cheats (ou edite `config/fldsmdfr.cfg`). Dica: `evolutionSpeed 0.2` deixa tudo mais lento.

## Arquitetura (pacote `com.fldsmdfr`)
| Classe | Responsabilidade |
|---|---|
| `core.MachineState` | Estado persistente e simulacao logica (evolucao, producao, instabilidade, surtos, offline). |
| `core.BigNum` | Numero mantissa x 10^expoente(long): evolucao e producao nunca estouram. |
| `core.MachineModel` | Regras puras de aparencia: tier, escala, eras, cores, posicao dos modulos, tamanho da comida. Usado pelo cliente (desenho) e servidor (pontos de lancamento). |
| `core.MachineStats` | Producao/lancamento a partir da quantidade de componentes; composicao muda a cada era. |
| `core.LaunchSystem` | Lotes de comida a partir dos canhoes desenhados; trajetoria balistica; limite de entidades. |
| `entity.EntityFoodDrop` | Comida fisica (tipo e tamanho no DataWatcher), nao e salva no chunk. |
| `client.Gfx` | Gravador de geometria (pilha de matrizes + primitivas): maquina inteira em UMA chamada de desenho. |
| `client.RenderCore` / `RenderFood` / `FoodModels` | Maquina e comidas desenhadas por codigo. |
| `tile.TileEntityFldsmdfr` | Orquestra no servidor; cliente recebe so um snapshot pequeno (`MachineView`). |

### Como os tres sistemas se interligam
1. A evolucao `E` cresce exponencialmente e acelera (taxa depende do tier, da capacidade e da instabilidade).
2. `E` define o numero de componentes `n = 4 + 2*E^0.45`; so uma amostra deles e desenhada (nivel de detalhe), a quantidade total continua na logica.
3. Producao e lancamento vem de `n`; o tamanho do corpo e da comida vem do tier `T = log10(1+E)`.
4. Instabilidade cresce com o tamanho: vibracao, luzes piscando, arcos eletricos, surtos de superproducao e lancamentos mais frequentes.

## Limitacoes
* A logica pura e a simulacao de 20 anos foram executadas (`LogicSelfTest`); o desenho e a fisica dentro do Minecraft so podem ser verificados jogando.
* Apenas `visualComponents` (padrao 220) pods sao desenhados; a quantidade logica pode ser muito maior.
* Numero de comidas ativas limitado por `maxActiveFood` (padrao 150); os mais antigos somem. A caixa de colisao da comida e limitada a 6 blocos (o modelo desenhado continua crescendo).
* A comida e decorativa/fisica: nao causa dano e nao e salva ao fechar o mundo.
* Quebrar o bloco apaga os dados da maquina. A progressao offline so ocorre quando o chunk e recarregado.
* Sem GUI de configuracao em jogo: use comando ou arquivo.
