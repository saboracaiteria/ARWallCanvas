# ARWallCanvas 🎨📱

**Aplicativo Android de Realidade Aumentada para desenho em paredes virtuais.**

## ✨ Funcionalidades

- Câmera ao vivo com sobreposição AR
- Ferramentas de desenho: pincel, marcador, spray, borracha
- Seletor de cores
- Desfazer / Refazer
- Salvar desenhos

## 🛠️ Tecnologias

| Componente | Tecnologia |
|------------|-----------|
| Linguagem | Kotlin |
| AR | Google ARCore |
| Câmera | CameraX |
| Mínimo | Android 8.0 (API 26) |
| Build | Gradle + Kotlin DSL |

## 🚀 Compilar

```bash
./gradlew assembleDebug
```

APK em: `app/build/outputs/apk/debug/`

## 📁 Estrutura

```
ARWallCanvas/
├── app/src/main/java/com/arwallcanvas/
│   ├── MainActivity.kt
│   ├── ARWallRenderer.kt
│   ├── DrawingCanvas.kt
│   ├── DepthHelper.kt
│   ├── drawing/DrawingEngine.kt
│   ├── ui/CameraPreviewView.kt
│   ├── ui/DrawingOverlayView.kt
│   └── utils/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```
