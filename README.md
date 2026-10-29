# Varinha Mágica

Aplicativo Android em Kotlin e Jetpack Compose para explorar feitiços do universo Harry Potter e reconhecê-los por gestos de um único traço.

## API utilizada

[Hogwarts API](https://hogwarts-api.com/api/spells) - lista de feitiços, categorias e efeitos. O app usa Retrofit e coroutines e exibe carregamento, sucesso e erro de conexão.

## Funcionalidades

- Cadastro e login locais com senha armazenada como hash SHA-256.
- Sessão persistente e favoritos isolados por usuário com Room.
- Quatro abas: Feitiços, Desenhar, Favoritos e Duelo.
- Duelo de feiticeiros por turnos contra um oponente controlado pelo app (Fácil, Médio ou Difícil): o jogador monta um grimório com 7 feitiços e, a cada turno, tem um tempo limite para desenhar um deles. A precisão do desenho altera a força do feitiço, e vitórias e derrotas ficam salvas por usuário.
- Tela de detalhe acessada por rota tipada.
- Vinte e cinco gestos de um traço guardados localmente; o reconhecimento é executado quando o dedo deixa a tela.

## Como gerar o APK

No terminal do projeto, com o Android Studio/JDK configurado:

```powershell
./gradlew.bat assembleDebug
```

O arquivo gerado fica em `app/build/outputs/apk/debug/app-debug.apk`. Transfira-o para um celular Android, abra o arquivo e permita a instalação pela origem usada.

## Prints

Adicionar antes da entrega: login, cadastro, lista, detalhe, favoritos e estado de erro sem internet.

## Divisão de tarefas

| Integrante | O que fez |
|---|---|
| Integrante 1 | API, Room e repositórios |
| Integrante 2 | Telas Compose, navegação e gestos |

## O que aprendemos

Completar com uma frase de cada integrante antes da entrega.
