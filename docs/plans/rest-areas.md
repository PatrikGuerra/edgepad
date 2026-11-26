# Plano: apoiar a mão no trackpad sem gerar toque

Este documento compara duas formas de deixar a mão apoiada na tela do Edgepad sem que o apoio mova o ponteiro, clique ou mude a contagem de dedos dos gestos. O alvo é o tablet Samsung SM-T520 (Galaxy Tab Pro 10.1, LineageOS 14.1, Android 7.1.2), na branch `feature/android-7`.

## Como o trackpad recebe toques hoje

- `ControlSurface.onTouchEvent` (`android/app/src/main/java/me/akshitbansal/edgepad/surface/ControlSurface.kt`) recebe todos os toques da tela.
- `down(event)` decide o que o primeiro dedo pegou: um botão do topo, a faixa de bateria, um botão de mídia, uma régua de canto ou o trackpad.
- Os dedos do trackpad vão para o `TrackpadRecognizer` por `feed(...)`, e `fingers(...)` conta quantos dedos estão na tela. Essa contagem escolhe o gesto: 1 dedo move, 2 rolam, 3 e 4 trocam de janela.
- Hoje todo dedo na área do trackpad conta. Uma palma apoiada vira um dedo a mais, e um movimento com 1 dedo vira rolagem com 2.

Os dois planos usam o mesmo ponto de corte: um conjunto `ignored` com os ids de ponteiro que não contam. O id entra no conjunto quando o dedo toca, e sai quando ele sobe. `feed`, `fingers` e `move` pulam esses ids. Os ponteiros continuam no `MotionEvent`, só não são lidos.

## Plano A: bordas de apoio

Faixas nas bordas da tela onde um toque que começa ali é ignorado até subir.

### Regras

| Regra | Motivo |
|---|---|
| Um toque que começa numa borda de apoio é ignorado até subir | a palma apoiada não faz nada |
| Um dedo que começa fora e entra na borda continua valendo | um deslize longo não quebra |
| Um toque ignorado não conta para os gestos | palma + 1 dedo continua sendo "mover", e não "rolar" |
| Réguas de canto e botões que ficam numa borda continuam funcionando | os cantos continuam usáveis |
| Uma sombra leve mostra as bordas, se ligada | dá para ver onde elas ficam |

### Configuração

Em **Configurações › Superfície › Trackpad › Áreas de apoio**:

| Opção | Valores | Padrão |
|---|---|---|
| Esquerda, Direita, Embaixo, Em cima | ligado ou desligado, cada uma | só Embaixo |
| Largura | 0 a 30 % do lado da tela | 12 % |
| Mostrar as áreas | ligado ou desligado | ligado |
| Em pé e deitado | guardados separados, como o layout de mídia (`Settings.layoutKey`) | — |

### Código

1. `Settings.kt`: `restLeft`, `restRight`, `restBottom`, `restTop` (`flag`), `restWidth` (`bounded`, 0 a 0,30) e `restShown`, guardados por orientação.
2. `ControlSurface.kt`: em `onSizeChanged`, montar os retângulos das bordas. Em `ACTION_DOWN` e `ACTION_POINTER_DOWN`, se o ponto cai num retângulo e não pega uma régua nem um botão, guardar o id em `ignored`. Desenhar a sombra em `onDraw` quando `restShown`.
3. `SurfaceSettingsScreen.kt`: a seção nova com os interruptores e o controle deslizante, e uma prévia como a de `DialPreview`.
4. Textos em `values/strings.xml` e `values-pt-rBR/strings.xml`.
5. Teste de unidade para a regra "começou na borda" (geometria pura, sem Android).

### Custo e risco

- Pequeno: cerca de 150 linhas. Funciona igual em qualquer tela, porque só usa a posição do toque.
- Risco: a borda tira área útil do trackpad. Num tablet de 10" isso pesa pouco. Num celular pesa mais.

## Plano B: rejeição de palma pelo tamanho do toque

Ignorar um toque cujo tamanho de contato passa de um limite, em qualquer lugar da tela.

### Regras

| Regra | Motivo |
|---|---|
| Um toque com `touchMajor` acima do limite nos primeiros 80 ms é ignorado até subir | a palma é larga, o dedo é estreito |
| Um toque ignorado não conta para os gestos | igual ao plano A |
| Um toque que começou pequeno e cresceu depois continua valendo | um dedo que aperta mais não é cortado no meio do gesto |

### Configuração

Em **Configurações › Superfície › Trackpad**:

| Opção | Valores | Padrão |
|---|---|---|
| Ignorar toques grandes | ligado ou desligado | desligado |
| Tamanho do toque | 5 a 40 mm, com uma área de teste que mostra o tamanho do toque ao vivo | 18 mm |

### Código

1. `Settings.kt`: `palmReject` (`flag`) e `palmMm` (`bounded`).
2. `ControlSurface.kt`: em `ACTION_DOWN` e `ACTION_POINTER_DOWN`, ler `event.getTouchMajor(i)` em px, converter para mm com `displayMetrics.xdpi`, e guardar o id em `ignored` se passar do limite. Esperar até 80 ms antes de decidir, porque a palma cresce ao encostar.
3. `SurfaceSettingsScreen.kt`: o interruptor, o controle deslizante e a área de teste.
4. Textos nos dois idiomas.
5. Teste de unidade para a decisão "grande ou pequeno" com valores gravados do tablet.

### Custo e risco

- Médio: cerca de 200 linhas, e a espera de 80 ms atrasa o primeiro movimento de todo dedo.
- Risco alto: depende do driver da tela. Muitas telas mandam um tamanho fixo ou sem escala real, e aí o plano B não faz nada. Uma palma de lado (só a borda da mão) pode parecer um dedo.

## Verificação no SM-T520

O que o tablet informa, lido com `adb shell getevent -lp` e `adb shell dumpsys input`:

| Dado | Valor |
|---|---|
| Tela | `sec_touchscreen`, até 10 dedos |
| `ABS_MT_TOUCH_MAJOR` e `ABS_MT_TOUCH_MINOR` | 0 a 255 |
| `ABS_MT_PRESSURE` | 0 a 255 |
| `touch.size.calibration` | `geometric` |

A tela manda o tamanho do toque, então o plano B é possível. Falta medir se um dedo e uma palma dão números bem diferentes. Essa medição está na seção "Resultado da medição".

## Resultado da medição

Não feita: o usuário não estava perto do tablet. Para medir depois, gravar com `adb shell getevent -lt /dev/input/event2` 5 s de toques com um dedo e 5 s com a palma apoiada, e comparar os valores de `ABS_MT_TOUCH_MAJOR`.

## Decisão

Plano A primeiro, porque:

1. Ele funciona no SM-T520 sem depender do driver da tela.
2. Num tablet de 10", a mão apoia nas bordas, e perder 12 % de uma borda custa pouco.
3. Ele não atrasa nenhum toque.
4. O conjunto `ignored` fica pronto para o plano B, que entra depois como um interruptor a mais, se a medição mostrar que o dedo e a palma dão números bem diferentes.
