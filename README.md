# 日出日落闹钟

一款基于天文算法的智能闹钟应用，支持根据日出、日落时间或自定义时间设置闹钟，自动计算每日的日出日落时间。

## 功能特性

### 核心功能
- **日出/日落闹钟**：基于天文算法精确计算日出、日落时间，支持设置提前或延后响铃
- **自定义闹钟**：支持设置任意固定时间的闹钟
- **重复模式**：每天、工作日、周末、自定义（周一~周日自由组合）
- **闹钟编辑**：支持修改已创建的闹钟

### 闹钟设置
- **基准时间选择**：日出、日落、自定义时间
- **偏移量调节**：支持 ±120 分钟的偏移调节（日出/日落闹钟）
- **铃声选择**：可自定义闹钟铃声
- **响铃模式**：全屏闹钟 / 通知栏提醒
- **振动开关**：可开启/关闭振动
- **响铃时长**：1~30 分钟可调
- **渐强铃声**：0~60 秒渐强时长设置
- **贪睡功能**：支持贪睡，时长 1~30 分钟可调
- **节假日跳过**：可选择节假日自动跳过响铃

### 城市定位
- **500+ 城市数据**：覆盖中国 34 个省级行政区及下属市/县
- **模糊搜索**：支持输入城市名关键词快速搜索（如输入"寻乌"可搜索到"寻乌县"）
- **网络搜索**：结合 Android Geocoder 进行网络搜索，提供更广泛的城市匹配
- **GPS 定位**：支持一键获取当前位置
- **切换自动更新**：切换城市后自动重新计算所有已启用闹钟的响铃时间

### 界面与主题
- **Material Design 3**：现代化的 UI 设计
- **深色/浅色模式**：支持跟随系统、浅色、深色三种模式
- **时间校准**：支持手动校准天文算法计算的时间

### 数据持久化
- **Room 数据库**：闹钟数据持久化存储，重启不丢失
- **自动初始化**：首次启动时自动创建示例闹钟

## 技术栈

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose (Material 3)
- **数据库**：Room
- **异步处理**：Kotlin Coroutines / Flow
- **后台调度**：AlarmManager + WorkManager
- **依赖注入**：无（采用 Application 单例模式）

## 项目结构

```
app/src/main/java/com/snuabar/sunrisesunsetalarm/
├── data/
│   ├── CityData.kt                    # 城市数据（500+ 城市）
│   ├── database/
│   │   ├── AlarmDao.kt               # 闹钟数据访问对象
│   │   ├── AppDatabase.kt            # Room 数据库配置
│   │   └── LocationDao.kt            # 位置数据访问对象
│   ├── model/
│   │   ├── Alarm.kt                  # 闹钟实体
│   │   ├── Location.kt               # 位置实体
│   │   └── AppSettings.kt            # 应用设置
│   └── repository/
│       ├── AlarmRepository.kt        # 闹钟数据仓库
│       └── LocationRepository.kt     # 位置数据仓库
├── receiver/
│   ├── AlarmReceiver.kt              # 闹钟广播接收器
│   ├── AlarmRescheduleWork.kt        # 闹钟重调度 Worker
│   └── BootReceiver.kt               # 开机广播（重启后恢复闹钟）
├── service/
│   ├── AlarmManagerHelper.kt         # AlarmManager 封装
│   ├── AlarmNotificationHelper.kt    # 通知管理
│   ├── AlarmService.kt               # 前台服务
│   ├── AlarmDismissReceiver.kt       # 闹钟关闭接收器
│   └── SnoozeHelper.kt               # 贪睡逻辑
├── ui/
│   ├── components/
│   │   ├── AddAlarmBottomSheet.kt    # 添加/编辑闹钟弹窗
│   │   ├── AlarmCard.kt              # 闹钟卡片
│   │   └── LocationPicker.kt         # 城市选择器
│   ├── screens/
│   │   ├── HomeScreen.kt             # 主屏幕
│   │   └── FullScreenAlarmActivity.kt # 全屏闹钟页面
│   └── theme/                        # Compose 主题
├── util/
│   ├── SunCalcUtil.kt                # 日出日落计算（NOAA 算法）
│   ├── SettingsManager.kt            # 设置管理
│   ├── HolidayUtil.kt                # 节假日判断
│   └── LocationManagerHelper.kt      # 位置管理
├── MainActivity.kt
└── SunriseSunsetApplication.kt       # Application 入口
```

## 日出日落算法

应用内置了简化的 [NOAA 太阳位置算法](https://gml.noaa.gov/grad/conference/SunCalc/suncalc.pdf)，无需联网即可计算任意日期、任意地理位置的日出日落时间。

```
SunTimes = calculateSunTimes(date, latitude, longitude)
// 返回：日出时间、日落时间、正午时间
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `ACCESS_FINE_LOCATION` | 获取精确位置用于 GPS 定位 |
| `ACCESS_COARSE_LOCATION` | 获取粗略位置 |
| `RECEIVE_BOOT_COMPLETED` | 开机后重新调度闹钟 |
| `SCHEDULE_EXACT_ALARM` | 设置精确闹钟（Android 12+） |
| `POST_NOTIFICATIONS` | 发送闹钟通知（Android 13+） |
| `VIBRATE` | 闹钟振动提醒 |
| `USE_FULL_SCREEN_INTENT` | 全屏闹钟弹窗 |
| `FOREGROUND_SERVICE` | 前台服务播放闹钟 |
| `WAKE_LOCK` | 唤醒设备响铃 |
| `INTERNET` | Geocoder 网络搜索 |

## 开发环境

- Android Studio (最新稳定版)
- Android SDK 36
- minSdk: 26 (Android 8.0)
- targetSdk: 36 (Android 16)
- JDK 17
- Kotlin 1.9.22

## 构建运行

1. 克隆仓库
2. 用 Android Studio 打开项目
3. 同步 Gradle 依赖
4. 连接设备或启动模拟器
5. 点击 Run

## 未来计划

- [ ] 天气联动（根据天气调整日出时间提示）
- [ ] 云端备份（闹钟设置同步）
- [ ] 智能推荐（根据睡眠周期推荐最佳唤醒时间）
- [ ] 语音播报（响铃时播报天气和时间）
