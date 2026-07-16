# Player 项目结构

## 目录结构

```
player/
├── app/                        # Sample（UI + Media3 注入）
│   └── src/main/assets/media/
│       ├── demo.mp4
│       ├── demo.srt
│       └── demo.vtt
└── player-sdk/
    ├── player-api/             # 公开 API
    ├── player-core/            # 生命周期 / 状态机 / Engine / 字幕 / 倍速
    ├── player-media3/          # Media3Engine + Cache
    └── player-common/          # （可选）
```

## 公开 API（player-api）

- `PlayerController`
- `PlayerState`
- `PlayerEvent`
- `PlayerListener`
- `MediaSource`（Url / LocalFile / Asset）
- `PlayerConfig`
- `PlayerError`
- `PlayerCapability`
- `CacheCapability` / `CacheConfig`（V1.1）
- `SubtitleCapability` / `SubtitleCue` / `SubtitleTrack` / `SubtitleConfig`（V1.2）
- `SpeedCapability` / `SpeedConfig` / `AbRepeatRange`（V1.3）
- `ScreenshotCapability`（V1.4）

## 模块依赖关系

```
app
 └── player-media3
      └── player-core
           └── player-api
```

## 快速开始

```kotlin
implementation(project(":player-sdk:player-media3"))
```

```kotlin
val handle = Media3PlayerFactory(context).create()
val controller = handle.controller
handle.engine.bindPlayerView(playerView)

// HTTP
controller.setMediaSource(MediaSource.Url(url, title = "HTTP"))
// Asset
controller.setMediaSource(MediaSource.Asset("media/demo.mp4", title = "Asset"))
// Local（应用私有目录绝对路径）
controller.setMediaSource(MediaSource.LocalFile(path, title = "Local"))

controller.prepare()
controller.play()
```

## Sample 验收

| 按钮 | MediaSource / 能力 | 说明 |
|------|-------------------|------|
| HTTP / HLS / Asset / Local | `MediaSource` | 基础播放 + HLS |
| Play / Pause | Controller | 播放控制 |
| 0.5x / 1x / 1.5x / 2x | `SpeedCapability.setSpeed` | 倍速 |
| Loop ON/OFF | `PlayerConfig.loop` | 全曲循环 |
| Set A / Set B / Clear AB | `SpeedCapability` AB | 区间循环 |
| Capture | `ScreenshotCapability.capture` | 当前帧截图预览 |
| Load SRT / Load VTT | `SubtitleCapability` | 加载外挂字幕 |
| Sub ON/OFF | `setEnabled` | 开关字幕 |
| Preload / Refresh / Clear | `CacheCapability` | 缓存验证 |

## V1.1 缓存快速开始

```kotlin
val handle = Media3PlayerFactory(context).create()
val cache = Media3CacheCapability(context, handle.engine)
handle.controller.install(cache)
```

## V1.2 字幕快速开始

```kotlin
val subtitle = DefaultSubtitleCapability()
controller.install(subtitle)
subtitle.loadSrt(assets.open("media/demo.srt").reader().readText())
// UI: collect subtitle.currentCue
```

## V1.3 倍速 / 循环 / AB 快速开始

```kotlin
val speed = DefaultSpeedCapability(engine)
controller.install(speed)
speed.setSpeed(1.5f)
controller.setConfig(controller.config.copy(loop = true))
speed.setAbRepeat(startMs = 2000, endMs = 8000)
```


## V1.4 截图快速开始

```kotlin
val shot = Media3ScreenshotCapability(engine)
controller.install(shot)
val bitmap = shot.capture() // suspend
```

## V2.0 HLS 快速开始

继续用 `MediaSource.Url`，把地址换成 `.m3u8`（`player-media3` 已含 HLS 模块）：

```kotlin
controller.setMediaSource(
    MediaSource.Url("https://example.com/live/index.m3u8", title = "HLS")
)
controller.prepare()
controller.play()
```

## 扩展指南

| 需求 | 修改模块 |
|------|----------|
| 公开契约 | `player-api` |
| 状态机 / 生命周期 / 字幕 / 倍速·AB | `player-core` |
| Media3 播放 / 磁盘缓存 / 截图 / HLS | `player-media3` |
| Demo UI | `app` |

详见：

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [API_GUIDE.md](API_GUIDE.md)
- [MODULES.md](MODULES.md)
- [ROADMAP.md](ROADMAP.md)
- [TASKS.md](TASKS.md)
